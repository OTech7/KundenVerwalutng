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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    static {
        // Damit die App denselben Proxy/VPN nutzt wie der Browser (Windows-Einstellungen).
        System.setProperty("java.net.useSystemProxies", "true");
    }

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
                String jarUrl = assetUrl(json, "app\\.jar");   // kleines Update (~wenige MB)
                String zipUrl = assetUrl(json, "\\.zip");      // voller Installer (Rückfall)
                if (tag == null) {
                    if (mitRueckmeldung) zeigeInfo("Es wurde noch keine Version veröffentlicht.");
                    return;
                }
                String neu = tag.replaceFirst("^[vV]", "").trim();
                String seite = (seiteUrl != null && !seiteUrl.isBlank())
                        ? seiteUrl : "https://github.com/" + REPO + "/releases/latest";
                if (istNeuer(neu, Version.aktuell())) {
                    Platform.runLater(() -> zeigeHinweis(neu, seite, jarUrl, zipUrl));
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
        HttpClient c = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)          // vermeidet "Connection reset" (HTTP/2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())             // System-/VPN-Proxy nutzen (wie der Browser)
                .connectTimeout(Duration.ofSeconds(6)).build();
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

    /** Sucht die Download-Adresse eines Anhangs, dessen Name auf 'endungRegex' endet. */
    private static String assetUrl(String json, String endungRegex) {
        Matcher m = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]*" + endungRegex + ")\"").matcher(json);
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

    private static void zeigeHinweis(String neu, String seite, String jarUrl, String zipUrl) {
        ButtonType jetzt = new ButtonType("Jetzt aktualisieren", ButtonBar.ButtonData.OK_DONE);
        ButtonType spaeter = new ButtonType("Später", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Version " + neu + " ist verfügbar (installiert: " + Version.aktuell() + ").\n"
                + "Jetzt automatisch herunterladen und installieren?",
                jetzt, spaeter);
        a.setTitle("Aktualisierung");
        a.setHeaderText("Neue Version verfügbar");
        a.showAndWait().ifPresent(bt -> {
            if (bt == jetzt) automatischAktualisieren(jarUrl, zipUrl, seite);
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

    private static void automatischAktualisieren(String jarUrl, String zipUrl, String seite) {
        if (!istWindows()) { oeffneBrowser(seite); return; }
        // Bevorzugt das kleine JAR-Update (~wenige MB); sonst der volle Installer.
        boolean nurJar = jarUrl != null && !jarUrl.isBlank();
        String url = nurJar ? jarUrl : zipUrl;
        if (url == null || url.isBlank()) { oeffneBrowser(seite); return; }

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
                Path datei = tmp.resolve(nurJar ? "KundenVerwaltung-app.jar" : "KundenVerwaltung-Setup.zip");

                ladeMitFortsetzung(url, datei);

                Path batch = nurJar ? schreibeJarUpdater(tmp, datei) : schreibeUpdaterBatch(tmp, datei);

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
                System.err.println("Auto-Update fehlgeschlagen: " + e);
                String grund = e.getMessage() == null ? e.toString() : e.getMessage();
                Platform.runLater(() -> {
                    p.close();
                    Alert a = new Alert(Alert.AlertType.ERROR,
                            "Das automatische Update ist fehlgeschlagen.\n(" + grund + ")\n\n"
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
     * Lädt eine (große) Datei herunter und setzt bei Verbindungsabbrüchen mit
     * HTTP-Range fort, statt komplett neu zu beginnen. Bis zu 8 Versuche.
     */
    private static void ladeMitFortsetzung(String url, Path ziel) throws Exception {
        HttpClient c = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())             // System-/VPN-Proxy nutzen (wie der Browser)
                .connectTimeout(Duration.ofSeconds(15)).build();

        long erwartet = -1;
        Exception letzter = null;

        for (int versuch = 1; versuch <= 8; versuch++) {
            long vorhanden = Files.exists(ziel) ? Files.size(ziel) : 0;
            if (erwartet > 0 && vorhanden >= erwartet) return;   // schon vollständig

            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "KundenVerwaltung-Updater")
                    .header("Accept", "application/octet-stream")
                    .timeout(Duration.ofMinutes(10)).GET();
            if (vorhanden > 0) rb.header("Range", "bytes=" + vorhanden + "-");

            try {
                HttpResponse<InputStream> resp = c.send(rb.build(), HttpResponse.BodyHandlers.ofInputStream());
                int sc = resp.statusCode();

                if (sc == 200 && vorhanden > 0) {          // Server ignoriert Range -> neu beginnen
                    Files.deleteIfExists(ziel);
                    vorhanden = 0;
                } else if (sc != 200 && sc != 206) {
                    throw new RuntimeException("HTTP " + sc);
                }

                if (sc == 200) {
                    erwartet = resp.headers().firstValueAsLong("Content-Length").orElse(erwartet);
                } else { // 206: Gesamtgröße aus "Content-Range: bytes a-b/total"
                    erwartet = resp.headers().firstValue("Content-Range")
                            .map(v -> { try { return Long.parseLong(v.substring(v.indexOf('/') + 1).trim()); }
                                        catch (Exception e) { return -1L; } })
                            .filter(x -> x > 0).orElse(erwartet);
                }

                try (InputStream in = resp.body();
                     OutputStream out = Files.newOutputStream(ziel,
                             StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    in.transferTo(out);
                }

                long jetzt = Files.size(ziel);
                if (erwartet <= 0 || jetzt >= erwartet) return;   // fertig
                letzter = new IOException("Abbruch bei " + jetzt + "/" + erwartet + " Bytes");
            } catch (IOException ioe) {
                letzter = ioe;   // Verbindung abgebrochen -> nächster Versuch setzt fort
            }
        }
        throw (letzter != null) ? letzter : new IOException("Download fehlgeschlagen");
    }

    /**
     * Kleines Update: ersetzt nur die App-JAR im installierten Programm und
     * startet neu. Legt vorher eine Sicherung an und stellt sie bei einem Fehler
     * wieder her (Rollback). Nutzerdaten in %APPDATA% bleiben unberührt.
     */
    private static Path schreibeJarUpdater(Path tmp, Path neueJar) throws Exception {
        Path batch = tmp.resolve("kv-update.bat");
        String inhalt =
                "@echo off\r\n" +
                "chcp 65001 >nul\r\n" +
                "set \"NEU=" + neueJar + "\"\r\n" +
                "set \"APP=%LOCALAPPDATA%\\KundenVerwaltung\"\r\n" +
                "set \"ZIEL=%APP%\\app\\KundenVerwaltung-app.jar\"\r\n" +
                "rem --- Warten, bis die laufende App geschlossen ist ---\r\n" +
                "timeout /t 2 /nobreak >nul\r\n" +
                "taskkill /IM KundenVerwaltung.exe /F >nul 2>&1\r\n" +
                "timeout /t 1 /nobreak >nul\r\n" +
                "rem --- Sicherung + Austausch (mit Rollback) ---\r\n" +
                "copy /Y \"%ZIEL%\" \"%ZIEL%.bak\" >nul 2>&1\r\n" +
                "copy /Y \"%NEU%\" \"%ZIEL%\" >nul\r\n" +
                "if errorlevel 1 (\r\n" +
                "  copy /Y \"%ZIEL%.bak\" \"%ZIEL%\" >nul 2>&1\r\n" +
                ")\r\n" +
                "del \"%ZIEL%.bak\" >nul 2>&1\r\n" +
                "rem --- Neu starten ---\r\n" +
                "start \"\" \"%APP%\\KundenVerwaltung.exe\"\r\n";
        Files.writeString(batch, inhalt);
        return batch;
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
