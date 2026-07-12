package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.model.Mitarbeiter;
import de.reinheit.kundenverwaltung.service.AuditService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Datenzugriff für Mitarbeiter (nur Namen). */
public class MitarbeiterDao {

    /** Alle Mitarbeiter (alphabetisch). */
    public List<Mitarbeiter> alle() {
        List<Mitarbeiter> out = new ArrayList<>();
        String sql = "SELECT Id, Name FROM Mitarbeiter ORDER BY Name COLLATE NOCASE";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(new Mitarbeiter(rs.getInt("Id"), rs.getString("Name")));
        } catch (SQLException e) {
            throw new RuntimeException("Mitarbeiter laden fehlgeschlagen", e);
        }
        return out;
    }

    /** Nur die Namen – für Auswahllisten. */
    public List<String> namen() {
        List<String> out = new ArrayList<>();
        for (Mitarbeiter m : alle()) out.add(m.getName());
        return out;
    }

    /** Fügt einen Mitarbeiter hinzu. Doppelte Namen werden ignoriert. */
    public void hinzufuegen(String name) {
        if (name == null || name.isBlank()) return;
        try (PreparedStatement ps = Database.get().prepareStatement(
                "INSERT OR IGNORE INTO Mitarbeiter (Name) VALUES (?)")) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            AuditService.log("Angelegt", "Mitarbeiter", name.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Mitarbeiter hinzufügen fehlgeschlagen", e);
        }
    }

    /** Löscht einen Mitarbeiter. Bereits erfasste Kunden/Einsätze behalten den Namen. */
    public void loeschen(int id) {
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM Mitarbeiter WHERE Id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            AuditService.log("Gelöscht", "Mitarbeiter", "Nr. " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Mitarbeiter löschen fehlgeschlagen", e);
        }
    }

    /** Benennt einen Mitarbeiter um. */
    public void umbenennen(int id, String neuerName) {
        if (neuerName == null || neuerName.isBlank()) return;
        try (PreparedStatement ps = Database.get().prepareStatement("UPDATE Mitarbeiter SET Name=? WHERE Id=?")) {
            ps.setString(1, neuerName.trim());
            ps.setInt(2, id);
            ps.executeUpdate();
            AuditService.log("Bearbeitet", "Mitarbeiter", "Nr. " + id + " → " + neuerName.trim());
        } catch (SQLException e) {
            throw new RuntimeException("Mitarbeiter umbenennen fehlgeschlagen", e);
        }
    }
}
