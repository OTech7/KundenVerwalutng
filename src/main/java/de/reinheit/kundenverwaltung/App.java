package de.reinheit.kundenverwaltung;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.service.Session;
import de.reinheit.kundenverwaltung.ui.Hauptfenster;
import de.reinheit.kundenverwaltung.ui.LoginFenster;
import de.reinheit.kundenverwaltung.ui.Meldung;
import de.reinheit.kundenverwaltung.ui.RegistrierungFenster;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/** Einstiegspunkt der Anwendung. Startet mit dem Anmeldebildschirm. */
public class App extends Application {

    private Stage stage;
    private Scene scene;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Unerwartete Fehler abfangen, statt die Anwendung abstürzen zu lassen
        Meldung.installiereGlobalenHandler();

        try {
            Database.init();   // Schema + Beispiel-/Admin-Daten beim Start
        } catch (Exception e) {
            System.err.println("Datenbank-Initialisierung fehlgeschlagen: " + e);
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Die Datenbank konnte nicht geöffnet werden.\n"
                    + "Bitte prüfen Sie, ob die Datei 'kunden.db' beschreibbar ist.");
            a.setHeaderText("Start nicht möglich");
            a.showAndWait();
            Platform.exit();
            return;
        }

        scene = new Scene(new javafx.scene.layout.StackPane(), 1000, 640);
        var css = App.class.getResource("/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("KundenVerwaltung – Reinheit & Sauberkeit GmbH");
        stage.setScene(scene);

        zeigeLogin();
        stage.show();
    }

    private void zeigeLogin() {
        stage.setWidth(560);
        stage.setHeight(640);
        setRoot(new LoginFenster(this::zeigeHauptfenster, this::zeigeRegistrierung).getRoot());
        stage.centerOnScreen();
    }

    private void zeigeRegistrierung() {
        setRoot(new RegistrierungFenster(this::zeigeLogin).getRoot());
    }

    private void zeigeHauptfenster() {
        stage.setWidth(1000);
        stage.setHeight(640);
        setRoot(new Hauptfenster(this::abmelden).getRoot());
        stage.centerOnScreen();
    }

    private void abmelden() {
        Session.abmelden();
        zeigeLogin();
    }

    private void setRoot(Parent root) {
        scene.setRoot(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
