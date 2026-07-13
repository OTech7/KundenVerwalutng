package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.model.Einsatz;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Termin;
import de.reinheit.kundenverwaltung.service.AuditService;
import de.reinheit.kundenverwaltung.service.StundenService;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datenzugriff für Einsätze. Beim Hinzufügen eines Einsatzes mit Status
 * "Erledigt" werden automatisch die erbrachten/verbleibenden Stunden des
 * Kunden aktualisiert (StundenService).
 */
public class EinsatzDao {

    private final StundenService stundenService = new StundenService();

    /**
     * Erzeugt aus mehreren Terminen jeweils EINEN Sammel-Einsatz pro Kunde.
     * Beispiel: 5 Termine eines Kunden ergeben 1 Einsatz.
     *  - Dauer   = Summe der Termindauern (z. B. 5 × 1,5 h = 7,5 h)
     *  - Datum   = frühester Termin; der Zeitraum (von–bis) steht in den Notizen
     *  - Mitarbeiter / Leistungsart = vom Kunden übernommen
     *  - Status  = leer (offen); es werden KEINE Stunden gebucht, bis der Einsatz
     *              auf „Erledigt" gesetzt wird.
     *
     * @return Anzahl der erzeugten Einsätze (= Anzahl der betroffenen Kunden).
     */
    public int erzeugeAusTerminen(List<Termin> termine) {
        if (termine == null || termine.isEmpty()) return 0;

        // Kundendaten (Mitarbeiter, Leistungsart) einmalig laden
        Map<Integer, Kunde> kunden = new HashMap<>();
        for (Kunde k : new KundeDao().findeAlle()) kunden.put(k.getKundennummer(), k);

        // Termine nach Kunde gruppieren
        Map<Integer, List<Termin>> nachKunde = new HashMap<>();
        for (Termin t : termine) {
            nachKunde.computeIfAbsent(t.getKundennummer(), x -> new ArrayList<>()).add(t);
        }

        String sql = """
            INSERT INTO Einsaetze
            (Kundennummer, Einsatzdatum, Mitarbeiter, EinsatzdauerStunden, Leistungsart, Einsatzstatus, Notizen, ZeitraumBis)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        int erzeugt = 0;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            for (Map.Entry<Integer, List<Termin>> e : nachKunde.entrySet()) {
                int kundennummer = e.getKey();
                List<Termin> liste = e.getValue();

                double summe = 0;
                String min = null, max = null;
                for (Termin t : liste) {
                    summe += t.getDauer();
                    String d = t.getTerminDatum();
                    if (d != null) {
                        if (min == null || d.compareTo(min) < 0) min = d;   // ISO -> lexikografisch = chronologisch
                        if (max == null || d.compareTo(max) > 0) max = d;
                    }
                }

                Kunde k = kunden.get(kundennummer);
                String zeitraum = de.reinheit.kundenverwaltung.service.Datum.anzeige(min)
                        + " – " + de.reinheit.kundenverwaltung.service.Datum.anzeige(max);
                String notiz = "Sammel-Einsatz für " + liste.size() + " Termine (" + zeitraum + ")";

                ps.setInt(1, kundennummer);
                ps.setString(2, min);                                   // frühester Termin
                ps.setString(3, k != null ? k.getZustaendigerMitarbeiter() : null);
                ps.setDouble(4, summe);                                 // Summe der Dauern
                ps.setString(5, k != null ? k.getLeistungsart() : null);
                ps.setString(6, "");                                    // Status offen
                ps.setString(7, notiz);
                ps.setString(8, max);                                   // spätester Termin (Zeitraum-Ende)
                ps.addBatch();
                erzeugt++;
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            throw new RuntimeException("Einsätze aus Terminen erzeugen fehlgeschlagen", ex);
        }
        if (erzeugt > 0) AuditService.log("Angelegt", "Einsatz", erzeugt + " Sammel-Einsatz/-Einsätze aus Terminen");
        return erzeugt;
    }

    /**
     * Setzt jeden Sammel-Einsatz automatisch auf „Erledigt", sobald ALLE seine
     * Termine erledigt oder abgesagt sind; sonst bleibt der Status leer.
     * „Termine des Einsatzes" = Termine des Kunden im Zeitraum [Einsatzdatum … ZeitraumBis].
     * Bucht keine Stunden (die kommen aus den Terminen).
     * @return Anzahl der geänderten Einsätze
     */
    public int aktualisiereStatusAusTerminen() {
        String sql = """
            UPDATE Einsaetze
            SET Einsatzstatus = CASE
              WHEN (SELECT COUNT(*) FROM Termine t
                    WHERE t.Kundennummer = Einsaetze.Kundennummer
                      AND t.TerminDatum BETWEEN Einsaetze.Einsatzdatum AND Einsaetze.ZeitraumBis) > 0
               AND (SELECT COUNT(*) FROM Termine t
                    WHERE t.Kundennummer = Einsaetze.Kundennummer
                      AND t.TerminDatum BETWEEN Einsaetze.Einsatzdatum AND Einsaetze.ZeitraumBis
                      AND (t.Status IS NULL OR t.Status NOT IN ('Erledigt','Abgesagt'))) = 0
              THEN 'Erledigt' ELSE '' END
            WHERE ZeitraumBis IS NOT NULL
            """;
        try (Statement st = Database.get().createStatement()) {
            return st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Einsatz-Status aus Terminen aktualisieren fehlgeschlagen", e);
        }
    }

    public List<Einsatz> findeAlle() {
        List<Einsatz> list = new ArrayList<>();
        String sql = "SELECT * FROM Einsaetze ORDER BY Id DESC";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mappe(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Einsätze laden fehlgeschlagen", e);
        }
        return list;
    }

    /** Einsatz speichern; bei "Erledigt" Stunden automatisch verbuchen. */
    public void hinzufuegen(Einsatz e) {
        String sql = """
            INSERT INTO Einsaetze
            (Kundennummer, Einsatzdatum, Mitarbeiter, EinsatzdauerStunden,
             Leistungsart, Einsatzstatus, Notizen)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, e.getKundennummer());
            ps.setString(2, e.getEinsatzdatum());
            ps.setString(3, e.getMitarbeiter());
            ps.setDouble(4, e.getEinsatzdauerStunden());
            ps.setString(5, e.getLeistungsart());
            ps.setString(6, e.getEinsatzstatus());
            ps.setString(7, e.getNotizen());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Einsatz hinzufügen fehlgeschlagen", ex);
        }
        // --- Automatische Stundenbuchung ---
        if ("Erledigt".equals(e.getEinsatzstatus())) {
            stundenService.einsatzVerbuchen(e.getKundennummer(), e.getEinsatzdauerStunden());
        }
        AuditService.log("Angelegt", "Einsatz", "Kunde #" + e.getKundennummer() + " · " + e.getEinsatzstatus());
    }

    /**
     * Aktualisiert einen Einsatz. Die gebuchten Stunden des Kunden werden um die
     * Differenz (neu − alt) korrigiert, damit nichts doppelt gezählt wird.
     * Gebuchte Stunden = Dauer, wenn Status „Erledigt“, sonst 0.
     */
    public void aktualisieren(Einsatz alt, Einsatz neu) {
        String sql = """
            UPDATE Einsaetze SET
              Kundennummer=?, Einsatzdatum=?, Mitarbeiter=?, EinsatzdauerStunden=?,
              Leistungsart=?, Einsatzstatus=?, Notizen=?
            WHERE Id=?
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, neu.getKundennummer());
            ps.setString(2, neu.getEinsatzdatum());
            ps.setString(3, neu.getMitarbeiter());
            ps.setDouble(4, neu.getEinsatzdauerStunden());
            ps.setString(5, neu.getLeistungsart());
            ps.setString(6, neu.getEinsatzstatus());
            ps.setString(7, neu.getNotizen());
            ps.setInt(8, alt.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Einsatz aktualisieren fehlgeschlagen", ex);
        }
        // Stunden-Korrektur (Delta)
        double altBuchung = "Erledigt".equals(alt.getEinsatzstatus()) ? alt.getEinsatzdauerStunden() : 0;
        double neuBuchung = "Erledigt".equals(neu.getEinsatzstatus()) ? neu.getEinsatzdauerStunden() : 0;
        double delta = neuBuchung - altBuchung;
        if (delta != 0 && alt.getKundennummer() == neu.getKundennummer()) {
            stundenService.einsatzVerbuchen(neu.getKundennummer(), delta);
        } else if (alt.getKundennummer() != neu.getKundennummer()) {
            // Kunde gewechselt: alte Buchung beim alten Kunden zurücknehmen, neue beim neuen
            if (altBuchung != 0) stundenService.einsatzVerbuchen(alt.getKundennummer(), -altBuchung);
            if (neuBuchung != 0) stundenService.einsatzVerbuchen(neu.getKundennummer(), neuBuchung);
        }
        AuditService.log("Bearbeitet", "Einsatz", "Nr. " + alt.getId());
    }

    /** Löscht einen Einsatz und nimmt ggf. gebuchte Stunden zurück. */
    public void loeschen(Einsatz e) {
        try (PreparedStatement ps = Database.get().prepareStatement("DELETE FROM Einsaetze WHERE Id=?")) {
            ps.setInt(1, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Einsatz löschen fehlgeschlagen", ex);
        }
        if ("Erledigt".equals(e.getEinsatzstatus())) {
            stundenService.einsatzVerbuchen(e.getKundennummer(), -e.getEinsatzdauerStunden());
        }
        AuditService.log("Gelöscht", "Einsatz", "Nr. " + e.getId());
    }

    private Einsatz mappe(ResultSet rs) throws SQLException {
        Einsatz e = new Einsatz();
        e.setId(rs.getInt("Id"));
        e.setKundennummer(rs.getInt("Kundennummer"));
        e.setEinsatzdatum(rs.getString("Einsatzdatum"));
        e.setMitarbeiter(rs.getString("Mitarbeiter"));
        e.setEinsatzdauerStunden(rs.getDouble("EinsatzdauerStunden"));
        e.setLeistungsart(rs.getString("Leistungsart"));
        e.setEinsatzstatus(rs.getString("Einsatzstatus"));
        e.setNotizen(rs.getString("Notizen"));
        return e;
    }
}
