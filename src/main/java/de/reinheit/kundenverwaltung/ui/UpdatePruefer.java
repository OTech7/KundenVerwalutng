package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.service.Version;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prüft beim Programmstart im Hintergrund, ob auf GitHub eine neuere Version
 * veröffentlicht wurde. Bei Bestätigung wird das Update automatisch
 * heruntergeladen, installiert und die App neu gestartet.
 *
 * Ohne Internet oder ohne Veröffentlichung passiert nichts (still).
 */
public final class UpdatePruefer {

    // ┌──────────────────────────────────────────────────────────────────┐
    // │ WICHTIG: Hier exakt "Kontoname/Repository-Name" eintragen –        │
    // │ genau wie in der GitHub-Adresse (Groß-/Kleinschreibung beachten).  │
    // └──────────────────────────────────────────────────────────────────┘
    private static final String REPO = "OTech7/KundenVerwalutng";

    private UpdatePruefer() {}

    /** Startet die Prüfung in einem Hintergrund-Thread (blockiert den Start nicht). */
    public static void pruefeImHintergrund() {
        pruefe(false);
    }

    /** Manuelle Prüfung (Menüpunkt): zeigt immer eine Rückmeldung. */
    public static void pruefeManuell() {
        pruefe(true);
    }

    private static void pruefe(boolean mitRueckmeldung) {
        Thread t = new Thread(() -> {
            try {
                String json = hole("https://api.github.com/repos/" + REPO + "/releases/latest");
                if (json == null) {
                    if (mitRueckmeldung) zeigeInfo("Es wurde keine Veröffentlichung gefunden.\n"
                            + "Prüfen Sie den Repository-Namen und die Internetverbindung.");
                    return;
                }
                String tag = feld(json, "tag_name");
                String seiteUrl = feld(json, "html_url");
                String zipUrl = zipDownloadUrl(json);
                if (tag == null) {
                    if (mitRueckmeldung) zeigeInfo("Es wurde noch keine Version veröffentlicht.");
                    return;
                }
                String neu = tag.replaceFirst("^[vV]", "").trim();
                String seite = (seiteUrl != null && !seiteUrl.isBlank())
                        ? seiteUrl : "https://github.com/" + REPO + "/releases/latest";
                if (istNeuer(neu, Version.aktuell())) {
                    Platform.runLater(() -> zeigeHinweis(neu, seite, zipUrl));
                } else if (mitRueckmeldung) {
                    zeigeInfo("Sie verwenden bereits die aktuelle Version " + Version.aktuell() + ".");
                }
            } catch (Exception e) {
                if (mitRueckmeldung) zeigeInfo("Die Aktualisierung konnte nicht geprüft werden.\n"
                        + "Bitte prüfen Sie Ihre Internetverbindung.");
            }
        }, "update-pruefer");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------ HTTP

    private static String hole(String url) throws Exception {
        HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
        HttpRequest r = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "KundenVerwaltung-Updater")   // GitHub verlangt einen User-Agent
                .GET().build();
        HttpResponse<String> resp = c.send(r, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 ? resp.body() : null;
    }

    /** Liest ein einfaches JSON-Textfeld ("name":"wert"). */
    private static String feld(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Sucht die Download-Adresse der angehängten .zip-Datei. */
    private static String zipDownloadUrl(String json) {
        Matcher m = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]*\\.zip)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** true, wenn neu > aktuell (Versionen wie 1.2.0). */
    static boolean istNeuer(String neu, String aktuell) {
        String[] a = neu.split("\\.");
        String[] b = aktuell.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? zahl(a[i]) : 0;
            int y = i < b.length ? zahl(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int zahl(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }

    // --------------------------------------------------------------- Dialoge

    private static void zeigeInfo(String text) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, text);
            a.setTitle("Aktualisierung");
            a.setHeaderText(null);
            a.showAndWait();
        });
    }

    private static void zeigeHinweis(String neu, String seite, String zipUrl) {
        ButtonType jetzt = new ButtonType("Jetzt aktualisieren", ButtonBar.ButtonData.OK_DONE);
        ButtonType spaeter = new ButtonType("Später", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Version " + neu + " ist verfügbar (installiert: " + Version.aktuell() + ").\n"
                + "Jetzt automatisch herunterladen und installieren?",
                jetzt, spaeter);
        a.setTitle("Aktualisierung");
        a.setHeaderText("Neue Version verfügbar");
        a.showAndWait().ifPresent(bt -> {
            if (bt == jetzt) automatischAktualisieren(zipUrl, seite);
        });
    }

    // --------------------------------------------------------- Auto-Update

    private static boolean istWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void oeffneBrowser(String seite) {
        if (java.awt.Desktop.isDesktopSupported()) {
            try { java.awt.Desktop.getDesktop().browse(URI.create(seite)); } catch (Exception ignored) {}
        }
    }

    private static void automatischAktualisieren(String zipUrl, String seite) {
        // Ohne Windows oder ohne ZIP-Adresse: einfach die Seite im Browser öffnen.
        if (zipUrl == null || zipUrl.isBlank() || !istWindows()) { oeffneBrowser(seite); return; }

        Stage p = new Stage();
        p.initModality(Modality.APPLICATION_MODAL);
        p.setTitle("Aktualisierung");
        p.setResizable(false);
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(320);
        bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        Label l = new Label("Das Update wird heruntergeladen und installiert …");
        VBox box = new VBox(14, l, bar);
        box.setPadding(new Insets(22));
        p.setScene(new Scene(box));
        p.show();

        Thread t = new Thread(() -> {
            try {
                Path tmp = Files.createTempDirectory("kv-update");
                Path zip = tmp.resolve("KundenVerwaltung-Setup.zip");

                HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest r = HttpRequest.newBuilder(URI.create(zipUrl))
                        .header("User-Agent", "KundenVerwaltung-Updater")
                        .timeout(Duration.ofMinutes(5)).GET().build();
                HttpResponse<Path> resp = c.send(r, HttpResponse.BodyHandlers.ofFile(zip));
                if (resp.statusCode() != 200) throw new RuntimeException("HTTP " + resp.statusCode());

                Path batch = schreibeUpdaterBatch(tmp, zip);

                Platform.runLater(() -> {
                    try {
                        // Loslösen: eigenes Fenster, läuft weiter, nachdem die App sich beendet.
                        // (Java setzt bei Bedarf Anführungszeichen um Pfade mit Leerzeichen.)
                        new ProcessBuilder("cmd", "/c", "start", "", batch.toString()).start();
                    } catch (Exception ignored) { }
                    p.close();
                    Platform.exit();
                    System.exit(0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    p.close();
                    Alert a = new Alert(Alert.AlertType.ERROR,
                            "Das automatische Update ist fehlgeschlagen.\n"
                            + "Möchten Sie die Download-Seite im Browser öffnen?",
                            ButtonType.YES, ButtonType.NO);
                    a.setTitle("Aktualisierung");
                    a.setHeaderText("Update fehlgeschlagen");
                    a.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) oeffneBrowser(seite); });
                });
            }
        }, "update-download");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Schreibt ein Batch-Skript, das (1) auf das Beenden der App wartet,
     * (2) das ZIP entpackt, (3) die Programmdateien ersetzt und (4) neu startet.
     * Die Nutzerdaten liegen in %APPDATA% und werden NICHT angefasst.
     */
    private static Path schreibeUpdaterBatch(Path tmp, Path zip) throws Exception {
        Path work = tmp.resolve("entpackt");
        Path batch = tmp.resolve("kv-update.bat");
        String inhalt =
                "@echo off\r\n" +
                "chcp 65001 >nul\r\n" +
                "set \"ZIP=" + zip + "\"\r\n" +
                "set \"WORK=" + work + "\"\r\n" +
                "set \"ZIEL=%LOCALAPPDATA%\\KundenVerwaltung\"\r\n" +
                "rem --- Warten, bis die laufende App geschlossen ist ---\r\n" +
                "timeout /t 2 /nobreak >nul\r\n" +
                "taskkill /IM KundenVerwaltung.exe /F >nul 2>&1\r\n" +
                "timeout /t 1 /nobreak >nul\r\n" +
                "rem --- ZIP entpacken ---\r\n" +
                "powershell -NoProfile -Command \"Expand-Archive -Path '%ZIP%' -DestinationPath '%WORK%' -Force\"\r\n" +
                "rem --- Programmdateien ersetzen ---\r\n" +
                "if exist \"%ZIEL%\" rmdir /S /Q \"%ZIEL%\"\r\n" +
                "mkdir \"%ZIEL%\" 2>nul\r\n" +
                "xcopy \"%WORK%\\KundenVerwaltung-Setup\\app\" \"%ZIEL%\" /E /I /Q /Y >nul\r\n" +
                "rem --- Neu starten ---\r\n" +
                "start \"\" \"%ZIEL%\\KundenVerwaltung.exe\"\r\n";
        Files.writeString(batch, inhalt);
        return batch;
    }
}
