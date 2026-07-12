package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.service.Version;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prüft beim Programmstart im Hintergrund, ob auf GitHub eine neuere Version
 * veröffentlicht wurde, und zeigt bei Bedarf einen Hinweis mit Download-Knopf.
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
        Thread t = new Thread(() -> {
            try {
                String json = hole("https://api.github.com/repos/" + REPO + "/releases/latest");
                if (json == null) return;
                String tag = feld(json, "tag_name");        // z. B. "v1.1.0"
                String url = feld(json, "html_url");         // Seite der Veröffentlichung
                if (tag == null) return;
                String neu = tag.replaceFirst("^[vV]", "").trim();
                if (istNeuer(neu, Version.aktuell())) {
                    String seite = (url != null && !url.isBlank())
                            ? url : "https://github.com/" + REPO + "/releases/latest";
                    Platform.runLater(() -> zeigeHinweis(neu, seite));
                }
            } catch (Exception ignored) { /* offline o. Ä. – still ignorieren */ }
        }, "update-pruefer");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Manuelle Prüfung (über einen Menüpunkt). Zeigt immer eine Rückmeldung:
     * neue Version, bereits aktuell oder Fehler (z. B. keine Verbindung).
     */
    public static void pruefeManuell() {
        Thread t = new Thread(() -> {
            try {
                String json = hole("https://api.github.com/repos/" + REPO + "/releases/latest");
                if (json == null) {
                    zeigeInfo("Es wurde keine Veröffentlichung gefunden.\n"
                            + "Prüfen Sie den Repository-Namen und die Internetverbindung.");
                    return;
                }
                String tag = feld(json, "tag_name");
                String url = feld(json, "html_url");
                if (tag == null) {
                    zeigeInfo("Es wurde noch keine Version veröffentlicht.");
                    return;
                }
                String neu = tag.replaceFirst("^[vV]", "").trim();
                String seite = (url != null && !url.isBlank())
                        ? url : "https://github.com/" + REPO + "/releases/latest";
                if (istNeuer(neu, Version.aktuell())) {
                    Platform.runLater(() -> zeigeHinweis(neu, seite));
                } else {
                    zeigeInfo("Sie verwenden bereits die aktuelle Version " + Version.aktuell() + ".");
                }
            } catch (Exception e) {
                zeigeInfo("Die Aktualisierung konnte nicht geprüft werden.\n"
                        + "Bitte prüfen Sie Ihre Internetverbindung.");
            }
        }, "update-pruefer-manuell");
        t.setDaemon(true);
        t.start();
    }

    private static void zeigeInfo(String text) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, text);
            a.setTitle("Aktualisierung");
            a.setHeaderText(null);
            a.showAndWait();
        });
    }

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

    private static void zeigeHinweis(String neu, String seite) {
        ButtonType herunterladen = new ButtonType("Herunterladen");
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Version " + neu + " ist verfügbar (installiert: " + Version.aktuell() + ").\n"
                + "Möchten Sie die neue Version herunterladen?",
                herunterladen, new ButtonType("Später", ButtonType.CANCEL.getButtonData()));
        a.setTitle("Aktualisierung");
        a.setHeaderText("Neue Version verfügbar");
        a.showAndWait().ifPresent(bt -> {
            if (bt == herunterladen && java.awt.Desktop.isDesktopSupported()) {
                try { java.awt.Desktop.getDesktop().browse(URI.create(seite)); } catch (Exception ignored) {}
            }
        });
    }
}
