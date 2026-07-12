package de.reinheit.kundenverwaltung.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswortServiceTest {

    private final PasswortService pw = new PasswortService();

    @Test
    void gleichesPasswortUndSaltErgibtGleichenHash() {
        String salt = pw.neuesSalt();
        assertEquals(pw.hash("geheim123", salt), pw.hash("geheim123", salt));
    }

    @Test
    void unterschiedlichesSaltErgibtUnterschiedlichenHash() {
        String h1 = pw.hash("geheim123", pw.neuesSalt());
        String h2 = pw.hash("geheim123", pw.neuesSalt());
        assertNotEquals(h1, h2, "Zwei zufällige Salts sollten unterschiedliche Hashes ergeben");
    }

    @Test
    void pruefenAkzeptiertRichtigesPasswort() {
        String salt = pw.neuesSalt();
        String hash = pw.hash("MeinPasswort!", salt);
        assertTrue(pw.pruefen("MeinPasswort!", salt, hash));
    }

    @Test
    void pruefenLehntFalschesPasswortAb() {
        String salt = pw.neuesSalt();
        String hash = pw.hash("MeinPasswort!", salt);
        assertFalse(pw.pruefen("falsch", salt, hash));
    }

    @Test
    void hashIstNichtDasKlartextPasswort() {
        String salt = pw.neuesSalt();
        String hash = pw.hash("Klartext", salt);
        assertFalse(hash.contains("Klartext"));
        assertTrue(hash.length() > 20);
    }
}
