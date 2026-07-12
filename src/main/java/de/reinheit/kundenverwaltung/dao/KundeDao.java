package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.service.AuditService;
import de.reinheit.kundenverwaltung.service.CryptoService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Datenzugriff für Kunden. Termine und Einsätze werden NICHT mehr automatisch
 * beim Anlegen erzeugt – dies geschieht bewusst über „Termine erzeugen".
 */
public class KundeDao {

    public List<Kunde> findeAlle()      { return finde(null); }
    public List<Kunde> findeAktive()    { return finde(1); }
    public List<Kunde> findeEhemalige() { return finde(0); }

    private List<Kunde> finde(Integer aktivFilter) {
        String sql = "SELECT * FROM Kunden"
                + (aktivFilter == null ? "" : " WHERE IstAktiv = ?")
                + " ORDER BY Kundennummer";
        List<Kunde> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            if (aktivFilter != null) ps.setInt(1, aktivFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mappe(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Kunden laden fehlgeschlagen", e);
        }
        return list;
    }

    /** Fügt einen Kunden hinzu (ohne automatische Termine/Einsätze). */
    public int hinzufuegen(Kunde k) {
        // VerbleibendeStunden konsistent setzen
        k.setVerbleibendeStunden(k.getGenehmigteStunden() - k.getErbrachteStunden());
        String sql = """
            INSERT INTO Kunden
            (VollstaendigerName, Adresse, KrankenkasseNummer, Leistungsart,
             GenehmigteStunden, ErbrachteStunden, VerbleibendeStunden,
             ZustaendigerMitarbeiter, Notizen, IstAktiv, TerminPlan,
             Geburtsdatum, Telefon, EMail, Pflegegrad, PflegegradSeit,
             Vertragsbeginn, Abrechnungszeitraum, NaechsteAbrechnung,
             Wochentage, Uhrzeit, OrtBereich, Zugang, StandardTerminDauer,
             AbrechnungRhythmusMonate, LetzteAbrechnungBis, PreisProTermin)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, k.getVollstaendigerName());
            ps.setString(2, CryptoService.verschluesseln(k.getAdresse()));
            ps.setString(3, CryptoService.verschluesseln(k.getKrankenkasseNummer()));
            ps.setString(4, k.getLeistungsart());
            ps.setDouble(5, k.getGenehmigteStunden());
            ps.setDouble(6, k.getErbrachteStunden());
            ps.setDouble(7, k.getVerbleibendeStunden());
            ps.setString(8, k.getZustaendigerMitarbeiter());
            ps.setString(9, k.getNotizen());
            ps.setInt(10, k.isIstAktiv() ? 1 : 0);
            ps.setString(11, k.getTerminPlan());
            ps.setString(12, CryptoService.verschluesseln(k.getGeburtsdatum()));
            ps.setString(13, CryptoService.verschluesseln(k.getTelefon()));
            ps.setString(14, CryptoService.verschluesseln(k.getEMail()));
            ps.setString(15, CryptoService.verschluesseln(k.getPflegegrad()));
            ps.setString(16, k.getPflegegradSeit());
            ps.setString(17, k.getVertragsbeginn());
            ps.setString(18, k.getAbrechnungszeitraum());
            ps.setString(19, k.getNaechsteAbrechnung());
            ps.setString(20, k.getWochentage());
            ps.setString(21, k.getUhrzeit());
            ps.setString(22, k.getOrtBereich());
            ps.setString(23, k.getZugang());
            ps.setDouble(24, k.getStandardTerminDauer());
            ps.setInt(25, k.getAbrechnungRhythmusMonate());
            ps.setString(26, k.getLetzteAbrechnungBis());
            ps.setDouble(27, k.getPreisProTermin());
            ps.executeUpdate();

            int neueNr;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                neueNr = keys.getInt(1);
            }
            k.setKundennummer(neueNr);

            // Keine automatische Termin-/Einsatz-Erstellung mehr beim Anlegen.
            // Termine werden bewusst über „Termine erzeugen" angelegt.
            AuditService.log("Angelegt", "Kunde", "Nr. " + neueNr + " – " + k.getVollstaendigerName());
            return neueNr;
        } catch (SQLException e) {
            throw new RuntimeException("Kunde hinzufügen fehlgeschlagen", e);
        }
    }

    /** Aktualisiert einen bestehenden Kunden (alle Felder, anhand der Kundennummer). */
    public void aktualisieren(Kunde k) {
        k.setVerbleibendeStunden(k.getGenehmigteStunden() - k.getErbrachteStunden());
        String sql = """
            UPDATE Kunden SET
              VollstaendigerName=?, Adresse=?, KrankenkasseNummer=?, Leistungsart=?,
              GenehmigteStunden=?, ErbrachteStunden=?, VerbleibendeStunden=?,
              ZustaendigerMitarbeiter=?, Notizen=?, IstAktiv=?, TerminPlan=?,
              Geburtsdatum=?, Telefon=?, EMail=?, Pflegegrad=?, PflegegradSeit=?,
              Vertragsbeginn=?, Abrechnungszeitraum=?, NaechsteAbrechnung=?,
              Wochentage=?, Uhrzeit=?, OrtBereich=?, Zugang=?, StandardTerminDauer=?,
              AbrechnungRhythmusMonate=?, LetzteAbrechnungBis=?, PreisProTermin=?
            WHERE Kundennummer=?
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, k.getVollstaendigerName());
            ps.setString(2, CryptoService.verschluesseln(k.getAdresse()));
            ps.setString(3, CryptoService.verschluesseln(k.getKrankenkasseNummer()));
            ps.setString(4, k.getLeistungsart());
            ps.setDouble(5, k.getGenehmigteStunden());
            ps.setDouble(6, k.getErbrachteStunden());
            ps.setDouble(7, k.getVerbleibendeStunden());
            ps.setString(8, k.getZustaendigerMitarbeiter());
            ps.setString(9, k.getNotizen());
            ps.setInt(10, k.isIstAktiv() ? 1 : 0);
            ps.setString(11, k.getTerminPlan());
            ps.setString(12, CryptoService.verschluesseln(k.getGeburtsdatum()));
            ps.setString(13, CryptoService.verschluesseln(k.getTelefon()));
            ps.setString(14, CryptoService.verschluesseln(k.getEMail()));
            ps.setString(15, CryptoService.verschluesseln(k.getPflegegrad()));
            ps.setString(16, k.getPflegegradSeit());
            ps.setString(17, k.getVertragsbeginn());
            ps.setString(18, k.getAbrechnungszeitraum());
            ps.setString(19, k.getNaechsteAbrechnung());
            ps.setString(20, k.getWochentage());
            ps.setString(21, k.getUhrzeit());
            ps.setString(22, k.getOrtBereich());
            ps.setString(23, k.getZugang());
            ps.setDouble(24, k.getStandardTerminDauer());
            ps.setInt(25, k.getAbrechnungRhythmusMonate());
            ps.setString(26, k.getLetzteAbrechnungBis());
            ps.setDouble(27, k.getPreisProTermin());
            ps.setInt(28, k.getKundennummer());
            ps.executeUpdate();
            AuditService.log("Bearbeitet", "Kunde", "Nr. " + k.getKundennummer() + " – " + k.getVollstaendigerName());
        } catch (SQLException e) {
            throw new RuntimeException("Kunde aktualisieren fehlgeschlagen", e);
        }
    }

    /** Löscht einen Kunden (samt zugehöriger Termine/Einsätze/Zahlungen via CASCADE). */
    public void loeschen(int kundennummer) {
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM Kunden WHERE Kundennummer=?")) {
            ps.setInt(1, kundennummer);
            ps.executeUpdate();
            AuditService.log("Gelöscht", "Kunde", "Nr. " + kundennummer);
        } catch (SQLException e) {
            throw new RuntimeException("Kunde löschen fehlgeschlagen", e);
        }
    }

    private Kunde mappe(ResultSet rs) throws SQLException {
        Kunde k = new Kunde();
        k.setKundennummer(rs.getInt("Kundennummer"));
        k.setVollstaendigerName(rs.getString("VollstaendigerName"));
        k.setAdresse(CryptoService.entschluesseln(rs.getString("Adresse")));
        k.setKrankenkasseNummer(CryptoService.entschluesseln(rs.getString("KrankenkasseNummer")));
        k.setLeistungsart(rs.getString("Leistungsart"));
        k.setGenehmigteStunden(rs.getDouble("GenehmigteStunden"));
        k.setErbrachteStunden(rs.getDouble("ErbrachteStunden"));
        k.setVerbleibendeStunden(rs.getDouble("VerbleibendeStunden"));
        k.setZustaendigerMitarbeiter(rs.getString("ZustaendigerMitarbeiter"));
        k.setNotizen(rs.getString("Notizen"));
        k.setIstAktiv(rs.getInt("IstAktiv") == 1);
        k.setTerminPlan(rs.getString("TerminPlan"));
        k.setGeburtsdatum(CryptoService.entschluesseln(rs.getString("Geburtsdatum")));
        k.setTelefon(CryptoService.entschluesseln(rs.getString("Telefon")));
        k.setEMail(CryptoService.entschluesseln(rs.getString("EMail")));
        k.setPflegegrad(CryptoService.entschluesseln(rs.getString("Pflegegrad")));
        k.setPflegegradSeit(rs.getString("PflegegradSeit"));
        k.setVertragsbeginn(rs.getString("Vertragsbeginn"));
        k.setAbrechnungszeitraum(rs.getString("Abrechnungszeitraum"));
        k.setNaechsteAbrechnung(rs.getString("NaechsteAbrechnung"));
        k.setWochentage(rs.getString("Wochentage"));
        k.setUhrzeit(rs.getString("Uhrzeit"));
        k.setOrtBereich(rs.getString("OrtBereich"));
        k.setZugang(rs.getString("Zugang"));
        try {
            double d = rs.getDouble("StandardTerminDauer");
            k.setStandardTerminDauer(d > 0 ? d : 1.5);
        } catch (SQLException ignored) { k.setStandardTerminDauer(1.5); }
        try {
            int r = rs.getInt("AbrechnungRhythmusMonate");
            k.setAbrechnungRhythmusMonate(r > 0 ? r : 3);
            k.setLetzteAbrechnungBis(rs.getString("LetzteAbrechnungBis"));
            k.setPreisProTermin(rs.getDouble("PreisProTermin"));
        } catch (SQLException ignored) { k.setAbrechnungRhythmusMonate(3); }
        return k;
    }
}
