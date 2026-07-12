package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.service.AuditService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet admin-pflegbare Listen (Stammdaten): Leistungsart, Mitarbeiter,
 * Krankenkasse. Wird in den Auswahlfeldern der Kunden-/Einsatzformulare genutzt.
 */
public class StammdatenDao {

    public static final String LEISTUNGSART = "Leistungsart";
    public static final String KRANKENKASSE = "Krankenkasse";
    // Hinweis: Es gibt keine Kategorie "Mitarbeiter" mehr.
    // Mitarbeiter werden über die Benutzerkonten verwaltet (BenutzerDao).

    /** Alle Werte einer Kategorie (alphabetisch). */
    public List<String> liste(String kategorie) {
        List<String> out = new ArrayList<>();
        String sql = "SELECT Wert FROM Stammdaten WHERE Kategorie=? ORDER BY Wert COLLATE NOCASE";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, kategorie);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString("Wert"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Stammdaten laden fehlgeschlagen", e);
        }
        return out;
    }

    /** Fügt einen neuen Wert hinzu. Doppelte werden ignoriert. */
    public void hinzufuegen(String kategorie, String wert) {
        if (wert == null || wert.isBlank()) return;
        String sql = "INSERT OR IGNORE INTO Stammdaten (Kategorie, Wert) VALUES (?,?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, kategorie);
            ps.setString(2, wert.trim());
            ps.executeUpdate();
            AuditService.log("Angelegt", "Stammdaten", kategorie + ": " + wert.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Stammdaten hinzufügen fehlgeschlagen", e);
        }
    }

    /** Entfernt einen Wert aus einer Liste. */
    public void loeschen(String kategorie, String wert) {
        if (wert == null || wert.isBlank()) return;
        String sql = "DELETE FROM Stammdaten WHERE Kategorie=? AND Wert=?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, kategorie);
            ps.setString(2, wert);
            ps.executeUpdate();
            AuditService.log("Gelöscht", "Stammdaten", kategorie + ": " + wert);
        } catch (SQLException e) {
            throw new RuntimeException("Stammdaten löschen fehlgeschlagen", e);
        }
    }
}
