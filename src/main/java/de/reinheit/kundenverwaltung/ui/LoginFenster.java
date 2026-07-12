package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.BenutzerDao;
import de.reinheit.kundenverwaltung.model.Benutzer;
import de.reinheit.kundenverwaltung.service.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Anmeldebildschirm. Bei erfolgreicher Anmeldung wird onErfolg ausgeführt.
 * Über "Konto erstellen" gelangt man zur Registrierung.
 */
public class LoginFenster {

    // Brute-Force-Schutz (prozessweit)
    private static final int MAX_VERSUCHE = 5;
    private static final long SPERRDAUER_MS = 30_000;
    private static final Map<String, Integer> VERSUCHE = new HashMap<>();
    private static final Map<String, Long> SPERRE_BIS = new HashMap<>();

    private final BenutzerDao dao = new BenutzerDao();
    private final StackPane root = new StackPane();

    public LoginFenster(Runnable onErfolg, Runnable zurRegistrierung) {
        root.getStyleClass().add("auth-bg");

        VBox karte = new VBox(14);
        karte.getStyleClass().add("auth-card");
        karte.setAlignment(Pos.CENTER);
        karte.setMaxWidth(360);
        karte.setMaxHeight(Region.USE_PREF_SIZE);
        karte.setPadding(new Insets(28));

        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
            logo.setFitWidth(260);
            logo.setPreserveRatio(true);
            karte.getChildren().add(logo);
        }

        Label titel = new Label("Anmelden");
        titel.getStyleClass().add("auth-title");

        TextField benutzer = new TextField();
        benutzer.setPromptText("Benutzername");
        PasswordField passwort = new PasswordField();
        passwort.setPromptText("Passwort");

        Label fehler = new Label();
        fehler.getStyleClass().add("auth-error");
        fehler.setWrapText(true);
        fehler.setManaged(false);

        Button anmelden = new Button("Anmelden");
        anmelden.getStyleClass().add("primary");
        anmelden.setMaxWidth(Double.MAX_VALUE);

        Hyperlink registrieren = new Hyperlink("Noch kein Konto? Konto erstellen");

        Runnable login = () -> {
            fehler.setManaged(false);
            String u = benutzer.getText().trim();
            String p = passwort.getText();
            if (u.isEmpty() || p.isEmpty()) { zeigeFehler(fehler, "Bitte Benutzername und Passwort eingeben."); return; }

            // Brute-Force-Schutz (Audit M-3): nach zu vielen Fehlversuchen kurz sperren
            long jetzt = System.currentTimeMillis();
            Long sperre = SPERRE_BIS.get(u.toLowerCase());
            if (sperre != null && jetzt < sperre) {
                long sek = (sperre - jetzt) / 1000 + 1;
                zeigeFehler(fehler, "Zu viele Fehlversuche. Bitte " + sek + " s warten.");
                return;
            }

            Benutzer b = dao.anmelden(u, p);
            if (b == null) {
                int n = VERSUCHE.merge(u.toLowerCase(), 1, Integer::sum);
                if (n >= MAX_VERSUCHE) {
                    SPERRE_BIS.put(u.toLowerCase(), jetzt + SPERRDAUER_MS);
                    VERSUCHE.remove(u.toLowerCase());
                    zeigeFehler(fehler, "Zu viele Fehlversuche. Konto für 30 s gesperrt.");
                } else {
                    zeigeFehler(fehler, "Benutzername oder Passwort ist falsch. (" + n + "/" + MAX_VERSUCHE + ")");
                }
                return;
            }
            // Erfolg: Zähler zurücksetzen
            VERSUCHE.remove(u.toLowerCase());
            SPERRE_BIS.remove(u.toLowerCase());

            // Erzwungene Passwortänderung beim ersten Login (Audit M-1)
            if (b.isMussPasswortAendern()) {
                if (!passwortAendern(b.getBenutzername())) return;  // abgebrochen -> nicht anmelden
                b.setMussPasswortAendern(false);
            }
            Session.anmelden(b);
            onErfolg.run();
        };
        anmelden.setOnAction(e -> login.run());
        passwort.setOnAction(e -> login.run());   // Enter im Passwortfeld
        registrieren.setOnAction(e -> zurRegistrierung.run());

        Label hinweis = new Label("Erstanmeldung erfordert eine Passwortänderung.");
        hinweis.getStyleClass().add("auth-hint");

        karte.getChildren().addAll(titel, benutzer, passwort, fehler, anmelden, registrieren, hinweis);
        root.getChildren().add(karte);
    }

    private void zeigeFehler(Label l, String text) {
        l.setText(text);
        l.setManaged(true);
    }

    /** Erzwungene Passwortänderung. Gibt true zurück, wenn erfolgreich geändert. */
    private boolean passwortAendern(String benutzername) {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Passwort ändern");
        dlg.setHeaderText("Bitte vergeben Sie ein neues Passwort für „" + benutzername + "“.");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField p1 = new PasswordField(); p1.setPromptText("Neues Passwort (mind. 10 Zeichen)");
        PasswordField p2 = new PasswordField(); p2.setPromptText("Bestätigen");
        Label fehler = new Label(); fehler.getStyleClass().add("auth-error"); fehler.setManaged(false);
        VBox box = new VBox(10, p1, p2, fehler);
        box.setPadding(new Insets(12));
        dlg.getDialogPane().setContent(box);

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            fehler.setManaged(true);
            String a = p1.getText(), b = p2.getText();
            if (a.length() < 10) { fehler.setText("Mindestens 10 Zeichen."); ev.consume(); return; }
            if (!a.equals(b)) { fehler.setText("Passwörter stimmen nicht überein."); ev.consume(); return; }
            dao.passwortSetzen(benutzername, a);
        });
        dlg.setResultConverter(bt -> bt == ButtonType.OK);
        return Boolean.TRUE.equals(dlg.showAndWait().orElse(false));
    }

    public StackPane getRoot() { return root; }
}
