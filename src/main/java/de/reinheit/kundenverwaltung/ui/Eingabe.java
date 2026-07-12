package de.reinheit.kundenverwaltung.ui;

import java.time.LocalDate;

/**
 * Hilfsfunktionen zur Prüfung von Benutzereingaben.
 * Liefern entweder einen gültigen Wert oder eine Fehlermeldung (null = ok).
 */
public final class Eingabe {

    private Eingabe() {}

    /** true, wenn der Text leer/null ist. */
    public static boolean leer(String s) {
        return s == null || s.isBlank();
    }

    /** Parst eine Dezimalzahl (Komma oder Punkt). Gibt null zurück, wenn ungültig. */
    public static Double zahl(String s) {
        if (leer(s)) return null;
        try {
            return Double.valueOf(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parst eine Ganzzahl. Gibt null zurück, wenn ungültig. */
    public static Integer ganzzahl(String s) {
        if (leer(s)) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Prüft eine Dezimalzahl auf Gültigkeit und Wertebereich.
     * @return Fehlermeldung oder null, wenn alles in Ordnung ist.
     */
    public static String pruefeZahl(String s, String feld, double min, double max) {
        Double d = zahl(s);
        if (d == null) return feld + ": bitte eine gültige Zahl eingeben.";
        if (d < min || d > max) return feld + ": Wert muss zwischen " + min + " und " + max + " liegen.";
        return null;
    }

    /** Wie {@link #pruefeZahl}, aber ein leeres Feld ist erlaubt (wird als 0 gewertet). */
    public static String pruefeZahlOptional(String s, String feld, double min, double max) {
        if (leer(s)) return null;
        return pruefeZahl(s, feld, min, max);
    }

    /** Prüft eine Ganzzahl auf Gültigkeit und Wertebereich. */
    public static String pruefeGanzzahl(String s, String feld, int min, int max) {
        Integer i = ganzzahl(s);
        if (i == null) return feld + ": bitte eine gültige ganze Zahl eingeben.";
        if (i < min || i > max) return feld + ": Wert muss zwischen " + min + " und " + max + " liegen.";
        return null;
    }

    /** Parst ein Datum (TT.MM.JJJJ oder JJJJ-MM-TT). Gibt null zurück, wenn ungültig. */
    public static LocalDate datum(String s) {
        return de.reinheit.kundenverwaltung.service.Datum.parse(s);
    }

    /**
     * Prüft ein optionales Datumsfeld: leer ist erlaubt, aber wenn gefüllt,
     * muss es das Format TT.MM.JJJJ haben.
     */
    public static String pruefeDatumOptional(String s, String feld) {
        if (leer(s)) return null;
        return datum(s) == null
                ? feld + ": bitte im Format " + de.reinheit.kundenverwaltung.service.Datum.HINWEIS + " eingeben."
                : null;
    }
}
