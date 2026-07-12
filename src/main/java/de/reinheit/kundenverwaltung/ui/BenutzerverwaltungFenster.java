package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.BenutzerDao;
import de.reinheit.kundenverwaltung.model.Benutzer;
import de.reinheit.kundenverwaltung.service.AuditService;
import de.reinheit.kundenverwaltung.service.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Benutzerverwaltung – nur für Administratoren. Hier können Konten (auch Admins)
 * angelegt und gelöscht werden. Ersetzt die Admin-Auswahl in der öffentlichen
 * Registrierung (Audit H-1).
 */
public class BenutzerverwaltungFenster {

    private final BenutzerDao dao = new BenutzerDao();
    private final BorderPane root = new BorderPane();
    private final TableView<Benutzer> tabelle = new TableView<>();
    private final ObservableList<Benutzer> daten = FXCollections.observableArrayList();

    public BenutzerverwaltungFenster() {
        Button add = new Button("＋ Benutzer anlegen");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> dialog());

        Button loeschen = new Button("🗑 Löschen");
        loeschen.setDisable(true);
        loeschen.setOnAction(e -> benutzerLoeschen());

        HBox bar = new HBox(12, add, loeschen);
        bar.setPadding(new Insets(0, 0, 14, 0));

        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelle.getColumns().setAll(
                col("Id", "id", 50),
                col("Benutzername", "benutzername", 180),
                col("Rolle", "rolle", 120),
                col("Erstellt am", "erstelltAm", 120)
        );
        tabelle.setItems(daten);
        tabelle.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> loeschen.setDisable(n == null));

        root.setTop(bar);
        root.setCenter(tabelle);
        laden();
    }

    private <T> TableColumn<Benutzer, T> col(String t, String prop, double w) {
        TableColumn<Benutzer, T> c = new TableColumn<>(t);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private void laden() { daten.setAll(dao.alleBenutzer()); }

    private void dialog() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Neuen Benutzer anlegen");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField benutzer = new TextField();
        PasswordField passwort = new PasswordField();
        PasswordField passwort2 = new PasswordField();
        ComboBox<String> rolle = new ComboBox<>(FXCollections.observableArrayList("Benutzer", "Admin"));
        rolle.getSelectionModel().selectFirst();
        rolle.setMaxWidth(Double.MAX_VALUE);
        Label hinweis = new Label();
        hinweis.setWrapText(true);
        hinweis.getStyleClass().add("auth-error");
        hinweis.setManaged(false);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(14));
        int r = 0;
        g.addRow(r++, new Label("Benutzername"), benutzer);
        g.addRow(r++, new Label("Passwort (mind. 10)"), passwort);
        g.addRow(r++, new Label("Bestätigen"), passwort2);
        g.addRow(r++, new Label("Rolle"), rolle);
        g.add(hinweis, 0, r, 2, 1);
        dlg.getDialogPane().setContent(g);

        // OK-Button abfangen, um bei Fehler den Dialog offen zu halten
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String u = benutzer.getText().trim();
            String p = passwort.getText();
            hinweis.setManaged(true);
            if (u.isEmpty() || p.isEmpty()) { hinweis.setText("Bitte alle Felder ausfüllen."); ev.consume(); return; }
            if (p.length() < 10) { hinweis.setText("Passwort mind. 10 Zeichen."); ev.consume(); return; }
            if (!p.equals(passwort2.getText())) { hinweis.setText("Passwörter stimmen nicht überein."); ev.consume(); return; }
            if (dao.benutzernameExistiert(u)) { hinweis.setText("Benutzername bereits vergeben."); ev.consume(); return; }
            dao.registrieren(u, p, rolle.getValue());
            AuditService.log("Angelegt", "Benutzer", u + " (" + rolle.getValue() + ")");
        });

        dlg.showAndWait();
        laden();
    }

    private void benutzerLoeschen() {
        Benutzer b = tabelle.getSelectionModel().getSelectedItem();
        if (b == null) return;

        // Schutzregeln
        if ("admin".equalsIgnoreCase(b.getBenutzername())) {
            Meldung.warnung("Das Administratorkonto „admin“ kann nicht gelöscht werden."); return;
        }
        if (b.getBenutzername().equalsIgnoreCase(Session.benutzername())) {
            Meldung.warnung("Sie können Ihr eigenes Konto nicht löschen."); return;
        }
        if (b.istAdmin() && dao.alleBenutzer().stream().filter(Benutzer::istAdmin).count() <= 1) {
            Meldung.warnung("Der letzte Admin-Zugang kann nicht gelöscht werden."); return;
        }

        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Benutzer „" + b.getBenutzername() + "“ wirklich löschen?", ButtonType.YES, ButtonType.NO);
        c.setHeaderText("Benutzer löschen");
        c.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                dao.loeschen(b.getId());
                AuditService.log("Gelöscht", "Benutzer", b.getBenutzername());
                laden();
            }
        });
    }

    public BorderPane getRoot() { return root; }
}
