package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.model.Termin;
import de.reinheit.kundenverwaltung.service.AuditService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Datenzugriff für Termine (werden automatisch beim Anlegen eines Kunden erzeugt). */
public class TerminDao {

    /** Alle Termine, optional gefiltert nach Kundennummer (null = alle). */
    public List<Termin> finde(Integer kundennummer) {
        List<Termin> list = new ArrayList<>();
        String sql = "SELECT * FROM Termine"
                + (kundennummer == null ? "" : " WHERE Kundennummer = ?")
                + " ORDER BY TerminDatum";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            if (kundennummer != null) ps.setInt(1, kundennummer);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mappe(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Termine laden fehlgeschlagen", e);
        }
        return list;
    }

    /** Speichert eine Liste erzeugter Termine (Batch). Gibt die Anzahl zurück. */
    public int speichern(List<Termin> termine) {
        String sql = "INSERT INTO Termine (Kundennummer, TerminDatum, Dauer, Woche, Status, Notizen) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            for (Termin t : termine) {
                ps.setInt(1, t.getKundennummer());
                ps.setString(2, t.getTerminDatum());
                ps.setDouble(3, t.getDauer());
                ps.setString(4, t.getWoche());
                ps.setString(5, t.getStatus());
                ps.setString(6, t.getNotizen());
                ps.addBatch();
            }
            int[] r = ps.executeBatch();
            return r.length;
        } catch (SQLException e) {
            throw new RuntimeException("Termine speichern fehlgeschlagen", e);
        }
    }

    /**
     * Markiert alle fälligen, noch offenen Termine als „Erledigt":
     * Datum ist heute oder in der Vergangenheit und der Status ist leer.
     * Ein manuell gesetzter Status (z. B. „Abgesagt") bleibt unangetastet.
     * @return Anzahl der aktualisierten Termine
     */
    public int markiereFaelligeAlsErledigt() {
        String heute = java.time.LocalDate.now().toString();
        String sql = "UPDATE Termine SET Status='Erledigt' "
                + "WHERE (Status IS NULL OR Status='') AND TerminDatum <= ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, heute);
            int n = ps.executeUpdate();
            if (n > 0) AuditService.log("Aktualisiert", "Termin", n + " faellige Termine auf Erledigt gesetzt");
            return n;
        } catch (SQLException e) {
            throw new RuntimeException("Fällige Termine aktualisieren fehlgeschlagen", e);
        }
    }

    /** Löscht einen Termin. */
    public void loeschen(int id) {
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM Termine WHERE Id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            AuditService.log("Gelöscht", "Termin", "Nr. " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Termin löschen fehlgeschlagen", e);
        }
    }

    /** Aktualisiert Datum, Dauer, Woche, Status und Notizen eines Termins. */
    public void aktualisieren(Termin t) {
        String sql = "UPDATE Termine SET TerminDatum=?, Dauer=?, Woche=?, Status=?, Notizen=? WHERE Id=?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, t.getTerminDatum());
            ps.setDouble(2, t.getDauer());
            ps.setString(3, t.getWoche());
            ps.setString(4, t.getStatus());
            ps.setString(5, t.getNotizen());
            ps.setInt(6, t.getId());
            ps.executeUpdate();
            AuditService.log("Bearbeitet", "Termin", "Nr. " + t.getId() + " · " + t.getStatus());
        } catch (SQLException e) {
            throw new RuntimeException("Termin aktualisieren fehlgeschlagen", e);
        }
    }

    private Termin mappe(ResultSet rs) throws SQLException {
        Termin t = new Termin();
        t.setId(rs.getInt("Id"));
        t.setKundennummer(rs.getInt("Kundennummer"));
        t.setTerminDatum(rs.getString("TerminDatum"));
        t.setDauer(rs.getDouble("Dauer"));
        t.setWoche(rs.getString("Woche"));
        t.setStatus(rs.getString("Status"));
        t.setNotizen(rs.getString("Notizen"));
        return t;
    }
}
