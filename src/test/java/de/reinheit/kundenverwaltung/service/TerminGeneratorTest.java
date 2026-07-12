package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Termin;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TerminGeneratorTest {

    private final TerminGenerator gen = new TerminGenerator();

    private Kunde kunde(String plan) {
        Kunde k = new Kunde();
        k.setKundennummer(1);
        k.setTerminPlan(plan);
        return k;
    }

    @Test
    void woche1und3ErzeugtZweiTermine() {
        List<Termin> t = gen.fuerMonat(kunde("Woche 1 und 3"), YearMonth.of(2026, 7));
        assertEquals(2, t.size());
        // Tag (w-1)*7+3 -> Woche1 = 3., Woche3 = 17.
        assertEquals("2026-07-03", t.get(0).getTerminDatum());
        assertEquals("2026-07-17", t.get(1).getTerminDatum());
        assertEquals("Woche 1", t.get(0).getWoche());
        assertEquals("Woche 3", t.get(1).getWoche());
    }

    @Test
    void woche2und4ErzeugtZweiTermine() {
        List<Termin> t = gen.fuerMonat(kunde("Woche 2 und 4"), YearMonth.of(2026, 7));
        assertEquals(2, t.size());
        assertEquals("2026-07-10", t.get(0).getTerminDatum());
        assertEquals("2026-07-24", t.get(1).getTerminDatum());
    }

    @Test
    void jederTerminDauert15Stunden() {
        List<Termin> t = gen.fuerMonat(kunde("Woche 1 und 3"), YearMonth.of(2026, 7));
        assertTrue(t.stream().allMatch(x -> x.getDauer() == 1.5));
    }

    @Test
    void terminGehoertZumRichtigenKunden() {
        List<Termin> t = gen.fuerMonat(kunde("Woche 2 und 4"), YearMonth.of(2026, 7));
        assertTrue(t.stream().allMatch(x -> x.getKundennummer() == 1));
    }
}
