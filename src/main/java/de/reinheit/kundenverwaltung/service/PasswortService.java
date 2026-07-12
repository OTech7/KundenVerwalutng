package de.reinheit.kundenverwaltung.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Sichere Passwort-Speicherung mit PBKDF2 (in der JDK enthalten – keine
 * zusätzliche Bibliothek nötig). Jedes Passwort erhält ein zufälliges Salt;
 * gespeichert werden Salt und Hash getrennt.
 */
public class PasswortService {

    private static final int ITERATIONEN = 120_000;
    private static final int SCHLUESSEL_LAENGE = 256; // bit
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Erzeugt ein neues zufälliges Salt (Base64-kodiert). */
    public String neuesSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Berechnet den PBKDF2-Hash eines Passworts mit gegebenem Salt (Base64). */
    public String hash(String passwort, String saltBase64) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(passwort.toCharArray(), salt, ITERATIONEN, SCHLUESSEL_LAENGE);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Passwort-Hashing fehlgeschlagen", e);
        }
    }

    /** Prüft, ob ein Passwort zum gespeicherten Hash/Salt passt. */
    public boolean pruefen(String passwort, String saltBase64, String erwarteterHash) {
        String berechnet = hash(passwort, saltBase64);
        // zeitkonstanter Vergleich
        return java.security.MessageDigest.isEqual(
                berechnet.getBytes(), erwarteterHash.getBytes());
    }
}
