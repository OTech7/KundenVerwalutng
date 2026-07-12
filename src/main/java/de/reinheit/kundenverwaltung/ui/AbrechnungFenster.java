package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.KundeDao;
import de.reinheit.kundenverwaltung.dao.RechnungDao;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Rechnung;
import de.reinheit.kundenverwaltung.service.AbrechnungService;
import de.reinheit.kundenverwaltung.service.Datum;
import de.reinheit.kundenverwaltung.service.WordExportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Abrechnung (nur Admin): zeigt je Kunde den aktuellen Abrechnungszeitraum,
 * wie viele Termine geplant/erbracht/offen sind, sowie die Gesamtnutzung
 * (genutzte vs. verbleibende Termine). Über „Rechnung erstellen" wird die
 * Periode abgerechnet und die nächste Periode beginnt automatisch.
 */
public class AbrechnungFenster {

    private final KundeDao kundeDao = new KundeDao();
    private final RechnungDao rechnungDao = new RechnungDao();
    private final de.reinheit.kundenverwaltung.dao.ZahlungDao zahlungDao = new de.reinheit.kundenverwaltung.dao.ZahlungDao();
    private final AbrechnungService service = new AbrechnungService();

    private final BorderPane root = new BorderPane();
    private final ComboBox<Kunde> kundeBox = new ComboBox<>();
    private final ComboBox<String> quartalBox = new ComboBox<>();
    private final ComboBox<Integer> jahrBox = new ComboBox<>();
    private final TableView<Rechnung> tabelle = new TableView<>();
    private final ObservableList<Rechnung> daten = FXCollections.observableArrayList();

    private final Label lblAktuell   = new Label();
    private final Label lblGeplant   = new Label();
    private final Label lblErbracht  = new Label();
    private final Label lblOffen     = new Label();
    private final Label lblBetrag    = new Label();
    private final Label lblGesamt    = new Label();
    private final Label lblZahlungen = new Label();
    private double letzterPreis = 0;   // zuletzt verwendeter Preis je Termin (Vorgabe)
    private final Button btnAbrechnen = new Button("🧾 Rechnung erstellen");
    private final Button btnWord = new Button("📄 Rechnung als Word");

    public AbrechnungFenster() {
        List<Kunde> kunden = kundeDao.findeAlle();
        kundeBox.setItems(FXCollections.observableArrayList(kunden));
        kundeBox.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Kunde k) { return k == null ? "" : "#" + k.getKundennummer() + " – " + k.getVollstaendigerName(); }
            public Kunde fromString(String s) { return null; }
        });
        kundeBox.setOnAction(e -> aktualisiereAnsicht());
        kundeBox.setPrefWidth(260);

        // Quartal + Jahr frei wählbar
        quartalBox.setItems(FXCollections.observableArrayList("Q1", "Q2", "Q3", "Q4"));
        int jahrJetzt = java.time.LocalDate.now().getYear();
        jahrBox.setItems(FXCollections.observableArrayList(
                java.util.stream.IntStream.rangeClosed(jahrJetzt - 5, jahrJetzt + 1).boxed().toList()));
        int aktQ = (java.time.LocalDate.now().getMonthValue() - 1) / 3 + 1;
        quartalBox.setValue("Q" + aktQ);
        jahrBox.setValue(jahrJetzt);
        quartalBox.setOnAction(e -> aktualisiereAnsicht());
        jahrBox.setOnAction(e -> aktualisiereAnsicht());

        btnAbrechnen.getStyleClass().add("primary");
        btnAbrechnen.setOnAction(e -> abrechnen());
        btnWord.setDisable(true);
        btnWord.setOnAction(e -> rechnungAlsWord());

        HBox bar = new HBox(10, new Label("Kunde:"), kundeBox,
                new Label("Quartal:"), quartalBox, jahrBox, btnAbrechnen);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 14, 0));

        // ----- Übersichtskarte -----
        GridPane info = new GridPane();
        info.setHgap(18); info.setVgap(8);
        info.setPadding(new Insets(16));
        info.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #d4e9f5; -fx-border-radius: 12;");
        int r = 0;
        info.add(kopf("Gewähltes Quartal"), 0, r++, 2, 1);
        info.addRow(r++, feld("Zeitraum:"), lblAktuell);
        info.addRow(r++, feld("Geplante Termine:"), lblGeplant);
        info.addRow(r++, feld("Erbrachte Termine:"), lblErbracht);
        info.addRow(r++, feld("Nicht wahrgenommen:"), lblOffen);
        info.addRow(r++, feld("Betrag:"), lblBetrag);
        info.add(kopf("Gesamt (Vertragslaufzeit)"), 0, r++, 2, 1);
        info.addRow(r++, feld("Genutzt / Verbleibend:"), lblGesamt);
        info.add(kopf("Zahlungen (Quartale im Jahr)"), 0, r++, 2, 1);
        lblZahlungen.setWrapText(true);
        info.add(lblZahlungen, 0, r++, 2, 1);

        // ----- Rechnungshistorie -----
        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelle.getColumns().setAll(
                col("Nr.", "id", 50),
                col("Zeitraum", "zeitraum", 190),
                col("Geplant", "anzahlGeplant", 70),
                col("Erbracht", "anzahlErbracht", 70),
                col("Betrag (€)", "betrag", 90),
                col("Status", "status", 100),
                col("Erstellt am", "erstelltAmAnzeige", 100)
        );
        tabelle.setItems(daten);
        tabelle.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> btnWord.setDisable(n == null));

        VBox rechts = new VBox(10, new Label("Bisherige Rechnungen:"), tabelle, btnWord);
        VBox.setVgrow(tabelle, Priority.ALWAYS);

        HBox mitte = new HBox(18, info, rechts);
        HBox.setHgrow(rechts, Priority.ALWAYS);

        root.setTop(bar);
        root.setCenter(mitte);

        if (!kunden.isEmpty()) { kundeBox.getSelectionModel().selectFirst(); aktualisiereAnsicht(); }
    }

    private Label kopf(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #006E9C; -fx-padding: 10 0 2 0;");
        return l;
    }
    private Label feld(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #5b7a8a;");
        return l;
    }

    private <T> TableColumn<Rechnung, T> col(String t, String prop, double w) {
        TableColumn<Rechnung, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    /** Start- und Enddatum des gewählten Quartals (Q1=Jan–Mär, … Q4=Okt–Dez). */
    private java.time.LocalDate[] quartalZeitraum() {
        int q = quartalBox.getValue() == null ? 1 : Integer.parseInt(quartalBox.getValue().substring(1));
        int jahr = jahrBox.getValue() == null ? java.time.LocalDate.now().getYear() : jahrBox.getValue();
        java.time.LocalDate von = java.time.LocalDate.of(jahr, (q - 1) * 3 + 1, 1);
        java.time.LocalDate bis = von.plusMonths(3).minusDays(1);
        return new java.time.LocalDate[]{von, bis};
    }

    /** Berechnet die Zählungen für Kunde + gewähltes Quartal neu. */
    private void aktualisiereAnsicht() {
        Kunde k = kundeBox.getValue();
        if (k == null) return;

        java.time.LocalDate[] zt = quartalZeitraum();
        AbrechnungService.Zeitraum z = service.fuer(k, zt[0], zt[1]);

        lblAktuell.setText(Datum.anzeige(z.von) + "  bis  " + Datum.anzeige(z.bis));
        lblGeplant.setText(String.valueOf(z.geplant));
        lblErbracht.setText(String.valueOf(z.erbracht));
        lblOffen.setText(String.valueOf(z.offen()));
        lblBetrag.setText("wird bei der Rechnungserstellung festgelegt");

        int genutzt = service.erbrachtGesamt(k);
        int geplantG = service.geplantGesamt(k);
        lblGesamt.setText(genutzt + " genutzt  /  " + Math.max(0, geplantG - genutzt) + " verbleibend   (von " + geplantG + " geplanten)");

        aktualisiereZahlungen(k);

        daten.setAll(rechnungDao.finde(k.getKundennummer()));
    }

    /**
     * Zeigt je Quartal des gewählten Jahres, ob eine Zahlung vorliegt, und
     * weist auf noch offene Quartale hin. Nur für Administratoren (Zahlungen).
     */
    private void aktualisiereZahlungen(Kunde k) {
        if (!de.reinheit.kundenverwaltung.service.Session.istAdmin()) {
            lblZahlungen.setText("Nur für Administratoren sichtbar.");
            return;
        }
        int jahr = jahrBox.getValue() == null ? java.time.LocalDate.now().getYear() : jahrBox.getValue();
        java.util.Set<String> bezahlt = new java.util.HashSet<>();
        for (de.reinheit.kundenverwaltung.model.Zahlung z : zahlungDao.findeFuerKunde(k.getKundennummer())) {
            if (z.getZahlungsdatum() != null) bezahlt.add(z.getZahlungsdatum().trim());
        }
        StringBuilder sb = new StringBuilder(jahr + ":   ");
        java.util.List<String> offen = new java.util.ArrayList<>();
        for (int q = 1; q <= 4; q++) {
            boolean da = bezahlt.contains("Q" + q + " " + jahr);
            sb.append("Q").append(q).append(da ? " ✓" : " ✗");
            if (q < 4) sb.append("   ·   ");
            if (!da) offen.add("Q" + q);
        }
        if (offen.isEmpty()) sb.append("\nAlle Quartale erfasst.");
        else sb.append("\nNoch offen: ").append(String.join(", ", offen)).append(".");
        lblZahlungen.setText(sb.toString());
    }

    /** Rechnung für das gewählte Quartal erstellen. */
    private void abrechnen() {
        Kunde k = kundeBox.getValue();
        if (k == null) return;
        java.time.LocalDate[] zt = quartalZeitraum();
        AbrechnungService.Zeitraum z = service.fuer(k, zt[0], zt[1]);

        // Hinweis, wenn dieses Quartal bereits abgerechnet wurde
        boolean schonAbgerechnet = daten.stream()
                .anyMatch(x -> zt[0].toString().equals(x.getZeitraumVon()) && zt[1].toString().equals(x.getZeitraumBis()));
        if (schonAbgerechnet) {
            Alert w = new Alert(Alert.AlertType.CONFIRMATION,
                    "Für " + quartalBox.getValue() + " " + jahrBox.getValue()
                    + " existiert bereits eine Rechnung. Erneut erstellen?", ButtonType.YES, ButtonType.NO);
            w.setHeaderText("Bereits abgerechnet");
            if (w.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        if (z.erbracht == 0) {
            Alert w = new Alert(Alert.AlertType.CONFIRMATION,
                    "Im Zeitraum " + Datum.anzeige(z.von) + " bis " + Datum.anzeige(z.bis) + " wurden keine Termine erbracht.\n"
                    + "Trotzdem abrechnen?", ButtonType.YES, ButtonType.NO);
            w.setHeaderText("Keine erbrachten Termine");
            if (w.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        }

        // Preis je Termin abfragen (Vorgabe = zuletzt verwendeter Wert)
        Dialog<Double> pd = new Dialog<>();
        pd.setTitle("Rechnung erstellen");
        pd.setHeaderText("Zeitraum: " + Datum.anzeige(z.von) + " bis " + Datum.anzeige(z.bis) + "\n"
                + "Erbrachte Termine: " + z.erbracht + " (von " + z.geplant + " geplanten)");
        pd.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField preis = new TextField(String.valueOf(letzterPreis));
        Label vorschau = new Label();
        Runnable rechne = () -> {
            Double p = de.reinheit.kundenverwaltung.ui.Eingabe.zahl(preis.getText());
            vorschau.setText(p == null ? "—" : String.format("Betrag: %.2f €", z.erbracht * p));
        };
        preis.textProperty().addListener((o, a, b) -> rechne.run());
        rechne.run();

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(14));
        g.addRow(0, new Label("Preis pro Termin (€)"), preis);
        g.addRow(1, new Label("Ergibt"), vorschau);
        g.add(new Label("0 = ohne Betrag (nur Anzahl abrechnen)"), 0, 2, 2, 1);
        pd.getDialogPane().setContent(g);

        Button ok = (Button) pd.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String f = de.reinheit.kundenverwaltung.ui.Eingabe.pruefeZahl(preis.getText(), "Preis pro Termin", 0, 100_000);
            if (f != null) { Meldung.warnung(f); ev.consume(); }
        });
        pd.setResultConverter(bt -> bt == ButtonType.OK
                ? de.reinheit.kundenverwaltung.ui.Eingabe.zahl(preis.getText()) : null);

        Double preisWert = pd.showAndWait().orElse(null);
        if (preisWert == null) return;
        letzterPreis = preisWert;

        Rechnung r = service.abrechnen(k, zt[0], zt[1], preisWert);
        aktualisiereAnsicht();
        tabelle.getSelectionModel().select(0);

        Alert fertig = new Alert(Alert.AlertType.INFORMATION,
                "Rechnung Nr. " + r.getId() + " für " + quartalBox.getValue() + " " + jahrBox.getValue()
                + " wurde erstellt.");
        fertig.setHeaderText("Abgerechnet");
        fertig.showAndWait();
    }

    /** Ausgewählte Rechnung als Word-Dokument speichern. */
    private void rechnungAlsWord() {
        Rechnung r = tabelle.getSelectionModel().getSelectedItem();
        Kunde k = kundeBox.getValue();
        if (r == null || k == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Rechnung speichern");
        fc.setInitialFileName("Rechnung_" + r.getId() + "_Kunde_" + k.getKundennummer() + ".docx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word-Dokument (*.docx)", "*.docx"));
        File ziel = fc.showSaveDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (ziel == null) return;

        try {
            new WordExportService().erstelleRechnung(k, r, ziel);
            if (java.awt.Desktop.isDesktopSupported()) {
                try { java.awt.Desktop.getDesktop().open(ziel); } catch (Exception ignored) {}
            }
            new Alert(Alert.AlertType.INFORMATION, "Rechnung gespeichert:\n" + ziel.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            System.err.println("Rechnung-Word fehlgeschlagen: " + ex);
            new Alert(Alert.AlertType.ERROR, "Die Rechnung konnte nicht erstellt werden.").showAndWait();
        }
    }

    public BorderPane getRoot() { return root; }
}
