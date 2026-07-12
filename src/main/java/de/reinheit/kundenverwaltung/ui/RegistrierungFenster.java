package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.BenutzerDao;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

/**
 * Registrierungsbildschirm: legt einen neuen Benutzer an (Passwort gehasht).
 * Nach Erfolg geht es zurück zur Anmeldung.
 */
public class RegistrierungFenster {

    private final BenutzerDao dao = new BenutzerDao();
    private final StackPane root = new StackPane();

    public RegistrierungFenster(Runnable zurueckZumLogin) {
        root.getStyleClass().add("auth-bg");

        VBox karte = new VBox(12);
        karte.getStyleClass().add("auth-card");
        karte.setAlignment(Pos.CENTER);
        karte.setMaxWidth(380);
        karte.setMaxHeight(Region.USE_PREF_SIZE);
        karte.setPadding(new Insets(26));

        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
            logo.setFitWidth(230);
            logo.setPreserveRatio(true);
            karte.getChildren().add(logo);
        }

        Label titel = new Label("Konto erstellen");
        titel.getStyleClass().add("auth-title");

        TextField benutzer = new TextField();
        benutzer.setPromptText("Benutzername");
        PasswordField passwort = new PasswordField();
        passwort.setPromptText("Passwort (mind. 10 Zeichen)");
        PasswordField passwort2 = new PasswordField();
        passwort2.setPromptText("Passwort bestätigen");

        // Öffentliche Registrierung erstellt ausschließlich Mitarbeiter-Konten.
        // Admin-Konten können nur von einem angemeldeten Admin angelegt werden
        // (Bereich „Benutzer"). Verhindert Rechteausweitung (Audit H-1).
        Label rolleInfo = new Label("Rolle: Benutzer");
        rolleInfo.getStyleClass().add("auth-label");

        Label meldung = new Label();
        meldung.setWrapText(true);
        meldung.setManaged(false);

        Button erstellen = new Button("Konto erstellen");
        erstellen.getStyleClass().add("primary");
        erstellen.setMaxWidth(Double.MAX_VALUE);

        Hyperlink zurueck = new Hyperlink("Zurück zur Anmeldung");
        zurueck.setOnAction(e -> zurueckZumLogin.run());

        erstellen.setOnAction(e -> {
            meldung.setManaged(true);
            String u = benutzer.getText().trim();
            String p = passwort.getText();
            String p2 = passwort2.getText();
            if (u.isEmpty() || p.isEmpty()) { fehler(meldung, "Bitte alle Felder ausfüllen."); return; }
            if (p.length() < 10) { fehler(meldung, "Das Passwort muss mindestens 10 Zeichen haben."); return; }
            if (!p.equals(p2)) { fehler(meldung, "Die Passwörter stimmen nicht überein."); return; }
            if (dao.benutzernameExistiert(u)) { fehler(meldung, "Dieser Benutzername ist bereits vergeben."); return; }
            dao.registrieren(u, p, "Benutzer");   // öffentliche Registrierung: nur Rolle Benutzer
            erfolg(meldung, "Konto erstellt. Sie können sich jetzt anmelden.");
            benutzer.setDisable(true); passwort.setDisable(true); passwort2.setDisable(true);
            erstellen.setDisable(true);
        });

        karte.getChildren().addAll(titel, benutzer, passwort, passwort2,
                rolleInfo, meldung, erstellen, zurueck);
        root.getChildren().add(karte);
    }

    private void fehler(Label l, String text) { l.setText(text); l.getStyleClass().setAll("auth-error"); }
    private void erfolg(Label l, String text) { l.setText(text); l.getStyleClass().setAll("auth-success"); }

    public StackPane getRoot() { return root; }
}
