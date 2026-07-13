package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.service.Datum;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Baut einen einheitlichen Listen-Bericht (Firmenlogo, Titel, Tabelle, Fußzeile)
 * für den Druck. Wird von den Bereichen Einsätze, Termine und Zahlungen genutzt.
 */
public final class Bericht {

    private Bericht() {}

    /**
     * Füllt 'ziel' mit einem Bericht.
     * @param breite nutzbare Breite in Punkten
     * @param titel  Berichtstitel (Datum wird angehängt)
     * @param header Spaltenüberschriften
     * @param rows   Zeilen (jede so lang wie 'header')
     */
    public static void liste(VBox ziel, double breite, String titel, String[] header, List<String[]> rows) {
        // Seitlicher Innenabstand für Textinhalte; das Logo bleibt volle Breite.
        double seit = 24;
        double innen = breite - 2 * seit;

        // ----- Firmenlogo (volle Breite, bis an den Seitenrand) -----
        var logoUrl = Bericht.class.getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
            logo.setFitWidth(breite);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            VBox kopf = new VBox(logo);
            kopf.setPadding(new Insets(0, 0, 10, 0));
            ziel.getChildren().add(kopf);
        }

        Label t = new Label(titel + "  ·  " + Datum.anzeige(java.time.LocalDate.now()));
        t.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #006E9C; "
                + "-fx-border-color: transparent transparent #01AAF0 transparent; -fx-border-width: 0 0 2 0; "
                + "-fx-padding: 0 0 8 0;");
        VBox tBox = new VBox(t);
        tBox.setPadding(new Insets(0, seit, 12, seit));
        ziel.getChildren().add(tBox);

        // ----- Tabelle: jede Zeile ein eigener Block (saubere Seitenumbrüche) -----
        double proz = 100.0 / header.length;
        ziel.getChildren().add(zeile(innen, seit, header, proz, true));
        for (String[] row : rows) ziel.getChildren().add(zeile(innen, seit, row, proz, false));

        Label fuss = new Label("— Erstellt mit KundenVerwaltung · " + rows.size() + " Einträge —");
        fuss.setStyle("-fx-text-fill: #94a3b8;");
        VBox fussBox = new VBox(fuss);
        fussBox.setPadding(new Insets(16, seit, 12, seit));
        ziel.getChildren().add(fussBox);
        // Die Firmen-Fußzeile (Bild) setzt der Drucker automatisch an jeden Seitenfuß.
    }

    /** Eine Tabellenzeile mit gleich breiten Spalten als eigener, eingerückter Block. */
    private static VBox zeile(double innen, double seit, String[] werte, double proz, boolean kopf) {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setPrefWidth(innen); g.setMaxWidth(innen);
        for (int c = 0; c < werte.length; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(proz);
            g.getColumnConstraints().add(cc);
        }
        for (int c = 0; c < werte.length; c++) {
            Label l = new Label(werte[c] == null ? "" : werte[c]);
            l.setWrapText(true);
            if (kopf) l.setStyle("-fx-font-weight: bold; -fx-text-fill: #006E9C;");
            g.add(l, c, 0);
        }
        VBox blk = new VBox(g);
        blk.setPadding(new Insets(1, seit, 1, seit));
        return blk;
    }
}
