package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.MitarbeiterDao;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Auswahlfeld für Mitarbeiter (nur Namen), mit "＋"-Knopf zum schnellen Anlegen.
 * Die vollständige Verwaltung erfolgt im Bereich „Mitarbeiter".
 */
public final class MitarbeiterFeld {

    private MitarbeiterFeld() {}

    /** Baut das Feld: Auswahlliste + Knopf zum Hinzufügen eines neuen Namens. */
    public static HBox erstelle(ComboBox<String> combo, MitarbeiterDao dao) {
        String vorbelegt = combo.getValue();     // beim Bearbeiten nicht verlieren
        combo.setItems(FXCollections.observableArrayList(dao.namen()));
        if (vorbelegt != null) combo.setValue(vorbelegt);
        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);

        Button plus = new Button("＋");
        plus.setTooltip(new Tooltip("Neuen Mitarbeiter hinzufügen"));
        plus.setOnAction(e -> {
            TextInputDialog td = new TextInputDialog();
            td.setTitle("Neuer Mitarbeiter");
            td.setHeaderText("Mitarbeiter hinzufügen");
            td.setContentText("Name:");
            td.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    dao.hinzufuegen(name.trim());
                    combo.setItems(FXCollections.observableArrayList(dao.namen()));
                    combo.setValue(name.trim());
                }
            });
        });

        return new HBox(6, combo, plus);
    }
}
