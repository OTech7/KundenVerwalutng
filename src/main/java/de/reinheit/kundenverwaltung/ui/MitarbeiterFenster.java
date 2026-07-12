package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.MitarbeiterDao;
import de.reinheit.kundenverwaltung.model.Mitarbeiter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

/**
 * Bereich „Mitarbeiter": einfache Namensliste. Wird überall als Auswahl für
 * „Zuständiger Mitarbeiter" (Kunden) und „Mitarbeiter" (Einsätze) verwendet.
 * Mitarbeiter haben kein Benutzerkonto und kein Passwort.
 */
public class MitarbeiterFenster {

    private final MitarbeiterDao dao = new MitarbeiterDao();
    private final BorderPane root = new BorderPane();
    private final TableView<Mitarbeiter> tabelle = new TableView<>();
    private final ObservableList<Mitarbeiter> daten = FXCollections.observableArrayList();

    public MitarbeiterFenster() {
        Button add = new Button("＋ Mitarbeiter hinzufügen");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> hinzufuegen());

        Button umbenennen = new Button("✏ Umbenennen");
        umbenennen.setDisable(true);
        umbenennen.setOnAction(e -> umbenennen());

        Button loeschen = new Button("🗑 Löschen");
        loeschen.setDisable(true);
        loeschen.setOnAction(e -> loeschen());

        HBox bar = new HBox(12, add, umbenennen, loeschen);
        bar.setPadding(new Insets(0, 0, 14, 0));

        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<Mitarbeiter, String> spName = new TableColumn<>("Name");
        spName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tabelle.getColumns().setAll(java.util.List.of(spName));
        tabelle.setItems(daten);
        tabelle.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            umbenennen.setDisable(n == null);
            loeschen.setDisable(n == null);
        });
        tabelle.setRowFactory(tv -> {
            TableRow<Mitarbeiter> row = new TableRow<>();
            row.setOnMouseClicked(ev -> { if (ev.getClickCount() == 2 && !row.isEmpty()) umbenennen(); });
            return row;
        });

        root.setTop(bar);
        root.setCenter(tabelle);
        laden();
    }

    private void laden() { daten.setAll(dao.alle()); }

    private void hinzufuegen() {
        TextInputDialog td = new TextInputDialog();
        td.setTitle("Neuer Mitarbeiter");
        td.setHeaderText("Mitarbeiter hinzufügen");
        td.setContentText("Name:");
        td.showAndWait().ifPresent(name -> {
            if (name.isBlank()) { Meldung.warnung("Bitte einen Namen eingeben."); return; }
            dao.hinzufuegen(name.trim());
            laden();
        });
    }

    private void umbenennen() {
        Mitarbeiter m = tabelle.getSelectionModel().getSelectedItem();
        if (m == null) return;
        TextInputDialog td = new TextInputDialog(m.getName());
        td.setTitle("Mitarbeiter umbenennen");
        td.setHeaderText("Neuen Namen eingeben");
        td.setContentText("Name:");
        td.showAndWait().ifPresent(name -> {
            if (name.isBlank()) { Meldung.warnung("Bitte einen Namen eingeben."); return; }
            dao.umbenennen(m.getId(), name.trim());
            laden();
        });
    }

    private void loeschen() {
        Mitarbeiter m = tabelle.getSelectionModel().getSelectedItem();
        if (m == null) return;
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Mitarbeiter „" + m.getName() + "“ wirklich löschen?\n"
                + "Bereits erfasste Kunden und Einsätze behalten den Namen.",
                ButtonType.YES, ButtonType.NO);
        c.setHeaderText("Mitarbeiter löschen");
        if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            dao.loeschen(m.getId());
            laden();
        }
    }

    public BorderPane getRoot() { return root; }
}
