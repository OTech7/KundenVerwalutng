package de.reinheit.kundenverwaltung.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Zentrale Meldungs- und Fehlerbehandlung für die Oberfläche.
 * Technische Details gehen ins Log, dem Benutzer wird eine verständliche
 * Meldung angezeigt (siehe Sicherheitsaudit L-4).
 */
public final class Meldung {

    private Meldung() {}

    public static void info(String text) {
        zeige(Alert.AlertType.INFORMATION, "Hinweis", text);
    }

    public static void warnung(String text) {
        zeige(Alert.AlertType.WARNING, "Achtung", text);
    }

    public static void fehler(String text) {
        zeige(Alert.AlertType.ERROR, "Fehler", text);
    }

    /** Wandelt eine Ausnahme in eine benutzerfreundliche Meldung um. */
    public static void fehler(String text, Throwable t) {
        System.err.println(text + " :: " + t);
        if (t instanceof SecurityException) {
            zeige(Alert.AlertType.WARNING, "Keine Berechtigung", t.getMessage());
            return;
        }
        zeige(Alert.AlertType.ERROR, "Fehler", text);
    }

    /**
     * Globaler Handler: verhindert, dass unerwartete Fehler die App abstürzen lassen.
     * Muss aus dem JavaFX-Application-Thread aufgerufen werden, damit auch dessen
     * Ausnahmen abgefangen werden.
     */
    public static void installiereGlobalenHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> behandle(t));
        Thread.currentThread().setUncaughtExceptionHandler((thread, t) -> behandle(t));
    }

    private static void behandle(Throwable t) {
        Throwable ursache = wurzel(t);
        System.err.println("Unerwarteter Fehler: " + ursache);
        t.printStackTrace();
        if (ursache instanceof SecurityException) {
            zeige(Alert.AlertType.WARNING, "Keine Berechtigung", ursache.getMessage());
        } else if (ursache instanceof IllegalStateException && ursache.getMessage() != null) {
            // Bekannte, für den Benutzer verständliche Fehler (z. B. Schlüsseldatei fehlt)
            zeige(Alert.AlertType.ERROR, "Fehler", ursache.getMessage());
        } else {
            zeige(Alert.AlertType.ERROR, "Fehler",
                    "Es ist ein unerwarteter Fehler aufgetreten. Die Aktion wurde nicht ausgeführt.");
        }
    }

    /** Ursprüngliche Ursache einer verschachtelten Ausnahme. */
    private static Throwable wurzel(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) c = c.getCause();
        return c;
    }

    private static void zeige(Alert.AlertType typ, String kopf, String text) {
        Runnable r = () -> {
            Alert a = new Alert(typ, text == null ? "" : text);
            a.setHeaderText(kopf);
            a.showAndWait();
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
