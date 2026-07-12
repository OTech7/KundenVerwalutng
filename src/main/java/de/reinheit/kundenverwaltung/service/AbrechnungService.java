package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.dao.RechnungDao;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Rechnung;

import java.time.LocalDate;

/**
 * Abrechnungslogik (Rechnung an die Krankenkasse):
 *
 *  - Der aktuelle Abrechnungszeitraum beginnt am Tag nach der letzten
 *    abgerechneten Periode. Gibt es noch keine, beginnt er am Vertragsbeginn
 *    (bzw. hilfsweise am 1. des aktuellen Monats).
 *  - Er dauert so viele Monate wie im Kunden hinterlegt (Standard: 3).
 *  - Im Zeitraum werden geplante Termine und tatsächlich erbrachte Einsätze gezählt.
 *  - Beim Erstellen der Rechnung wird die Periode fortgeschrieben, sodass
 *    automatisch die "nächste Abrechnung" entsteht.
 */
public class AbrechnungService {

    private final RechnungDao dao = new RechnungDao();

    /** Ergebnis-Objekt für die Anzeige eines Abrechnungszeitraums. */
    public static class Zeitraum {
        public final LocalDate von;
        public final LocalDate bis;
        public final int geplant;
        public final int erbracht;
        public final double betrag;

        Zeitraum(LocalDate von, LocalDate bis, int geplant, int erbracht, double betrag) {
            this.von = von; this.bis = bis;
            this.geplant = geplant; this.erbracht = erbracht; this.betrag = betrag;
        }
        /** Nicht wahrgenommene Termine im Zeitraum. */
        public int offen() { return Math.max(0, geplant - erbracht); }
    }

    /** Startdatum des aktuellen (noch nicht abgerechneten) Zeitraums. */
    public LocalDate aktuellerStart(Kunde k) {
        String letzte = k.getLetzteAbrechnungBis();
        if (letzte != null && !letzte.isBlank()) {
            try { return LocalDate.parse(letzte).plusDays(1); } catch (Exception ignored) {}
        }
        String beginn = k.getVertragsbeginn();
        if (beginn != null && !beginn.isBlank()) {
            try { return LocalDate.parse(beginn); } catch (Exception ignored) {}
        }
        return LocalDate.now().withDayOfMonth(1);
    }

    /** Enddatum = Start + Rhythmus (Monate) − 1 Tag. */
    public LocalDate endeVon(LocalDate start, Kunde k) {
        int monate = k.getAbrechnungRhythmusMonate() > 0 ? k.getAbrechnungRhythmusMonate() : 3;
        return start.plusMonths(monate).minusDays(1);
    }

    /** Der aktuelle Abrechnungszeitraum inkl. Zählungen. */
    public Zeitraum aktuellerZeitraum(Kunde k) {
        LocalDate von = aktuellerStart(k);
        LocalDate bis = endeVon(von, k);
        int geplant = dao.zaehleGeplant(k.getKundennummer(), von, bis);
        int erbracht = dao.zaehleErbracht(k.getKundennummer(), von, bis);
        // Der Betrag wird erst bei der Rechnungserstellung mit dem eingegebenen Preis berechnet.
        return new Zeitraum(von, bis, geplant, erbracht, 0);
    }

    /** Der darauffolgende Zeitraum (Vorschau: „nächste Abrechnung"). */
    public Zeitraum naechsterZeitraum(Kunde k) {
        LocalDate von = endeVon(aktuellerStart(k), k).plusDays(1);
        LocalDate bis = endeVon(von, k);
        int geplant = dao.zaehleGeplant(k.getKundennummer(), von, bis);
        int erbracht = dao.zaehleErbracht(k.getKundennummer(), von, bis);
        return new Zeitraum(von, bis, geplant, erbracht, 0);
    }

    /** Gesamt: geplante Termine insgesamt. */
    public int geplantGesamt(Kunde k)  { return dao.zaehleGeplantGesamt(k.getKundennummer()); }
    /** Gesamt: erbrachte (genutzte) Termine insgesamt. */
    public int erbrachtGesamt(Kunde k) { return dao.zaehleErbrachtGesamt(k.getKundennummer()); }
    /** Gesamt: noch nicht genutzte Termine. */
    public int verbleibendGesamt(Kunde k) { return Math.max(0, geplantGesamt(k) - erbrachtGesamt(k)); }

    /** Zeitraum mit Zählungen für ein frei gewähltes Von–Bis (z. B. ein Quartal). */
    public Zeitraum fuer(Kunde k, LocalDate von, LocalDate bis) {
        int geplant = dao.zaehleGeplant(k.getKundennummer(), von, bis);
        int erbracht = dao.zaehleErbracht(k.getKundennummer(), von, bis);
        return new Zeitraum(von, bis, geplant, erbracht, 0);
    }

    /**
     * Erstellt die Rechnung für einen frei gewählten Zeitraum (z. B. ein Quartal).
     * @param preisProTermin Preis je erbrachtem Termin (0 = ohne Betrag, nur Anzahl).
     */
    public Rechnung abrechnen(Kunde k, LocalDate von, LocalDate bis, double preisProTermin) {
        Zeitraum z = fuer(k, von, bis);
        Rechnung r = new Rechnung();
        r.setKundennummer(k.getKundennummer());
        r.setZeitraumVon(von.toString());
        r.setZeitraumBis(bis.toString());
        r.setAnzahlGeplant(z.geplant);
        r.setAnzahlErbracht(z.erbracht);
        r.setBetrag(z.erbracht * preisProTermin);
        r.setStatus("Abgerechnet");
        r.setErstelltAm(LocalDate.now().toString());
        int id = dao.erstellen(r);
        r.setId(id);
        return r;
    }
}
