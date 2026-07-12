package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.db.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Aktualisiert automatisch die erbrachten und verbleibenden Stunden eines Kunden,
 * wenn ein Einsatz mit Status "Erledigt" gespeichert wird (laut Spezifikation).
 *
 * VerbleibendeStunden = GenehmigteStunden - ErbrachteStunden
 */
public class StundenService {

    /** Bucht die Dauer eines erledigten Einsatzes auf das Stundenkonto des Kunden. */
    public void einsatzVerbuchen(int kundennummer, double dauer) {
        String sql = """
            UPDATE Kunden
               SET ErbrachteStunden    = ErbrachteStunden + ?,
                   VerbleibendeStunden = GenehmigteStunden - (ErbrachteStunden + ?)
             WHERE Kundennummer = ?
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setDouble(1, dauer);
            ps.setDouble(2, dauer);
            ps.setInt(3, kundennummer);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Stundenbuchung fehlgeschlagen", e);
        }
    }

}
