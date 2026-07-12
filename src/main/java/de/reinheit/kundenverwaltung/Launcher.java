package de.reinheit.kundenverwaltung;

/**
 * Start-Klasse für die Ausführung über den IntelliJ-Run-Button (grünes ▶).
 *
 * Wenn man die JavaFX-Hauptklasse (die Application erweitert) direkt startet,
 * verlangt die JVM JavaFX auf dem Module-Path und meldet sonst
 * "JavaFX runtime components are missing". Diese Launcher-Klasse erweitert
 * NICHT Application und umgeht diese Prüfung – JavaFX läuft dann vom Classpath.
 *
 * Tipp: In IntelliJ diese Klasse (Launcher) per ▶ starten, ODER über Maven:
 *   mvn javafx:run
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
