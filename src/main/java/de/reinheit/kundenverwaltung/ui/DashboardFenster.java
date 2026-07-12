package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.service.DashboardService;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * DashboardFenster: zeigt die 7 Statistiken aus der Spezifikation als Kacheln.
 */
public class DashboardFenster {

    private final DashboardService stats = new DashboardService();
    private final BorderPane root = new BorderPane();

    public DashboardFenster() {
        TilePane tiles = new TilePane();
        tiles.setHgap(16);
        tiles.setVgap(16);
        tiles.setPrefColumns(3);
        tiles.setPadding(new Insets(4));

        tiles.getChildren().addAll(
                kachel("Aktive Kunden", String.valueOf(stats.aktiveKunden()), "👥"),
                kachel("Ehemalige Kunden", String.valueOf(stats.ehemaligeKunden()), "📁"),
                kachel("Erledigte Einsätze", String.valueOf(stats.erledigteEinsaetze()), "✅"),
                kachel("Nicht erledigte Einsätze", String.valueOf(stats.nichtErledigteEinsaetze()), "⚠"),
                kachel("Bezahlte Zahlungen", String.valueOf(stats.bezahlteZahlungen()), "💶"),
                kachel("Gesamtstunden geleistet", fmt(stats.gesamtstundenGeleistet()) + " h", "⏱"),
                kachel("Termine im aktuellen Monat", String.valueOf(stats.termineImMonat()), "📅")
        );

        root.setCenter(tiles);
        BorderPane.setMargin(tiles, new Insets(4));
    }

    private VBox kachel(String label, String wert, String icon) {
        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size: 20px;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        Label v = new Label(wert);
        v.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #006E9C;");
        VBox box = new VBox(6, ic, l, v);
        box.setPadding(new Insets(18));
        box.setPrefSize(210, 120);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #d4e9f5; -fx-border-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(1,170,240,0.12), 8, 0, 0, 2);");
        return box;
    }

    private String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    public BorderPane getRoot() { return root; }
}