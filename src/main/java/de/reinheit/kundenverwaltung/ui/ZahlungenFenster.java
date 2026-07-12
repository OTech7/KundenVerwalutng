package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.KundeDao;
import de.reinheit.kundenverwaltung.dao.ZahlungDao;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Zahlung;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

/** ZahlungenFenster: Liste aller Zahlungen + Dialog zum Hinzufügen. */
public class ZahlungenFenster {

    private final ZahlungDao dao = new ZahlungDao();
    private final KundeDao kundeDao = new KundeDao();
    private final BorderPane root = new BorderPane();
    private final TableView<Zahlung> tabelle = new TableView<>();
    private final ObservableList<Zahlung> daten = FXCollections.observableArrayList();
    private final Label summe = new Label();

    public ZahlungenFenster() {
        Button add = new Button("＋ Zahlung hinzufügen");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> dialog(null));

        Button bearbeiten = new Button("✏ Bearbeiten");
        bearbeiten.getStyleClass().add("primary");
        bearbeiten.setDisable(true);
        bearbeiten.setOnAction(e -> {
            Zahlung sel = tabelle.getSelectionModel().getSelectedItem();
            if (sel != null) dialog(sel);
        });

        Button loeschen = new Button("🗑 Löschen");
        loeschen.setDisable(true);
        loeschen.setOnAction(e -> zahlungLoeschen());

        Button drucken = new Button("🖨 Drucken");
        drucken.setOnAction(e -> drucken());

        summe.getStyleClass().add("platzhalter");

        HBox bar = new HBox(12, add, bearbeiten, loeschen, drucken, summe);
        bar.setPadding(new Insets(0, 0, 14, 0));

        // Doppelklick öffnet die Zeile zum Bearbeiten
        tabelle.setRowFactory(tv -> {
            TableRow<Zahlung> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) dialog(row.getItem());
            });
            return row;
        });
        tabelle.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            bearbeiten.setDisable(n == null);
            loeschen.setDisable(n == null);
        });

        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelle.getColumns().setAll(
                col("Kunde", "kundenName", 180),
                col("Quartal", "zahlungsdatum", 100),
                col("Betrag (€)", "betrag", 90),
                col("Zahlungsart", "zahlungsart", 120),
                col("Status", "zahlungsstatus", 130),
                col("Notizen", "notizen", 170)
        );
        tabelle.setItems(daten);

        root.setTop(bar);
        root.setCenter(tabelle);
        laden();
    }

    private <T> TableColumn<Zahlung, T> col(String t, String prop, double w) {
        TableColumn<Zahlung, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private void laden() {
        java.util.Map<Integer, String> namen = new java.util.HashMap<>();
        for (Kunde k : kundeDao.findeAlle()) namen.put(k.getKundennummer(), k.getVollstaendigerName());

        List<Zahlung> z = dao.findeAlle();
        for (Zahlung x : z) x.setKundenName(namen.getOrDefault(x.getKundennummer(), "#" + x.getKundennummer()));
        daten.setAll(z);
        double bezahlt = z.stream().filter(x -> "Bezahlt".equals(x.getZahlungsstatus())).mapToDouble(Zahlung::getBetrag).sum();
        double offen = z.stream().filter(x -> !"Bezahlt".equals(x.getZahlungsstatus())).mapToDouble(Zahlung::getBetrag).sum();
        summe.setText(String.format("Bezahlt: %.2f €   ·   Offen/In Bearbeitung: %.2f €", bezahlt, offen));
    }

    /** Druckt die aktuell angezeigte Zahlungsliste. */
    private void drucken() {
        java.util.Map<Integer, String> namen = new java.util.HashMap<>();
        for (Kunde k : kundeDao.findeAlle()) namen.put(k.getKundennummer(), k.getVollstaendigerName());

        String[] header = {"Kunde", "Quartal", "Betrag", "Art", "Status"};
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (Zahlung z : daten) {
            rows.add(new String[]{
                    namen.getOrDefault(z.getKundennummer(), "#" + z.getKundennummer()),
                    z.getZahlungsdatum() == null ? "" : z.getZahlungsdatum(),
                    String.format("%.2f €", z.getBetrag()),
                    z.getZahlungsart(),
                    z.getZahlungsstatus()});
        }
        javafx.stage.Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        Drucker.drucke(owner, (ziel, breite) -> Bericht.liste(ziel, breite, "Zahlungsliste", header, rows));
    }

    private void dialog(Zahlung bestehend) {
        boolean bearbeiten = bestehend != null;
        List<Kunde> kunden = kundeDao.findeAlle();
        if (kunden.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Bitte zuerst einen Kunden anlegen.").showAndWait(); return; }

        Dialog<Zahlung> dlg = new Dialog<>();
        dlg.setTitle(bearbeiten ? "Zahlung bearbeiten" : "Zahlung hinzufügen");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Kunde> kunde = new ComboBox<>(FXCollections.observableArrayList(kunden));
        kunde.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Kunde k) { return k == null ? "" : "#" + k.getKundennummer() + " – " + k.getVollstaendigerName(); }
            public Kunde fromString(String s) { return null; }
        });
        kunde.getSelectionModel().selectFirst();

        // Zahlung wird pro Quartal erfasst (Quartal + Jahr statt Datum)
        int jahrJetzt = LocalDate.now().getYear();
        ComboBox<String> quartal = new ComboBox<>(FXCollections.observableArrayList("Q1", "Q2", "Q3", "Q4"));
        ComboBox<Integer> jahr = new ComboBox<>(FXCollections.observableArrayList(
                java.util.stream.IntStream.rangeClosed(jahrJetzt - 5, jahrJetzt + 1).boxed().toList()));
        int aktuellesQ = (LocalDate.now().getMonthValue() - 1) / 3 + 1;
        quartal.setValue("Q" + aktuellesQ);
        jahr.setValue(jahrJetzt);

        TextField betrag = new TextField("0");
        ComboBox<String> art = new ComboBox<>(FXCollections.observableArrayList("Krankenkasse", "Überweisung", "Bar", "Privat"));
        art.getSelectionModel().selectFirst();
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("Bezahlt", "Offen", "In Bearbeitung"));
        status.getSelectionModel().selectFirst();
        TextArea notizen = new TextArea(); notizen.setPrefRowCount(2);

        if (bearbeiten) {
            kunden.stream().filter(k -> k.getKundennummer() == bestehend.getKundennummer())
                    .findFirst().ifPresent(kunde::setValue);
            // gespeichert als "Q1 2026"
            String[] teile = bestehend.getZahlungsdatum() == null ? new String[0] : bestehend.getZahlungsdatum().trim().split("\\s+");
            if (teile.length == 2) {
                quartal.setValue(teile[0]);
                try { jahr.setValue(Integer.parseInt(teile[1])); } catch (Exception ignored) {}
            }
            betrag.setText(String.valueOf(bestehend.getBetrag()));
            art.setValue(bestehend.getZahlungsart());
            status.setValue(bestehend.getZahlungsstatus());
            notizen.setText(bestehend.getNotizen());
        }

        HBox quartalBox = new HBox(8, quartal, jahr);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(14));
        int r = 0;
        g.addRow(r++, new Label("Kunde"), kunde);
        g.addRow(r++, new Label("Quartal"), quartalBox);
        g.addRow(r++, new Label("Betrag (€)"), betrag);
        g.addRow(r++, new Label("Zahlungsart"), art);
        g.addRow(r++, new Label("Zahlungsstatus"), status);
        g.addRow(r++, new Label("Notizen"), notizen);
        dlg.getDialogPane().setContent(g);

        // ----- Eingabeprüfung -----
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String fehler = null;
            if (kunde.getValue() == null)        fehler = "Bitte einen Kunden auswählen.";
            else if (quartal.getValue() == null) fehler = "Bitte ein Quartal wählen.";
            else if (jahr.getValue() == null)    fehler = "Bitte ein Jahr wählen.";
            else if (art.getValue() == null)     fehler = "Bitte eine Zahlungsart auswählen.";
            else if (status.getValue() == null)  fehler = "Bitte einen Zahlungsstatus auswählen.";
            else fehler = Eingabe.pruefeZahl(betrag.getText(), "Betrag", 0, 1_000_000);
            if (fehler != null) { Meldung.warnung(fehler); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Zahlung z = new Zahlung();
            if (bearbeiten) z.setId(bestehend.getId());
            z.setKundennummer(kunde.getValue().getKundennummer());
            z.setZahlungsdatum(quartal.getValue() + " " + jahr.getValue());   // z. B. "Q1 2026"
            z.setBetrag(parse(betrag.getText()));
            z.setZahlungsart(art.getValue());
            z.setZahlungsstatus(status.getValue());
            z.setNotizen(notizen.getText());
            return z;
        });

        dlg.showAndWait().ifPresent(z -> {
            if (bearbeiten) dao.aktualisieren(z); else dao.hinzufuegen(z);
            laden();
        });
    }

    private void zahlungLoeschen() {
        Zahlung z = tabelle.getSelectionModel().getSelectedItem();
        if (z == null) return;
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Zahlung Nr. " + z.getId() + " (" + String.format("%.2f €", z.getBetrag()) + ") wirklich löschen?",
                ButtonType.YES, ButtonType.NO);
        c.setHeaderText("Zahlung löschen");
        c.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { dao.loeschen(z.getId()); laden(); }
        });
    }

    private double parse(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return 0; }
    }

    public BorderPane getRoot() { return root; }
}
