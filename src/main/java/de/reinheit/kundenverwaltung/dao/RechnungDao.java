package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.model.Rechnung;
import de.reinheit.kundenverwaltung.service.AuditService;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Datenzugriff für Abrechnungen (Rechnungen an die Krankenkasse).
 * Nur Administratoren (Abrechnung ist ein Finanzbereich).
 */
public class RechnungDao {

    /** Alle Rechnungen, optional nach Kunde gefiltert (null = alle). */
    public List<Rechnung> finde(Integer kundennummer) {
        List<Rechnung> list = new ArrayList<>();
        String sql = "SELECT * FROM Rechnungen"
                + (kundennummer == null ? "" : " WHERE Kundennummer = ?")
                + " ORDER BY ZeitraumVon DESC, Id DESC";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            if (kundennummer != null) ps.setInt(1, kundennummer);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mappe(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Rechnungen laden fehlgeschlagen", e);
        }
        return list;
    }

    /** Geplante Termine eines Kunden im Zeitraum. */
    public int zaehleGeplant(int kundennummer, LocalDate von, LocalDate bis) {
        String sql = "SELECT COUNT(*) FROM Termine WHERE Kundennummer=? AND TerminDatum BETWEEN ? AND ?";
        return zaehle(sql, kundennummer, von, bis);
    }

    /**
     * Tatsächlich erbrachte Termine eines Kunden im Zeitraum.
     * Ein abgesagter Termin zählt vertraglich ebenfalls als erbracht.
     */
    public int zaehleErbracht(int kundennummer, LocalDate von, LocalDate bis) {
        String sql = "SELECT COUNT(*) FROM Termine WHERE Kundennummer=? "
                + "AND Status IN ('Erledigt','Abgesagt') AND TerminDatum BETWEEN ? AND ?";
        return zaehle(sql, kundennummer, von, bis);
    }

    /** Geplante Termine insgesamt (ohne Zeitraum). */
    public int zaehleGeplantGesamt(int kundennummer) {
        return zaehleGesamt("SELECT COUNT(*) FROM Termine WHERE Kundennummer=?", kundennummer);
    }

    /** Erbrachte Termine insgesamt (erledigt oder abgesagt; ohne Zeitraum). */
    public int zaehleErbrachtGesamt(int kundennummer) {
        return zaehleGesamt("SELECT COUNT(*) FROM Termine WHERE Kundennummer=? "
                + "AND Status IN ('Erledigt','Abgesagt')", kundennummer);
    }

    private int zaehle(String sql, int kundennummer, LocalDate von, LocalDate bis) {
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, kundennummer);
            ps.setString(2, von.toString());
            ps.setString(3, bis.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) {
            throw new RuntimeException("Zählung fehlgeschlagen", e);
        }
    }

    private int zaehleGesamt(String sql, int kundennummer) {
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, kundennummer);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) {
            throw new RuntimeException("Zählung fehlgeschlagen", e);
        }
    }

    /**
     * Speichert eine Rechnung und setzt beim Kunden "LetzteAbrechnungBis" auf das
     * Enddatum – dadurch beginnt automatisch die nächste Abrechnungsperiode.
     */
    public int erstellen(Rechnung r) {
        String sql = """
            INSERT INTO Rechnungen
            (Kundennummer, ZeitraumVon, ZeitraumBis, AnzahlGeplant, AnzahlErbracht, Betrag, Status, ErstelltAm)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        int neueId;
        try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getKundennummer());
            ps.setString(2, r.getZeitraumVon());
            ps.setString(3, r.getZeitraumBis());
            ps.setInt(4, r.getAnzahlGeplant());
            ps.setInt(5, r.getAnzahlErbracht());
            ps.setDouble(6, r.getBetrag());
            ps.setString(7, "Abgerechnet");
            ps.setString(8, LocalDate.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                neueId = keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Rechnung erstellen fehlgeschlagen", e);
        }

        // Nächste Periode: beginnt nach dem Ende dieser Periode
        try (PreparedStatement ps = Database.get().prepareStatement(
                "UPDATE Kunden SET LetzteAbrechnungBis=? WHERE Kundennummer=?")) {
            ps.setString(1, r.getZeitraumBis());
            ps.setInt(2, r.getKundennummer());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Abrechnungsperiode konnte nicht fortgeschrieben werden", e);
        }

        AuditService.log("Angelegt", "Rechnung",
                "Kunde #" + r.getKundennummer() + " · " + r.getZeitraum() + " · " + r.getAnzahlErbracht() + " Termine");
        return neueId;
    }

    private Rechnung mappe(ResultSet rs) throws SQLException {
        Rechnung r = new Rechnung();
        r.setId(rs.getInt("Id"));
        r.setKundennummer(rs.getInt("Kundennummer"));
        r.setZeitraumVon(rs.getString("ZeitraumVon"));
        r.setZeitraumBis(rs.getString("ZeitraumBis"));
        r.setAnzahlGeplant(rs.getInt("AnzahlGeplant"));
        r.setAnzahlErbracht(rs.getInt("AnzahlErbracht"));
        r.setBetrag(rs.getDouble("Betrag"));
        r.setStatus(rs.getString("Status"));
        r.setErstelltAm(rs.getString("ErstelltAm"));
        return r;
    }
}
