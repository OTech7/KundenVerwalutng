package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Termin;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Erzeugt automatisch Termine aus dem TerminPlan eines Kunden.
 *  - "Woche 1 und 3"  -> Termine in Woche 1 und 3 des Monats (Tag 3 und 17)
 *  - "Woche 2 und 4"  -> Termine in Woche 2 und 4 des Monats (Tag 10 und 24)
 *  - "Wöchentlich"    -> ein Termin pro Woche (Tag 3, 10, 17, 24)
 * Dauer je Termin: Standard-Termindauer des Kunden (Fallback 1,5 h).
 */
public class TerminGenerator {

    public static final double DAUER = 1.5;

    /** Tage im Monat je nach Plan. */
    private int[] tageFuerPlan(String plan) {
        if (plan == null) return new int[]{10, 24};
        if (plan.contains("1 und 3")) return new int[]{3, 17};
        if (plan.contains("2 und 4")) return new int[]{10, 24};
        if (plan.toLowerCase().contains("wöch") || plan.toLowerCase().contains("woech"))
            return new int[]{3, 10, 17, 24};
        return new int[]{10, 24};
    }

    private double dauerVon(Kunde k) {
        double d = k.getStandardTerminDauer();
        return d > 0 ? d : DAUER;
    }

    /** Erzeugt die Termine eines Kunden für einen bestimmten Monat. */
    public List<Termin> fuerMonat(Kunde kunde, YearMonth monat) {
        List<Termin> result = new ArrayList<>();
        double dauer = dauerVon(kunde);
        int[] tage = tageFuerPlan(kunde.getTerminPlan());
        for (int tag : tage) {
            if (tag <= monat.lengthOfMonth()) {
                LocalDate datum = monat.atDay(tag);
                Termin t = new Termin(
                        kunde.getKundennummer(),
                        datum.toString(),
                        dauer,
                        wocheLabel(tag),
                        "Auto-Termin nach Plan „" + kunde.getTerminPlan() + "“"
                );
                // Nur vergangene Termine sind automatisch „Erledigt"; zukünftige bleiben offen.
                if (datum.isBefore(LocalDate.now())) t.setStatus("Erledigt");
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Erzeugt Termine für einen Kunden im Zeitraum [von, bis] nach dem Plan.
     * Die Dauer wird explizit übergeben (überschreibt den Kundenstandard).
     */
    public List<Termin> fuerZeitraum(Kunde kunde, LocalDate von, LocalDate bis, double dauer, String plan) {
        List<Termin> result = new ArrayList<>();
        if (von == null || bis == null || bis.isBefore(von)) return result;
        if (dauer <= 0) dauer = dauerVon(kunde);
        boolean woechentlich = plan != null && (plan.toLowerCase().contains("wöch") || plan.toLowerCase().contains("woech"));

        if (woechentlich) {
            for (LocalDate d = von; !d.isAfter(bis); d = d.plusWeeks(1)) {
                result.add(new Termin(kunde.getKundennummer(), d.toString(), dauer,
                        "Wöchentlich", "Auto-Termin (wöchentlich)"));
            }
            return result;
        }

        int[] tage = tageFuerPlan(plan);
        YearMonth m = YearMonth.from(von);
        YearMonth ende = YearMonth.from(bis);
        while (!m.isAfter(ende)) {
            for (int tag : tage) {
                if (tag <= m.lengthOfMonth()) {
                    LocalDate d = m.atDay(tag);
                    if (!d.isBefore(von) && !d.isAfter(bis)) {
                        result.add(new Termin(kunde.getKundennummer(), d.toString(), dauer,
                                wocheLabel(tag), "Auto-Termin nach Plan „" + plan + "“"));
                    }
                }
            }
            m = m.plusMonths(1);
        }
        return result;
    }

    private String wocheLabel(int tag) {
        return "Woche " + ((tag - 1) / 7 + 1);
    }

    // ----------------------------------------------------------------------
    // Neue Variante: fester Wochentag + Wochenmuster (nach Kalenderwoche im Monat)
    //   Woche 1 = Tag 1–7, Woche 2 = 8–14, Woche 3 = 15–21, Woche 4 = 22–28,
    //   (Woche 5 = 29–31, nur bei „Wöchentlich").
    //   Plan „1 und 3" -> Wochen {1,3}; „2 und 4" -> {2,4}; „Wöchentlich" -> alle.
    // Neu erzeugte Termine sind standardmäßig „Erledigt".
    // ----------------------------------------------------------------------

    /** Wochen im Monat, in denen ein Termin liegen soll. */
    private Set<Integer> wochenFuerPlan(String plan) {
        if (plan != null && plan.contains("1 und 3")) return Set.of(1, 3);
        if (plan != null && plan.contains("2 und 4")) return Set.of(2, 4);
        if (plan != null && plan.toLowerCase().contains("monat")) return Set.of(1);   // Monatlich: 1× pro Monat
        return Set.of(1, 2, 3, 4, 5);   // Wöchentlich
    }

    /**
     * Erzeugt Termine für einen Kunden im Zeitraum [von, bis] an einem festen
     * Wochentag, gefiltert nach dem Wochenmuster des Plans.
     * @param tag fester Wochentag (bleibt über den ganzen Zeitraum gleich)
     */
    public List<Termin> fuerZeitraum(Kunde kunde, LocalDate von, LocalDate bis,
                                     double dauer, String plan, DayOfWeek tag) {
        List<Termin> result = new ArrayList<>();
        if (von == null || bis == null || bis.isBefore(von) || tag == null) return result;
        if (dauer <= 0) dauer = dauerVon(kunde);

        Set<Integer> wochen = wochenFuerPlan(plan);
        // Erstes Vorkommen des Wochentags ab „von"
        LocalDate d = von.with(TemporalAdjusters.nextOrSame(tag));
        for (; !d.isAfter(bis); d = d.plusWeeks(1)) {
            int wocheImMonat = (d.getDayOfMonth() - 1) / 7 + 1;
            if (!wochen.contains(wocheImMonat)) continue;
            Termin t = new Termin(kunde.getKundennummer(), d.toString(), dauer,
                    "Woche " + wocheImMonat, "Auto-Termin nach Plan „" + plan + "“");
            // Nur vergangene Termine sind automatisch „Erledigt"; zukünftige bleiben offen.
            if (d.isBefore(LocalDate.now())) t.setStatus("Erledigt");
            result.add(t);
        }
        return result;
    }
}
