package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.db.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Liefert die Statistiken des Dashboards (laut Spezifikation):
 * aktive/ehemalige Kunden, erledigte/nicht erledigte Einsätze,
 * bezahlte Zahlungen, Gesamtstunden, Termine im aktuellen Monat.
 */
public class DashboardService {

    public int aktiveKunden()        { return zahl("SELECT COUNT(*) FROM Kunden WHERE IstAktiv = 1"); }
    public int ehemaligeKunden()     { return zahl("SELECT COUNT(*) FROM Kunden WHERE IstAktiv = 0"); }
    public int erledigteEinsaetze()  { return zahl("SELECT COUNT(*) FROM Einsaetze WHERE Einsatzstatus = 'Erledigt'"); }
    public int nichtErledigteEinsaetze() { return zahl("SELECT COUNT(*) FROM Einsaetze WHERE Einsatzstatus <> 'Erledigt'"); }
    public int bezahlteZahlungen()   { return zahl("SELECT COUNT(*) FROM Zahlungen WHERE Zahlungsstatus = 'Bezahlt'"); }
    public int termineImMonat()      { return zahl("SELECT COUNT(*) FROM Termine WHERE strftime('%Y-%m', TerminDatum) = strftime('%Y-%m','now')"); }

    public double gesamtstundenGeleistet() {
        return wert("SELECT COALESCE(SUM(EinsatzdauerStunden),0) FROM Einsaetze WHERE Einsatzstatus = 'Erledigt'");
    }

    private int zahl(String sql) { return (int) wert(sql); }

    private double wert(String sql) {
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Statistik fehlgeschlagen: " + sql, e);
        }
    }
}
