package de.reinheit.kundenverwaltung.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Gemeinsame A4-Druckfunktion. Baut den Druckinhalt in exakter Seitenbreite
 * auf und verteilt ihn – wenn nötig – in Originalgröße auf mehrere Seiten,
 * anstatt alles auf eine Seite zu stauchen. Dadurch bleibt das Logo immer
 * in voller Breite.
 */
public final class Drucker {

    private Drucker() {}

    /** Füllt den Container mit dem zu druckenden Inhalt (nutzbare Breite in Punkten). */
    public interface Inhalt {
        void befuellen(VBox ziel, double inhaltBreite);
    }

    /** Ungefähre druckbare A4-Breite (für die Vorschau, ohne Druckdialog). */
    private static double a4Breite() {
        PrinterJob probe = PrinterJob.createPrinterJob();
        if (probe != null) {
            PageLayout l = probe.getPrinter().createPageLayout(
                    Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            return l.getPrintableWidth();
        }
        return 523; // Fallback A4 bei Standardrändern
    }

    /** Ungefähre druckbare A4-Höhe (für die PDF-Ausgabe, ohne Druckdialog). */
    private static double a4Hoehe() {
        PrinterJob probe = PrinterJob.createPrinterJob();
        if (probe != null) {
            PageLayout l = probe.getPrinter().createPageLayout(
                    Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            return l.getPrintableHeight();
        }
        return 770; // Fallback A4 bei Standardrändern
    }

    /**
     * Zeigt zuerst eine Bildschirm-Vorschau des Druckinhalts. Von dort aus kann
     * der Nutzer drucken (Druckdialog) oder direkt als PDF speichern
     * (Speicherdialog mit vorgeschlagenem Dateinamen).
     * @param pdfName vorgeschlagener Dateiname für „Als PDF speichern"
     */
    public static void vorschau(Window owner, String titel, String pdfName, Inhalt inhalt) {
        double pw = a4Breite();
        double rand = 24;

        VBox seiteInhalt = new VBox(2);
        seiteInhalt.setPadding(new Insets(rand, 0, rand, 0));
        seiteInhalt.setMinWidth(pw);
        seiteInhalt.setPrefWidth(pw);
        seiteInhalt.setMaxWidth(pw);
        seiteInhalt.setStyle("-fx-background-color: white;");
        inhalt.befuellen(seiteInhalt, pw);

        // Weiße „Seite" auf grauem Hintergrund
        HBox blatt = new HBox(seiteInhalt);
        blatt.setAlignment(Pos.TOP_CENTER);
        blatt.setPadding(new Insets(18));
        blatt.setStyle("-fx-background-color: #e9eef2;");

        ScrollPane sp = new ScrollPane(blatt);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #e9eef2;");

        Button drucken = new Button("🖨  Drucken");
        drucken.getStyleClass().add("primary");
        Button pdf = new Button("📄  Als PDF speichern");
        Button schliessen = new Button("Schließen");
        HBox bar = new HBox(10, drucken, pdf, schliessen);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10, 14, 10, 14));

        BorderPane wurzel = new BorderPane();
        wurzel.setCenter(sp);
        wurzel.setBottom(bar);

        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.setTitle(titel == null ? "Druckvorschau" : "Druckvorschau – " + titel);
        Scene scene = new Scene(wurzel, pw + 90, 820);
        var css = Drucker.class.getResource("/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);

        schliessen.setOnAction(e -> stage.close());
        drucken.setOnAction(e -> { stage.close(); drucke(owner, inhalt); });
        pdf.setOnAction(e -> alsPdf(stage, pdfName, inhalt));

        stage.show();
    }

    public static void drucke(Window owner, Inhalt inhalt) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            Meldung.fehler("Es ist kein Drucker verfügbar.");
            return;
        }
        PageLayout layout = job.getPrinter().createPageLayout(
                Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);

        if (!job.showPrintDialog(owner)) return;   // hier "Microsoft Print to PDF" wählbar

        layout = job.getJobSettings().getPageLayout();
        double pw = layout.getPrintableWidth();
        double ph = layout.getPrintableHeight();

        // Inhalt seitenweise aufbauen (kein Block wird zerschnitten) und drucken.
        double rand = 24;
        List<VBox> seiten = baueSeiten(inhalt, pw, ph, rand);

        boolean ok = true;
        for (VBox s : seiten) {
            new Scene(new Group(s));
            s.applyCss();
            s.layout();
            ok = job.printPage(layout, s);
            if (!ok) break;
        }

        if (ok) job.endJob();
        else Meldung.fehler("Der Druckauftrag konnte nicht abgeschlossen werden.");
    }

    /**
     * Speichert den Inhalt als PDF-Datei (Dateiname wird vorgeschlagen).
     * Jede Seite wird als Bild eingebettet – so bleibt das Layout exakt wie im Druck.
     */
    public static void alsPdf(Window owner, String vorschlagName, Inhalt inhalt) {
        double pw = a4Breite();
        double ph = a4Hoehe();
        double rand = 24;

        FileChooser fc = new FileChooser();
        fc.setTitle("Als PDF speichern");
        fc.setInitialFileName(vorschlagName == null ? "Dokument.pdf" : vorschlagName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Datei (*.pdf)", "*.pdf"));
        File ziel = fc.showSaveDialog(owner);
        if (ziel == null) return;

        List<VBox> seiten = baueSeiten(inhalt, pw, ph, rand);
        double skala = 2.0;   // ~2-fache Auflösung für saubere Schrift

        try (PDDocument doc = new PDDocument()) {
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(Transform.scale(skala, skala));
            params.setFill(Color.WHITE);

            for (VBox s : seiten) {
                s.setMinHeight(ph);
                s.setPrefHeight(ph);
                s.setStyle("-fx-background-color: white;");
                new Scene(new Group(s));
                s.applyCss();
                s.layout();

                WritableImage fx = s.snapshot(params, null);
                BufferedImage bild = SwingFXUtils.fromFXImage(fx, null);

                PDPage seite = new PDPage(new PDRectangle((float) pw, (float) ph));
                doc.addPage(seite);
                PDImageXObject bildObjekt = LosslessFactory.createFromImage(doc, bild);
                try (PDPageContentStream cs = new PDPageContentStream(doc, seite)) {
                    cs.drawImage(bildObjekt, 0, 0, (float) pw, (float) ph);
                }
            }
            doc.save(ziel);
        } catch (Exception e) {
            System.err.println("PDF-Erstellung fehlgeschlagen: " + e);
            Meldung.fehler("Die PDF-Datei konnte nicht erstellt werden.");
            return;
        }

        if (java.awt.Desktop.isDesktopSupported()) {
            try { java.awt.Desktop.getDesktop().open(ziel); } catch (Exception ignored) {}
        }
    }

    /**
     * Baut den Inhalt in Seitenbreite auf und verteilt die Blöcke seitenweise,
     * ohne einen Block (z. B. eine Tabellenzeile) zu zerschneiden.
     */
    private static List<VBox> baueSeiten(Inhalt inhalt, double pw, double ph, double rand) {
        VBox master = new VBox(2);
        master.setPadding(new Insets(rand, 0, rand, 0));
        master.setMinWidth(pw);
        master.setPrefWidth(pw);
        master.setMaxWidth(pw);
        inhalt.befuellen(master, pw);

        new Scene(new Group(master));
        master.applyCss();
        master.layout();

        double spacing = master.getSpacing();
        List<Node> bloecke = new ArrayList<>(master.getChildren());
        List<Double> hoehen = new ArrayList<>();
        for (Node b : bloecke) hoehen.add(b.getLayoutBounds().getHeight());
        master.getChildren().clear();

        double nutzbar = ph - 2 * rand;
        List<VBox> seiten = new ArrayList<>();
        VBox aktuell = neueSeite(pw, rand, spacing);
        double h = 0;
        for (int i = 0; i < bloecke.size(); i++) {
            double bh = hoehen.get(i);
            if (h > 0 && h + spacing + bh > nutzbar) {
                seiten.add(aktuell);
                aktuell = neueSeite(pw, rand, spacing);
                h = 0;
            }
            aktuell.getChildren().add(bloecke.get(i));
            h += (h > 0 ? spacing : 0) + bh;
        }
        if (!aktuell.getChildren().isEmpty()) seiten.add(aktuell);
        return seiten;
    }

    /** Leere Druckseite in Seitenbreite (oben/unten Rand, seitlich 0). */
    private static VBox neueSeite(double pw, double rand, double spacing) {
        VBox s = new VBox(spacing);
        s.setPadding(new Insets(rand, 0, rand, 0));
        s.setMinWidth(pw);
        s.setPrefWidth(pw);
        s.setMaxWidth(pw);
        return s;
    }
}
