package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.KundeDao;
import de.reinheit.kundenverwaltung.dao.TerminDao;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Termin;
import de.reinheit.kundenverwaltung.service.Datum;
import de.reinheit.kundenverwaltung.service.TerminGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TermineFenster: zeigt die automatisch erzeugten Termine, optional gefiltert
 * nach Kundennummer (laut Spezifikation). Termine entstehen beim Anlegen eines
 * Kunden aus dessen TerminPlan (Woche 1+3 oder 2+4), 1,5 h je Termin.
 */
public class TermineFenster {

    private final TerminDao dao = new TerminDao();
    private final KundeDao kundeDao = new KundeDao();
    private final BorderPane root = new BorderPane();
    private final TableView<Termin> tabelle = new TableView<>();
    private final ObservableList<Termin> daten = FXCollections.observableArrayList();
    private final ComboBox<Kunde> filter = new ComboBox<>();

    public TermineFenster() {
        Label lbl = new Label("Kunde:");
        Kunde alle = new Kunde();
        alle.setKundennummer(0);
        alle.setVollstaendigerName("Alle Kunden");
        filter.getItems().add(alle);
        filter.getItems().addAll(kundeDao.findeAlle());
        filter.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Kunde k) {
                if (k == null) return "";
                return k.getKundennummer() == 0 ? "Alle Kunden" : "#" + k.getKundennummer() + " – " + k.getVollstaendigerName();
            }
            public Kunde fromString(String s) { return null; }
        });
        filter.getSelectionModel().selectFirst();
        filter.setOnAction(e -> {
            Kunde k = filter.getValue();
            laden(k == null || k.getKundennummer() == 0 ? null : k.getKundennummer());
        });

        Button erzeugen = new Button("＋ Termine erzeugen (Zeitraum)");
        erzeugen.getStyleClass().add("primary");
        erzeugen.setOnAction(e -> dialogErzeugen());

        Button bearbeiten = new Button("✏ Bearbeiten");
        bearbeiten.getStyleClass().add("primary");
        bearbeiten.setDisable(true);
        bearbeiten.setOnAction(e -> {
            Termin t = tabelle.getSelectionModel().getSelectedItem();
            if (t != null) dialogBearbeiten(t);
        });

        Button loeschen = new Button("🗑 Löschen");
        loeschen.setDisable(true);
        loeschen.setOnAction(e -> termineLoeschen());

        Button drucken = new Button("🖨 Drucken");
        drucken.setOnAction(e -> drucken());

        Label hinweis = new Label("Mehrfachauswahl zum Löschen · Doppelklick zum Bearbeiten");
        hinweis.getStyleClass().add("platzhalter");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(10, lbl, filter, bearbeiten, loeschen, drucken, hinweis, sp, erzeugen);
        bar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 14, 0));

        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelle.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabelle.getColumns().setAll(
                col("Kunde", "kundenName", 180),
                datumCol("Termindatum", "terminDatum", 110),
                col("Wochentag", "wochentag", 110),
                col("Woche", "woche", 90),
                col("Dauer (h)", "dauer", 80),
                col("Status", "status", 130),
                col("Notizen", "notizen", 200)
        );
        tabelle.setItems(daten);

        // Doppelklick öffnet den Bearbeiten-Dialog
        tabelle.setRowFactory(tv -> {
            TableRow<Termin> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) dialogBearbeiten(row.getItem());
            });
            return row;
        });
        tabelle.getSelectionModel().selectedItemProperty()
                .addListener((o, a, n) -> {
                    bearbeiten.setDisable(n == null);
                    loeschen.setDisable(n == null);
                });

        root.setTop(bar);
        root.setCenter(tabelle);
        laden(null);
    }

    /** Termin bearbeiten: Datum, Dauer, Status und Notizen. */
    private void dialogBearbeiten(Termin t) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Termin bearbeiten");
        dlg.setHeaderText(t.getKundenName());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        DatePicker datum = new DatePicker();
        datum.setPromptText(KundenFenster.DATUM_FORMAT);
        datum.setConverter(Datum.konverter());
        try { datum.setValue(LocalDate.parse(t.getTerminDatum())); } catch (Exception ignored) {}

        TextField dauer = new TextField(String.valueOf(t.getDauer()));

        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList(
                "", "Erledigt", "Abgesagt", "Nicht durchgeführt"));
        status.setValue(t.getStatus() == null ? "" : t.getStatus());
        status.setMaxWidth(Double.MAX_VALUE);

        TextArea notizen = new TextArea(t.getNotizen());
        notizen.setPrefRowCount(2);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(14));
        int r = 0;
        g.addRow(r++, new Label("Termindatum (" + KundenFenster.DATUM_FORMAT + ")"), datum);
        g.addRow(r++, new Label("Dauer (Std.)"), dauer);
        g.addRow(r++, new Label("Status"), status);
        g.addRow(r++, new Label("Notizen"), notizen);
        dlg.getDialogPane().setContent(g);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String fehler = null;
            if (datum.getValue() == null) fehler = "Bitte ein Termindatum wählen.";
            else fehler = Eingabe.pruefeZahl(dauer.getText(), "Dauer", 0.25, 24);
            if (fehler != null) { Meldung.warnung(fehler); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            LocalDate d = datum.getValue();
            t.setTerminDatum(d.toString());
            t.setWoche("Woche " + ((d.getDayOfMonth() - 1) / 7 + 1));   // Woche aus Datum neu berechnen
            Double du = Eingabe.zahl(dauer.getText());
            if (du != null) t.setDauer(du);
            String s = status.getValue();
            t.setStatus(s == null || s.isBlank() ? null : s);
            t.setNotizen(notizen.getText());
            dao.aktualisieren(t);
            Integer k = filter.getValue() == null || filter.getValue().getKundennummer() == 0
                    ? null : filter.getValue().getKundennummer();
            laden(k);
            return null;
        });
        dlg.showAndWait();
    }

    /** Löscht die aktuell ausgewählten Termine (Mehrfachauswahl möglich). */
    private void termineLoeschen() {
        List<Termin> auswahl = new java.util.ArrayList<>(tabelle.getSelectionModel().getSelectedItems());
        if (auswahl.isEmpty()) return;
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                auswahl.size() == 1
                        ? "Diesen Termin wirklich löschen?"
                        : auswahl.size() + " Termine wirklich löschen?",
                ButtonType.YES, ButtonType.NO);
        c.setHeaderText("Termine löschen");
        c.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            for (Termin t : auswahl) dao.loeschen(t.getId());
            Integer k = filter.getValue() == null || filter.getValue().getKundennummer() == 0
                    ? null : filter.getValue().getKundennummer();
            laden(k);
        });
    }

    private <T> TableColumn<Termin, T> col(String t, String prop, double w) {
        TableColumn<Termin, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    /** Spalte, die ein ISO-Datum als TT.MM.JJJJ anzeigt. */
    private TableColumn<Termin, String> datumCol(String t, String prop, double w) {
        TableColumn<Termin, String> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        c.setCellFactory(sp -> new TableCell<>() {
            @Override protected void updateItem(String wert, boolean leer) {
                super.updateItem(wert, leer);
                setText(leer || wert == null ? null : Datum.anzeige(wert));
            }
        });
        return c;
    }

    /** Druckt die aktuell angezeigte (gefilterte) Terminliste. */
    private void drucken() {
        String[] header = {"Kunde", "Datum", "Wochentag", "Woche", "Dauer", "Status"};
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        for (Termin t : daten) {
            rows.add(new String[]{
                    t.getKundenName(),
                    Datum.anzeige(t.getTerminDatum()),
                    t.getWochentag(),
                    t.getWoche(),
                    t.getDauer() + " h",
                    t.getStatus() == null ? "" : t.getStatus()});
        }
        Kunde gew = filter.getValue();
        String titel = (gew != null && gew.getKundennummer() != 0)
                ? "Terminliste – " + gew.getVollstaendigerName() : "Terminliste";
        javafx.stage.Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        Drucker.drucke(owner, (ziel, breite) -> Bericht.liste(ziel, breite, titel, header, rows));
    }

    private void laden(Integer kundennummer) {
        // Kundennamen für die Anzeige zuordnen
        Map<Integer, String> namen = new HashMap<>();
        for (Kunde k : kundeDao.findeAlle()) namen.put(k.getKundennummer(), k.getVollstaendigerName());

        List<Termin> t = dao.finde(kundennummer);
        for (Termin x : t) {
            x.setKundenName(namen.getOrDefault(x.getKundennummer(), "#" + x.getKundennummer()));
        }
        daten.setAll(t);
    }

    /** Dialog: Termine für einen Kunden im Zeitraum von–bis erzeugen. */
    private void dialogErzeugen() {
        List<Kunde> kunden = kundeDao.findeAlle();
        if (kunden.isEmpty()) { new Alert(Alert.AlertType.WARNING, "Bitte zuerst einen Kunden anlegen.").showAndWait(); return; }

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Termine erzeugen");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Kunde> kunde = new ComboBox<>(FXCollections.observableArrayList(kunden));
        kunde.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Kunde k) { return k == null ? "" : "#" + k.getKundennummer() + " – " + k.getVollstaendigerName(); }
            public Kunde fromString(String s) { return null; }
        });
        kunde.getSelectionModel().selectFirst();

        DatePicker von = new DatePicker(LocalDate.now());
        von.setPromptText(KundenFenster.DATUM_FORMAT);
        von.setConverter(Datum.konverter());
        DatePicker bis = new DatePicker(LocalDate.now().plusMonths(3));
        bis.setPromptText(KundenFenster.DATUM_FORMAT);
        bis.setConverter(Datum.konverter());
        ComboBox<String> wochentag = new ComboBox<>(FXCollections.observableArrayList(
                "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"));
        wochentag.setValue("Montag");
        ComboBox<String> plan = new ComboBox<>(FXCollections.observableArrayList("Woche 1 und 3", "Woche 2 und 4", "Wöchentlich"));
        TextField dauer = new TextField("1.5");

        // Vorbelegung aus dem gewählten Kunden
        Runnable ausKunde = () -> {
            Kunde k = kunde.getValue();
            if (k != null) {
                plan.setValue(k.getTerminPlan() != null ? k.getTerminPlan() : "Woche 1 und 3");
                dauer.setText(String.valueOf(k.getStandardTerminDauer() > 0 ? k.getStandardTerminDauer() : 1.5));
                String vorTag = tagAusText(k.getWochentage());
                if (vorTag != null) wochentag.setValue(vorTag);
            }
        };
        ausKunde.run();
        kunde.setOnAction(e -> ausKunde.run());

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(14));
        int r = 0;
        g.addRow(r++, new Label("Kunde"), kunde);
        g.addRow(r++, new Label("Von (" + KundenFenster.DATUM_FORMAT + ")"), von);
        g.addRow(r++, new Label("Bis (" + KundenFenster.DATUM_FORMAT + ")"), bis);
        g.addRow(r++, new Label("Wochentag"), wochentag);
        g.addRow(r++, new Label("Plan"), plan);
        g.addRow(r++, new Label("Dauer je Termin (Std.)"), dauer);
        dlg.getDialogPane().setContent(g);

        // ----- Eingabeprüfung -----
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String fehler = null;
            if (kunde.getValue() == null)      fehler = "Bitte einen Kunden auswählen.";
            else if (von.getValue() == null)   fehler = "Bitte ein Startdatum (Von) wählen.";
            else if (bis.getValue() == null)   fehler = "Bitte ein Enddatum (Bis) wählen.";
            else if (bis.getValue().isBefore(von.getValue()))
                                               fehler = "Das Enddatum darf nicht vor dem Startdatum liegen.";
            else if (von.getValue().plusYears(5).isBefore(bis.getValue()))
                                               fehler = "Der Zeitraum darf höchstens 5 Jahre umfassen.";
            else if (wochentag.getValue() == null) fehler = "Bitte einen Wochentag auswählen.";
            else if (plan.getValue() == null)  fehler = "Bitte einen Plan auswählen.";
            else fehler = Eingabe.pruefeZahl(dauer.getText(), "Dauer je Termin", 0.25, 24);
            if (fehler != null) { Meldung.warnung(fehler); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Kunde k = kunde.getValue();
            Double parsed = Eingabe.zahl(dauer.getText());
            double d = parsed != null ? parsed : 1.5;
            DayOfWeek tag = tagAus(wochentag.getValue());
            List<Termin> neu = new TerminGenerator().fuerZeitraum(k, von.getValue(), bis.getValue(), d, plan.getValue(), tag);
            if (neu.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Keine Termine im gewählten Zeitraum.").showAndWait();
            } else {
                int n = dao.speichern(neu);
                new de.reinheit.kundenverwaltung.dao.EinsatzDao().erzeugeAusTerminen(neu);
                laden(null);
                new Alert(Alert.AlertType.INFORMATION,
                        n + " Termine erzeugt für " + k.getVollstaendigerName() + ".\n"
                        + "Dazu wurde 1 Sammel-Einsatz (" + n + " Termine) automatisch angelegt.").showAndWait();
            }
            return null;
        });
        dlg.showAndWait();
    }

    /** Deutscher Wochentagsname -> DayOfWeek (Standard: Montag). */
    private DayOfWeek tagAus(String name) {
        if (name == null) return DayOfWeek.MONDAY;
        return switch (name) {
            case "Dienstag"   -> DayOfWeek.TUESDAY;
            case "Mittwoch"   -> DayOfWeek.WEDNESDAY;
            case "Donnerstag" -> DayOfWeek.THURSDAY;
            case "Freitag"    -> DayOfWeek.FRIDAY;
            case "Samstag"    -> DayOfWeek.SATURDAY;
            case "Sonntag"    -> DayOfWeek.SUNDAY;
            default            -> DayOfWeek.MONDAY;
        };
    }

    /** Sucht in einem freien Text (z. B. Kunden-Wochentage) einen bekannten Wochentag. */
    private String tagAusText(String text) {
        if (text == null) return null;
        String s = text.toLowerCase();
        if (s.contains("montag"))     return "Montag";
        if (s.contains("dienstag"))   return "Dienstag";
        if (s.contains("mittwoch"))   return "Mittwoch";
        if (s.contains("donnerstag")) return "Donnerstag";
        if (s.contains("freitag"))    return "Freitag";
        if (s.contains("samstag") || s.contains("sonnabend")) return "Samstag";
        if (s.contains("sonntag"))    return "Sonntag";
        return null;
    }

    public BorderPane getRoot() { return root; }
}
