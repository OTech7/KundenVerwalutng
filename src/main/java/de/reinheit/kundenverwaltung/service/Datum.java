package de.reinheit.kundenverwaltung.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Einheitliche Datumsbehandlung.
 *
 *  - Anzeige (Oberfläche, Berichte, Word): <b>TT.MM.JJJJ</b>  z. B. 31.01.2026
 *  - Speicherung (Datenbank):              <b>JJJJ-MM-TT</b>  z. B. 2026-01-31
 *
 * Beim Einlesen werden beide Schreibweisen akzeptiert, damit alte Daten und
 * Eingaben im Anzeigeformat gleichermaßen funktionieren.
 */
public final class Datum {

    /** Hinweistext für Eingabefelder. */
    public static final String HINWEIS = "TT.MM.JJJJ (z. B. 31.01.2026)";

    private static final DateTimeFormatter ANZEIGE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Locale DE = Locale.GERMAN;

    private Datum() {}

    /** Parst ein Datum aus Anzeige- (TT.MM.JJJJ) oder ISO-Format. null bei ungültig/leer. */
    public static LocalDate parse(String text) {
        if (text == null || text.isBlank()) return null;
        String s = text.trim();
        try { return LocalDate.parse(s, ANZEIGE); } catch (Exception ignored) { }
        try { return LocalDate.parse(s); } catch (Exception ignored) { }
        return null;
    }

    /** Anzeigeformat TT.MM.JJJJ. Leere/ungültige Werte werden unverändert zurückgegeben. */
    public static String anzeige(String text) {
        LocalDate d = parse(text);
        return d == null ? (text == null ? "" : text) : d.format(ANZEIGE);
    }

    /** Anzeigeformat TT.MM.JJJJ. */
    public static String anzeige(LocalDate d) {
        return d == null ? "" : d.format(ANZEIGE);
    }

    /** Speicherformat JJJJ-MM-TT. null, wenn die Eingabe leer oder ungültig ist. */
    public static String iso(String text) {
        LocalDate d = parse(text);
        return d == null ? null : d.toString();
    }

    /** Ausgeschriebener Wochentag, z. B. "Mittwoch". Leer bei ungültigem Datum. */
    public static String wochentag(String text) {
        LocalDate d = parse(text);
        return d == null ? "" : d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, DE);
    }

    /** StringConverter für JavaFX-DatePicker (Anzeige TT.MM.JJJJ). */
    public static javafx.util.StringConverter<LocalDate> konverter() {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(LocalDate d) { return anzeige(d); }
            @Override public LocalDate fromString(String s) { return parse(s); }
        };
    }
}
