package de.reinheit.kundenverwaltung.service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Feldverschlüsselung für besonders schützenswerte, personenbezogene Daten
 * (z. B. Krankenkasse, Pflegegrad, Kontaktdaten) – AES-256 im GCM-Modus.
 *
 * Verhalten:
 *  - Verschlüsselte Werte beginnen mit dem Präfix "enc:".
 *  - Beim Entschlüsseln werden Werte ohne dieses Präfix unverändert
 *    zurückgegeben (Abwärtskompatibilität zu bereits gespeicherten Klartexten).
 *
 * Schlüssel: wird in der Datei "kunden.key" neben der Datenbank abgelegt und
 * beim ersten Start erzeugt. HINWEIS: Für höchsten Schutz sollte zusätzlich die
 * Festplatte des Rechners verschlüsselt sein (BitLocker/FileVault), da der
 * Schlüssel lokal liegt.
 */
public final class CryptoService {

    private static final String PRAEFIX = "enc:";
    private static final String KEY_DATEI = "kunden.key";
    private static final int GCM_IV_LEN = 12;      // Byte
    private static final int GCM_TAG_LEN = 128;    // Bit
    private static final SecureRandom RANDOM = new SecureRandom();

    private static SecretKey schluessel;

    private CryptoService() {}

    private static synchronized SecretKey key() {
        if (schluessel != null) return schluessel;
        try {
            Path p = de.reinheit.kundenverwaltung.db.AppDaten.datei(KEY_DATEI);
            if (Files.exists(p)) {
                byte[] raw = Base64.getDecoder().decode(Files.readString(p).trim());
                schluessel = new SecretKeySpec(raw, "AES");
            } else {
                KeyGenerator kg = KeyGenerator.getInstance("AES");
                kg.init(256);
                schluessel = kg.generateKey();
                Files.writeString(p, Base64.getEncoder().encodeToString(schluessel.getEncoded()));
                try { // Dateirechte einschränken, soweit vom OS unterstützt
                    p.toFile().setReadable(false, false);
                    p.toFile().setReadable(true, true);
                    p.toFile().setWritable(false, false);
                    p.toFile().setWritable(true, true);
                } catch (Exception ignored) {}
            }
            return schluessel;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Der Verschlüsselungsschlüssel konnte nicht geladen oder erstellt werden.\n"
                    + "Erwartet in: " + de.reinheit.kundenverwaltung.db.AppDaten.verzeichnis().resolve(KEY_DATEI)
                    + "\nBitte Schreibrechte prüfen.", e);
        }
    }

    /** Verschlüsselt einen Klartext (null/leer bleibt unverändert). */
    public static String verschluesseln(String klartext) {
        if (klartext == null || klartext.isEmpty()) return klartext;
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] ct = c.doFinal(klartext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PRAEFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("Verschlüsselung fehlgeschlagen", e);
        }
    }

    /** Entschlüsselt einen Wert; Werte ohne "enc:"-Präfix bleiben unverändert. */
    public static String entschluesseln(String wert) {
        if (wert == null || !wert.startsWith(PRAEFIX)) return wert;
        try {
            byte[] all = Base64.getDecoder().decode(wert.substring(PRAEFIX.length()));
            byte[] iv = new byte[GCM_IV_LEN];
            byte[] ct = new byte[all.length - GCM_IV_LEN];
            System.arraycopy(all, 0, iv, 0, GCM_IV_LEN);
            System.arraycopy(all, GCM_IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LEN, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Falscher/fehlender Schlüssel: klare, handlungsleitende Meldung
            throw new IllegalStateException(
                    "Verschlüsselte Daten können nicht gelesen werden.\n"
                    + "Die Schlüsseldatei '" + KEY_DATEI + "' fehlt oder passt nicht zur Datenbank.\n"
                    + "Ordner: " + de.reinheit.kundenverwaltung.db.AppDaten.verzeichnis() + "\n"
                    + "Bitte die ursprüngliche Schlüsseldatei aus dem Backup wiederherstellen.", e);
        }
    }
}
