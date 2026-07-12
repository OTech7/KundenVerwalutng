package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.model.Zahlung;
import de.reinheit.kundenverwaltung.service.AuditService;
import de.reinheit.kundenverwaltung.service.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Datenzugriff für Zahlungen. Nur für Administratoren zugänglich (H-2). */
public class ZahlungDao {

    public List<Zahlung> findeAlle() {
        Session.requireAdmin();
        List<Zahlung> list = new ArrayList<>();
        String sql = "SELECT * FROM Zahlungen ORDER BY Id DESC";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mappe(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Zahlungen laden fehlgeschlagen", e);
        }
        return list;
    }

    /** Alle Zahlungen eines Kunden. */
    public List<Zahlung> findeFuerKunde(int kundennummer) {
        Session.requireAdmin();
        List<Zahlung> list = new ArrayList<>();
        String sql = "SELECT * FROM Zahlungen WHERE Kundennummer=? ORDER BY Zahlungsdatum DESC, Id DESC";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, kundennummer);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mappe(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Zahlungen laden fehlgeschlagen", e);
        }
        return list;
    }

    public void hinzufuegen(Zahlung z) {
        Session.requireAdmin();
        String sql = """
            INSERT INTO Zahlungen
            (Kundennummer, Zahlungsdatum, Betrag, Zahlungsart, Zahlungsstatus, Notizen)
            VALUES (?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, z.getKundennummer());
            ps.setString(2, z.getZahlungsdatum());
            ps.setDouble(3, z.getBetrag());
            ps.setString(4, z.getZahlungsart());
            ps.setString(5, z.getZahlungsstatus());
            ps.setString(6, z.getNotizen());
            ps.executeUpdate();
            AuditService.log("Angelegt", "Zahlung", "Kunde #" + z.getKundennummer() + " · " + z.getBetrag() + " €");
        } catch (SQLException e) {
            throw new RuntimeException("Zahlung hinzufügen fehlgeschlagen", e);
        }
    }

    /** Aktualisiert eine bestehende Zahlung (anhand der Id). */
    public void aktualisieren(Zahlung z) {
        Session.requireAdmin();
        String sql = """
            UPDATE Zahlungen SET
              Kundennummer=?, Zahlungsdatum=?, Betrag=?, Zahlungsart=?, Zahlungsstatus=?, Notizen=?
            WHERE Id=?
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, z.getKundennummer());
            ps.setString(2, z.getZahlungsdatum());
            ps.setDouble(3, z.getBetrag());
            ps.setString(4, z.getZahlungsart());
            ps.setString(5, z.getZahlungsstatus());
            ps.setString(6, z.getNotizen());
            ps.setInt(7, z.getId());
            ps.executeUpdate();
            AuditService.log("Bearbeitet", "Zahlung", "Nr. " + z.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Zahlung aktualisieren fehlgeschlagen", e);
        }
    }

    /** Löscht eine Zahlung. */
    public void loeschen(int id) {
        Session.requireAdmin();
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM Zahlungen WHERE Id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            AuditService.log("Gelöscht", "Zahlung", "Nr. " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Zahlung löschen fehlgeschlagen", e);
        }
    }

    private Zahlung mappe(ResultSet rs) throws SQLException {
        Zahlung z = new Zahlung();
        z.setId(rs.getInt("Id"));
        z.setKundennummer(rs.getInt("Kundennummer"));
        z.setZahlungsdatum(rs.getString("Zahlungsdatum"));
        z.setBetrag(rs.getDouble("Betrag"));
        z.setZahlungsart(rs.getString("Zahlungsart"));
        z.setZahlungsstatus(rs.getString("Zahlungsstatus"));
        z.setNotizen(rs.getString("Notizen"));
        return z;
    }
}
