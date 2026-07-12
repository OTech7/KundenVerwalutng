package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.model.Benutzer;
import de.reinheit.kundenverwaltung.service.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

/**
 * Hauptfenster mit Navigations-Buttons (laut Spezifikation):
 * Kunden, Einsätze, Zahlungen, Dashboard, Termine, Beenden.
 * Der zentrale Bereich tauscht das jeweilige Fenster aus.
 */
public class Hauptfenster {

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Runnable onAbmelden;

    public Hauptfenster(Runnable onAbmelden) {
        this.onAbmelden = onAbmelden;
        root.getStyleClass().add("root-pane");

        // ----- Seitenleiste -----
        VBox side = new VBox(6);
        side.getStyleClass().add("sidebar");
        side.setPadding(new Insets(18));
        side.setPrefWidth(220);

        VBox brand = new VBox(8);
        brand.setPadding(new Insets(0, 0, 18, 0));
        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
            logo.setFitWidth(184);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            StackPane card = new StackPane(logo);
            card.getStyleClass().add("logo-card");
            brand.getChildren().add(card);
        } else {
            Label name = new Label("Reinheit & Sauberkeit");
            name.getStyleClass().add("logo-name");
            brand.getChildren().add(name);
        }
        Label sub = new Label("KundenVerwaltung");
        sub.getStyleClass().add("logo-sub");
        brand.getChildren().add(sub);

        side.getChildren().add(brand);
        // Benutzer (Nicht-Admin) sehen alles außer Zahlungen.
        side.getChildren().addAll(
                navButton("📊  Dashboard",  () -> zeige(new DashboardFenster().getRoot())),
                navButton("👥  Kunden",     () -> zeige(new KundenFenster().getRoot())),
                navButton("🧑‍🔧  Mitarbeiter", () -> zeige(new MitarbeiterFenster().getRoot())),
                navButton("🧹  Einsätze",   () -> zeige(new EinsaetzeFenster().getRoot()))
        );
        // Nur Zahlungen ist ausschließlich für Administratoren sichtbar
        if (Session.istAdmin()) {
            side.getChildren().add(
                navButton("💶  Zahlungen", () -> zeige(new ZahlungenFenster().getRoot())));
        }
        side.getChildren().addAll(
                navButton("🧾  Abrechnung", () -> zeige(new AbrechnungFenster().getRoot())),
                navButton("📅  Termine",    () -> zeige(new TermineFenster().getRoot())),
                navButton("👤  Benutzer",   () -> zeige(new BenutzerverwaltungFenster().getRoot()))
        );
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        side.getChildren().add(spacer);

        // Angemeldeter Benutzer
        Benutzer b = Session.aktuellerBenutzer();
        if (b != null) {
            Label user = new Label("👤  " + b.getBenutzername() + "  (" + b.getRolle() + ")");
            user.getStyleClass().add("logo-sub");
            side.getChildren().add(user);
        }

        Button updates = navButton("🔄  Nach Updates suchen", UpdatePruefer::pruefeManuell);
        Button abmelden = navButton("🔓  Abmelden", () -> { if (onAbmelden != null) onAbmelden.run(); });
        Button beenden = navButton("⛔  Beenden", () -> System.exit(0));
        side.getChildren().addAll(updates, abmelden, beenden);

        root.setLeft(side);

        // ----- Inhalt -----
        content.getStyleClass().add("content");
        content.setPadding(new Insets(22));
        root.setCenter(content);

        // Startansicht: Dashboard
        zeige(new DashboardFenster().getRoot());

        // Im Hintergrund auf neue Version prüfen (GitHub-Release)
        UpdatePruefer.pruefeImHintergrund();
    }

    private Button navButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> action.run());
        return b;
    }

    private void zeige(javafx.scene.Node node) {
        content.getChildren().setAll(node);
    }

    public BorderPane getRoot() { return root; }
}
  