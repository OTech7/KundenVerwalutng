package de.reinheit.kundenverwaltung.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Legt fest, wo die Anwendungsdaten (Datenbank + Schlüsseldatei) gespeichert werden.
 *
 * Wichtig für die installierte EXE-Version: Das Installationsverzeichnis
 * (z. B. C:\Program Files\...) ist für normale Benutzer nicht beschreibbar.
 * Deshalb liegen die Daten im Benutzerprofil:
 *
 *   Windows: %APPDATA%\KundenVerwaltung\
 *   sonst:   ~/.config/KundenVerwaltung/
 *
 * Beim ersten Start werden vorhandene Dateien aus dem Arbeitsverzeichnis
 * (Entwicklungsmodus) automatisch dorthin übernommen – es gehen keine Daten verloren.
 */
public final class AppDaten {

    private static final String ORDNER = "KundenVerwaltung";

    private AppDaten() {}

    /** Datenverzeichnis; wird bei Bedarf angelegt. */
    public static Path verzeichnis() {
        String appdata = System.getenv("APPDATA");
        Path basis = (appdata != null && !appdata.isBlank())
                ? Path.of(appdata)
                : Path.of(System.getProperty("user.home"), ".config");
        Path dir = basis.resolve(ORDNER);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Das Datenverzeichnis '" + dir + "' konnte nicht angelegt werden. "
                    + "Bitte Schreibrechte prüfen.", e);
        }
        return dir;
    }

    /** Vollständiger Pfad einer Datei im Datenverzeichnis. */
    public static Path datei(String name) {
        return verzeichnis().resolve(name);
    }

    /**
     * Übernimmt eine Datei aus dem aktuellen Arbeitsverzeichnis ins Datenverzeichnis,
     * falls dort noch keine existiert (einmalige Migration von alten Installationen).
     */
    public static void migriereAusArbeitsverzeichnis(String name) {
        try {
            Path ziel = datei(name);
            if (Files.exists(ziel)) return;          // schon migriert
            Path alt = Path.of(name);
            if (!Files.exists(alt)) return;          // nichts zu migrieren
            Files.copy(alt, ziel, StandardCopyOption.COPY_ATTRIBUTES);
            System.out.println("Datei '" + name + "' wurde nach " + ziel + " übernommen.");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Die vorhandene Datei '" + name + "' konnte nicht ins Datenverzeichnis "
                    + "übernommen werden.", e);
        }
    }
}
