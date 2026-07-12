package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.EinsatzDao;
import de.reinheit.kundenverwaltung.dao.KundeDao;
import de.reinheit.kundenverwaltung.dao.StammdatenDao;
import de.reinheit.kundenverwaltung.model.Einsatz;
import de.reinheit.kundenverwaltung.model.Kunde;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

/**
 * EinsaetzeFenster: Liste aller Einsätze + Dialog zum Hinzufügen.
 * Status "Erledigt" aktualisiert automatisch die Stunden des Kunden (im DAO).
 */
public class EinsaetzeFenster {

    private final EinsatzDao dao = new EinsatzDao();
    private final KundeDao kundeDao = new KundeDao();
    private final StammdatenDao stammdaten = new StammdatenDao();
    private final de.reinheit.kundenverwaltung.dao.MitarbeiterDao mitarbeiterDao =
            new de.reinheit.kundenverwaltung.dao.MitarbeiterDao();
    private final BorderPane root = new BorderPane();
    private final TableView<Einsatz> tabelle = new TableView<>();
    private final ObservableList<Einsatz> daten = FXCollections.observableArrayList();
    private final ComboBox<Kunde> filter = new ComboBox<>();

    public EinsaetzeFenster() {
        Button add = new Button("＋ Einsatz hinzufügen");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> dialog(null));

        Button bearbeiten = new Button("✏ Bearbeiten");
        bearbeiten.getStyleClass().add("primary");
        bearbeiten.setDisable(true);
        bearbeiten.setOnAction(e -> {
            Einsatz sel = tabelle.getSelectionModel().getSelectedItem();
            if (sel != null) dialog(sel);
        });

        Button loeschen = new Button("🗑 Löschen");
        loeschen.setDisable(true);
        loeschen.setOnAction(e -> einsatzLoeschen());

        Button drucken = new Button("🖨 Drucken");
        drucken.setOnAction(e -> drucken());

        // Filter nach Kunde
        Kunde alle = new Kunde();
        alle.setKundennummer(0);
        alle.setVollstaendigerName("Alle Kunden");
        filter.getItems().add(alle);
        filter.getItems().addAll(kundeDao.findeAlle());
        filter.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Kunde k) {
                if (k == null) return "";
                return k.getKundennummer() == 0 ? "Alle Kunden" : k.getVollstaendigerName();
            }
            public Kunde fromString(String s) { return null; }
        });
        filter.getSelectionModel().selectFirst();
        filter.setOnAction(e -> laden());

        HBox bar = new HBox(12, add, bearbeiten, loeschen, drucken, new Separator(), new Label("Kunde:"), filter);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 14, 0));

        tabelle.setRowFactory(tv -> {
            TableRow<Einsatz> row = new TableRow<>();
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
                col("Kunde", "kundenName", 170),
                datumCol("Datum", "einsatzdatum", 100),
                col("Mitarbeiter", "mitarbeiter", 140),
                col("Dauer (h)", "einsatzdauerStunden", 80),
                col("Leistungsart", "leistungsart", 120),
                col("Status", "einsatzstatus", 120),
                col("Notizen", "notizen", 200)
        );
        tabelle.setItems(daten);

        root.setTop(bar);
        root.setCenter(tabelle);
        laden();
    }

    /** Spalte, die ein ISO-Datum als TT.MM.JJJJ anzeigt. */
    private TableColumn<Einsatz, String> datumCol(String t, String prop, double w) {
        TableColumn<Einsatz, String> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setCellFactory(sp -> new TableCell<>() {
            @Override protected void updateItem(String wert, boolean leer) {
                super.updateItem(wert, leer);
                setText(leer || wert == null ? null : de.reinheit.kundenverwaltung.service.Datum.anzeige(wert));
            }
        });
        return c;
    }

    private <T> TableColumn<Einsatz, T> col(String t, String prop, double w) {
        TableColumn<Einsatz, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    /** Druckt die aktuell angezeigte (gefilterte) Einsatzliste. */
    private void drucken() {
        String[] header = {"Kunde", "Datum", "Mitarbeiter", "Dauer", "Status"};
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (Einsatz e : daten) {
            rows.add(new String[]{
                    e.getKundenName(),
                    de.reinheit.kundenverwaltung.service.Datum.anzeige(e.getEinsatzdatum()),
                    e.getMitarbeiter(),
                    e.getEinsatzdauerStunden() + " h",
                    e.getEinsatzstatus()});
        }
        Kunde gew = filter.getValue();
        String titel = (gew != null && gew.getKundennummer() != 0)
                ? "Einsatzliste – " + gew.getVollstaendigerName() : "Einsatzliste";
        javafx.stage.Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        Drucker.drucke(owner, (ziel, breite) -> Bericht.liste(ziel, breite, titel, header, rows));
    }

    private void laden() {
        // Kundennamen zuordnen
        java.util.Map<Integer, String> namen = new java.util.HashMap<>();
        for (Kunde k : kundeDao.findeAlle()) namen.put(k.getKundennummer(), k.getVollstaendigerName());

        Kunde gewaehlt = filter.getValue();
        int nur = (gewaehlt == null) ? 0 : gewaehlt.getKundennummer();   // 0 = alle

        java.util.List<Einsatz> liste = new java.util.ArrayList<>();
        for (Einsatz e : dao.findeAlle()) {
            if (nur != 0 && e.getKundennummer() != nur) continue;
            e.setKundenName(namen.getOrDefault(e.getKundennummer(), "#" + e.getKundennummer()));
            liste.add(e);
        }
        daten.setAll(liste);
    }

    private void dialog(Einsatz bestehend) {
        boolean bearbeiten = bestehend != null;
        List<Kunde> kunden = kundeDao.findeAlle();
        if (kunden.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Bitte zuerst einen Kunden anlegen.").showAndWait(); return; }

        Dialog<Einsatz> dlg = new Dialog<>();
        dlg.setTitle(bearbeiten ? "Einsatz bearbeiten" : "Einsatz hinzufügen");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Kunde> kunde = new ComboBox<>(FXCollections.observableArrayList(kunden));
        kunde.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Kunde k) { return k == null ? "" : "#" + k.getKundennummer() + " – " + k.getVollstaendigerName(); }
            public Kunde fromString(String s) { return null; }
        });
        kunde.getSelectionModel().selectFirst();

        DatePicker datum = new DatePicker(LocalDate.now());
        datum.setPromptText(KundenFenster.DATUM_FORMAT);
        datum.setConverter(de.reinheit.kundenverwaltung.service.Datum.konverter());
        // Mitarbeiter = Benutzerkonten; direkt hier anlegen/löschen möglich (nur Admin)
        ComboBox<String> mitarbeiter = new ComboBox<>();
        TextField dauer = new TextField("1.5");
        ComboBox<String> leistung = new ComboBox<>(FXCollections.observableArrayList(
                stammdaten.liste(StammdatenDao.LEISTUNGSART)));
        leistung.getSelectionModel().selectFirst();
        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("Erledigt", "Abgesagt", "Nicht durchgeführt"));
        status.getSelectionModel().selectFirst();
        TextArea notizen = new TextArea(); notizen.setPrefRowCount(2);

        if (bearbeiten) {
            kunden.stream().filter(k -> k.getKundennummer() == bestehend.getKundennummer())
                    .findFirst().ifPresent(kunde::setValue);
            try { datum.setValue(LocalDate.parse(bestehend.getEinsatzdatum())); } catch (Exception ignored) {}
            mitarbeiter.setValue(bestehend.getMitarbeiter());
            dauer.setText(String.valueOf(bestehend.getEinsatzdauerStunden()));
            leistung.setValue(bestehend.getLeistungsart());
            status.setValue(bestehend.getEinsatzstatus());
            notizen.setText(bestehend.getNotizen());
        }

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(14));
        int r = 0;
        g.addRow(r++, new Label("Kunde"), kunde);
        g.addRow(r++, new Label("Einsatzdatum (" + KundenFenster.DATUM_FORMAT + ")"), datum);
        g.addRow(r++, new Label("Mitarbeiter"), MitarbeiterFeld.erstelle(mitarbeiter, mitarbeiterDao));
        g.addRow(r++, new Label("Einsatzdauer (Stunden)"), dauer);
        g.addRow(r++, new Label("Leistungsart"), leistung);
        g.addRow(r++, new Label("Einsatzstatus"), status);
        g.addRow(r++, new Label("Notizen"), notizen);
        g.add(new Label("ℹ „Erledigt“ erhöht erbrachte Stunden und senkt verbleibende Stunden."), 0, r, 2, 1);
        dlg.getDialogPane().setContent(g);

        // ----- Eingabeprüfung: Dialog bleibt bei Fehlern offen -----
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String fehler = null;
            if (kunde.getValue() == null)            fehler = "Bitte einen Kunden auswählen.";
            else if (datum.getValue() == null)       fehler = "Bitte ein gültiges Einsatzdatum wählen.";
            else if (mitarbeiter.getValue() == null) fehler = "Bitte einen Mitarbeiter auswählen.";
            else if (leistung.getValue() == null)    fehler = "Bitte eine Leistungsart auswählen.";
            else fehler = Eingabe.pruefeZahl(dauer.getText(), "Einsatzdauer", 0.25, 24);
            if (fehler != null) { Meldung.warnung(fehler); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Einsatz e = new Einsatz();
            if (bearbeiten) e.setId(bestehend.getId());
            e.setKundennummer(kunde.getValue().getKundennummer());
            e.setEinsatzdatum(datum.getValue().toString());
            e.setMitarbeiter(mitarbeiter.getValue());
            e.setEinsatzdauerStunden(parse(dauer.getText()));
            e.setLeistungsart(leistung.getValue());
            e.setEinsatzstatus(status.getValue());
            e.setNotizen(notizen.getText());
            return e;
        });

        dlg.showAndWait().ifPresent(e -> {
            if (bearbeiten) dao.aktualisieren(bestehend, e); else dao.hinzufuegen(e);
            laden();
        });
    }

    private void einsatzLoeschen() {
        Einsatz e = tabelle.getSelectionModel().getSelectedItem();
        if (e == null) return;
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Einsatz Nr. " + e.getId() + " wirklich löschen?"
                + ("Erledigt".equals(e.getEinsatzstatus())
                    ? "\nDie gebuchten " + e.getEinsatzdauerStunden() + " h werden dem Kunden wieder gutgeschrieben." : ""),
                ButtonType.YES, ButtonType.NO);
        c.setHeaderText("Einsatz löschen");
        c.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { dao.loeschen(e); laden(); }
        });
    }

    private double parse(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return 1.5; }
    }

    public BorderPane getRoot() { return root; }
}
