package de.reinheit.kundenverwaltung.ui;

import de.reinheit.kundenverwaltung.dao.KundeDao;
import de.reinheit.kundenverwaltung.dao.StammdatenDao;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.service.Datum;
import de.reinheit.kundenverwaltung.service.WordExportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Voll funktionsfähiges KundenFenster:
 *  - Tabelle aller Kunden
 *  - Filter: Alle / Nur aktive / Ehemalige
 *  - "Kunden hinzufügen" mit Dialog -> automatische Termin-Erstellung im DAO
 */
public class KundenFenster {

    /** Einheitlicher Hinweis auf das erwartete Datumsformat. */
    static final String DATUM_FORMAT = de.reinheit.kundenverwaltung.service.Datum.HINWEIS;

    private final KundeDao dao = new KundeDao();
    private final StammdatenDao stammdaten = new StammdatenDao();
    private final de.reinheit.kundenverwaltung.dao.MitarbeiterDao mitarbeiterDao =
            new de.reinheit.kundenverwaltung.dao.MitarbeiterDao();
    private final BorderPane root = new BorderPane();
    private final TableView<Kunde> tabelle = new TableView<>();
    private final ObservableList<Kunde> daten = FXCollections.observableArrayList();
    private String filter = "alle";

    public KundenFenster() {
        // ----- Werkzeugleiste -----
        Button hinzufuegen = new Button("＋ Kunden hinzufügen");
        hinzufuegen.getStyleClass().add("primary");
        hinzufuegen.setOnAction(e -> dialogKunde(null));

        ToggleGroup tg = new ToggleGroup();
        ToggleButton tAlle = filterButton("Alle Kunden", "alle", tg, true);
        ToggleButton tAktiv = filterButton("Nur aktive", "aktiv", tg, false);
        ToggleButton tEhem = filterButton("Ehemalige", "ehemalig", tg, false);

        Button bearbeiten = new Button("✏ Bearbeiten");
        bearbeiten.getStyleClass().add("primary");
        bearbeiten.setDisable(true);
        bearbeiten.setOnAction(e -> {
            Kunde k = tabelle.getSelectionModel().getSelectedItem();
            if (k != null) dialogKunde(k);
        });

        Button loeschen = new Button("🗑 Löschen");
        loeschen.setDisable(true);
        loeschen.setOnAction(e -> kundeLoeschen());

        Button druckBtn = new Button("🖨 Drucken");
        druckBtn.setDisable(true);
        druckBtn.setOnAction(e -> kundeDrucken());

        Button wordBtn = new Button("📄 Word-Datenblatt erstellen");
        wordBtn.setDisable(true);   // erst aktiv, wenn ein Kunde gewählt ist
        wordBtn.setOnAction(e -> datenblattErstellen());

        HBox bar = new HBox(10, hinzufuegen, bearbeiten, loeschen, new Separator(),
                tAlle, tAktiv, tEhem, new Separator(), druckBtn, wordBtn);
        bar.setPadding(new Insets(0, 0, 14, 0));

        // Buttons nur aktiv bei Auswahl
        tabelle.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {
            boolean keine = neu == null;
            wordBtn.setDisable(keine);
            druckBtn.setDisable(keine);
            bearbeiten.setDisable(keine);
            loeschen.setDisable(keine);
        });

        // Doppelklick öffnet den Kunden zum Bearbeiten
        tabelle.setRowFactory(tv -> {
            TableRow<Kunde> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) dialogKunde(row.getItem());
            });
            return row;
        });

        // ----- Tabelle -----
        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelle.getColumns().setAll(
                spalte("Nr.", "kundennummer", 50),
                spalte("Name", "vollstaendigerName", 160),
                spalte("Adresse", "adresse", 200),
                spalte("Leistungsart", "leistungsart", 110),
                spalte("Genehmigt", "genehmigteStunden", 80),
                spalte("Erbracht", "erbrachteStunden", 80),
                spalte("Verbleibend", "verbleibendeStunden", 90),
                spalte("Mitarbeiter", "zustaendigerMitarbeiter", 130),
                spalte("Plan", "terminPlan", 110)
        );
        tabelle.setItems(daten);

        root.setTop(bar);
        root.setCenter(tabelle);
        laden();
    }

    private ToggleButton filterButton(String text, String key, ToggleGroup tg, boolean sel) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(tg);
        b.setSelected(sel);
        b.setOnAction(e -> { filter = key; laden(); });
        return b;
    }

    private <T> TableColumn<Kunde, T> spalte(String titel, String prop, double breite) {
        TableColumn<Kunde, T> c = new TableColumn<>(titel);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(breite);
        return c;
    }

    private void laden() {
        daten.setAll(switch (filter) {
            case "aktiv" -> dao.findeAktive();
            case "ehemalig" -> dao.findeEhemalige();
            default -> dao.findeAlle();
        });
    }

    private void dialogKunde(Kunde bestehend) {
        boolean bearbeiten = bestehend != null;
        Dialog<Kunde> dlg = new Dialog<>();
        dlg.setTitle(bearbeiten ? "Kunde bearbeiten" : "Neuen Kunden hinzufügen");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField name = new TextField();
        TextField adresse = new TextField();
        // Krankenkasse: Auswahlliste (Name), admin-gepflegt
        ComboBox<String> kasse = new ComboBox<>(FXCollections.observableArrayList(stammdaten.liste(StammdatenDao.KRANKENKASSE)));
        ComboBox<String> leistung = new ComboBox<>(FXCollections.observableArrayList(stammdaten.liste(StammdatenDao.LEISTUNGSART)));
        leistung.getSelectionModel().selectFirst();
        TextField genehmigt = new TextField("0");
        // Mitarbeiter = Benutzerkonten; direkt hier anlegen/löschen möglich (nur Admin)
        ComboBox<String> mitarbeiter = new ComboBox<>();
        ComboBox<String> aktiv = new ComboBox<>(FXCollections.observableArrayList("Aktiver Kunde", "Ehemaliger Kunde"));
        aktiv.getSelectionModel().selectFirst();
        TextArea notizen = new TextArea();
        notizen.setPrefRowCount(2);

        // Zusätzliche Datenblatt-Felder
        TextField geburtsdatum = new TextField();
        geburtsdatum.setPromptText(DATUM_FORMAT);
        TextField telefon = new TextField();
        TextField email = new TextField();
        TextField pflegegrad = new TextField();
        TextField pflegegradSeit = new TextField();
        pflegegradSeit.setPromptText(DATUM_FORMAT);
        TextField vertragsbeginn = new TextField();
        vertragsbeginn.setPromptText(DATUM_FORMAT);
        TextField rhythmus = new TextField("3");      // Abrechnung alle N Monate
        TextField ortBereich = new TextField();
        TextField zugang = new TextField();

        if (bearbeiten) {
            name.setText(bestehend.getVollstaendigerName());
            adresse.setText(bestehend.getAdresse());
            kasse.setValue(bestehend.getKrankenkasseNummer());
            leistung.setValue(bestehend.getLeistungsart());
            genehmigt.setText(String.valueOf(bestehend.getGenehmigteStunden()));
            mitarbeiter.setValue(bestehend.getZustaendigerMitarbeiter());
            aktiv.setValue(bestehend.isIstAktiv() ? "Aktiver Kunde" : "Ehemaliger Kunde");
            notizen.setText(bestehend.getNotizen());
            geburtsdatum.setText(Datum.anzeige(bestehend.getGeburtsdatum()));
            telefon.setText(bestehend.getTelefon());
            email.setText(bestehend.getEMail());
            pflegegrad.setText(bestehend.getPflegegrad());
            pflegegradSeit.setText(Datum.anzeige(bestehend.getPflegegradSeit()));
            vertragsbeginn.setText(Datum.anzeige(bestehend.getVertragsbeginn()));
            rhythmus.setText(String.valueOf(bestehend.getAbrechnungRhythmusMonate()));
            ortBereich.setText(bestehend.getOrtBereich());
            zugang.setText(bestehend.getZugang());
        }

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(14));
        int r = 0;
        g.add(abschnitt("Kundendaten"), 0, r++, 2, 1);
        g.addRow(r++, new Label("Vollständiger Name"), name);
        g.addRow(r++, new Label("Adresse"), adresse);
        g.addRow(r++, new Label("Geburtsdatum (" + DATUM_FORMAT + ")"), geburtsdatum);
        g.addRow(r++, new Label("Telefonnummer"), telefon);
        g.addRow(r++, new Label("E-Mail"), email);
        g.addRow(r++, new Label("Krankenkasse"), mitVerwaltung(StammdatenDao.KRANKENKASSE, kasse));
        g.addRow(r++, new Label("Pflegegrad"), pflegegrad);
        g.addRow(r++, new Label("Pflegegrad seit (" + DATUM_FORMAT + ")"), pflegegradSeit);
        g.add(abschnitt("Vertrag & Leistung"), 0, r++, 2, 1);
        g.addRow(r++, new Label("Leistungsart"), mitVerwaltung(StammdatenDao.LEISTUNGSART, leistung));
        g.addRow(r++, new Label("Genehmigte Stunden"), genehmigt);
        g.addRow(r++, new Label("Zuständiger Mitarbeiter"), MitarbeiterFeld.erstelle(mitarbeiter, mitarbeiterDao));
        g.addRow(r++, new Label("Vertragsbeginn (" + DATUM_FORMAT + ")"), vertragsbeginn);
        g.addRow(r++, new Label("Abrechnung alle (Monate)"), rhythmus);
        g.add(abschnitt("Ort & Zugang"), 0, r++, 2, 1);
        g.addRow(r++, new Label("Ort / Bereich"), ortBereich);
        g.addRow(r++, new Label("Hinweise / Zugang"), zugang);
        g.add(abschnitt("Sonstiges"), 0, r++, 2, 1);
        g.addRow(r++, new Label("Status"), aktiv);
        g.addRow(r++, new Label("Notizen"), notizen);
        g.add(new Label("ℹ Termine werden separat über „Termine → Termine erzeugen“ angelegt."), 0, r, 2, 1);

        ScrollPane scroll = new ScrollPane(g);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(440);
        dlg.getDialogPane().setContent(scroll);

        // ----- Eingabeprüfung: Dialog bleibt bei Fehlern offen -----
        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String f = null;
            if (Eingabe.leer(name.getText()))        f = "Bitte den vollständigen Namen eingeben.";
            else if (leistung.getValue() == null)    f = "Bitte eine Leistungsart auswählen.";
            if (f == null) f = Eingabe.pruefeZahl(genehmigt.getText(), "Genehmigte Stunden", 0, 10_000);
            if (f == null) f = Eingabe.pruefeGanzzahl(rhythmus.getText(), "Abrechnung alle (Monate)", 1, 24);
            if (f == null) f = Eingabe.pruefeDatumOptional(geburtsdatum.getText(), "Geburtsdatum");
            if (f == null) f = Eingabe.pruefeDatumOptional(pflegegradSeit.getText(), "Pflegegrad seit");
            if (f == null) f = Eingabe.pruefeDatumOptional(vertragsbeginn.getText(), "Vertragsbeginn");
            if (f != null) { Meldung.warnung(f); ev.consume(); }
        });

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Kunde k = new Kunde();
            if (bearbeiten) {
                k.setKundennummer(bestehend.getKundennummer());
                k.setErbrachteStunden(bestehend.getErbrachteStunden());  // bereits gebuchte Stunden behalten
            } else {
                k.setErbrachteStunden(0);
            }
            k.setVollstaendigerName(name.getText());
            k.setAdresse(adresse.getText());
            k.setKrankenkasseNummer(kasse.getValue());
            k.setLeistungsart(leistung.getValue());
            k.setGenehmigteStunden(parse(genehmigt.getText()));
            k.setZustaendigerMitarbeiter(mitarbeiter.getValue());
            k.setIstAktiv(aktiv.getValue() == null || aktiv.getValue().startsWith("Aktiv"));
            k.setNotizen(notizen.getText());
            // Datumsfelder werden im ISO-Format gespeichert, angezeigt als TT.MM.JJJJ
            k.setGeburtsdatum(Datum.iso(geburtsdatum.getText()));
            k.setTelefon(telefon.getText());
            k.setEMail(email.getText());
            k.setPflegegrad(pflegegrad.getText());
            k.setPflegegradSeit(Datum.iso(pflegegradSeit.getText()));
            k.setVertragsbeginn(Datum.iso(vertragsbeginn.getText()));
            int rh;
            try { rh = Integer.parseInt(rhythmus.getText().trim()); } catch (Exception ex) { rh = 3; }
            k.setAbrechnungRhythmusMonate(rh > 0 ? rh : 3);
            k.setAbrechnungszeitraum("alle " + k.getAbrechnungRhythmusMonate() + " Monate");
            if (bearbeiten) k.setLetzteAbrechnungBis(bestehend.getLetzteAbrechnungBis());
            k.setNaechsteAbrechnung(bearbeiten ? bestehend.getNaechsteAbrechnung() : null);
            k.setOrtBereich(ortBereich.getText());
            k.setZugang(zugang.getText());
            return k;
        });

        dlg.showAndWait().ifPresent(k -> {
            if (bearbeiten) {
                dao.aktualisieren(k);
                laden();
                new Alert(Alert.AlertType.INFORMATION,
                        "Kunde „" + k.getVollstaendigerName() + "“ wurde aktualisiert.").showAndWait();
            } else {
                dao.hinzufuegen(k);
                laden();
                new Alert(Alert.AlertType.INFORMATION,
                        "Kunde „" + k.getVollstaendigerName() + "“ gespeichert.")
                        .showAndWait();
            }
        });
    }

    private double parse(String s) {
        try { return Double.parseDouble(s.trim().replace(',', '.')); }
        catch (Exception e) { return 0; }
    }

    private void kundeLoeschen() {
        Kunde k = tabelle.getSelectionModel().getSelectedItem();
        if (k == null) return;
        Alert bestaetigen = new Alert(Alert.AlertType.CONFIRMATION,
                "Kunde „" + k.getVollstaendigerName() + "“ (Nr. " + k.getKundennummer() + ") wirklich löschen?\n"
                + "Zugehörige Termine, Einsätze und Zahlungen werden ebenfalls gelöscht.",
                ButtonType.YES, ButtonType.NO);
        bestaetigen.setHeaderText("Kunde löschen");
        bestaetigen.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { dao.loeschen(k.getKundennummer()); laden(); }
        });
    }

    /**
     * Umschließt eine Auswahlliste mit einem "＋"- und einem "⚙"-Knopf:
     * neue Werte hinzufügen bzw. die Liste verwalten (Einträge löschen).
     */
    private HBox mitVerwaltung(String kategorie, ComboBox<String> combo) {
        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);
        HBox box = new HBox(6, combo);

        Button plus = new Button("＋");
        plus.setTooltip(new Tooltip("Neuen Eintrag hinzufügen"));
        plus.setOnAction(e -> {
            TextInputDialog td = new TextInputDialog();
            td.setTitle("Neuer Eintrag");
            td.setHeaderText(kategorie + " hinzufügen");
            td.setContentText("Bezeichnung:");
            td.showAndWait().ifPresent(wert -> {
                if (!wert.isBlank()) {
                    stammdaten.hinzufuegen(kategorie, wert.trim());
                    combo.setItems(FXCollections.observableArrayList(stammdaten.liste(kategorie)));
                    combo.setValue(wert.trim());
                }
            });
        });

        Button verwalten = new Button("⚙");
        verwalten.setTooltip(new Tooltip("Liste verwalten (Einträge löschen)"));
        verwalten.setOnAction(e -> listeVerwalten(kategorie, combo));

        box.getChildren().addAll(plus, verwalten);
        return box;
    }

    /** Dialog zum Verwalten einer Stammdaten-Liste: hinzufügen und löschen. */
    private void listeVerwalten(String kategorie, ComboBox<String> combo) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(kategorie + " verwalten");
        dlg.setHeaderText("Einträge hinzufügen oder löschen");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ListView<String> liste = new ListView<>(FXCollections.observableArrayList(stammdaten.liste(kategorie)));
        liste.setPrefHeight(220);

        TextField neu = new TextField();
        neu.setPromptText("Neuer Eintrag");
        Button add = new Button("Hinzufügen");
        add.getStyleClass().add("primary");
        Button del = new Button("🗑 Löschen");
        del.setDisable(true);

        liste.getSelectionModel().selectedItemProperty()
                .addListener((o, a, n) -> del.setDisable(n == null));

        Runnable neuLaden = () -> {
            liste.setItems(FXCollections.observableArrayList(stammdaten.liste(kategorie)));
            combo.setItems(FXCollections.observableArrayList(stammdaten.liste(kategorie)));
        };

        add.setOnAction(e -> {
            String w = neu.getText();
            if (w == null || w.isBlank()) { Meldung.warnung("Bitte eine Bezeichnung eingeben."); return; }
            stammdaten.hinzufuegen(kategorie, w.trim());
            neu.clear();
            neuLaden.run();
        });

        del.setOnAction(e -> {
            String w = liste.getSelectionModel().getSelectedItem();
            if (w == null) return;
            Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                    "Eintrag „" + w + "“ wirklich aus der Liste entfernen?\n"
                    + "Bereits gespeicherte Kunden behalten ihren Wert.",
                    ButtonType.YES, ButtonType.NO);
            c.setHeaderText(kategorie + " löschen");
            if (c.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                stammdaten.loeschen(kategorie, w);
                neuLaden.run();
            }
        });

        HBox zeile = new HBox(6, neu, add);
        HBox.setHgrow(neu, Priority.ALWAYS);
        VBox box = new VBox(10, liste, zeile, del);
        box.setPadding(new Insets(14));
        box.setPrefWidth(360);
        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
    }

    private Label abschnitt(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #006E9C; -fx-padding: 8 0 2 0;");
        return l;
    }

    /** Druckt das Datenblatt des ausgewählten Kunden inkl. seiner Termine mit Status. */
    private void kundeDrucken() {
        Kunde k = tabelle.getSelectionModel().getSelectedItem();
        if (k == null) return;
        javafx.stage.Window owner = root.getScene() != null ? root.getScene().getWindow() : null;
        String pdfName = "Kundendatenblatt_" + k.getKundennummer() + "_"
                + (k.getVollstaendigerName() == null ? "Kunde"
                   : k.getVollstaendigerName().replaceAll("[^\\p{L}0-9]+", "_")) + ".pdf";
        Drucker.vorschau(owner, "Kundendatenblatt – " + k.getVollstaendigerName(), pdfName,
                (ziel, breite) -> kundenblatt(ziel, breite, k));
    }

    /** Baut das Kundendatenblatt (alle Felder + Termine mit Status) für den Druck. */
    private void kundenblatt(VBox ziel, double breite, Kunde k) {
        // Seitlicher Innenabstand für Textinhalte; das Logo bleibt volle Breite.
        double seit = 24;
        double innen = breite - 2 * seit;

        // Logo (volle Breite, bis an den Seitenrand)
        var logoUrl = getClass().getResource("/images/logo.png");
        if (logoUrl != null) {
            javafx.scene.image.ImageView logo =
                    new javafx.scene.image.ImageView(new javafx.scene.image.Image(logoUrl.toExternalForm()));
            logo.setFitWidth(breite);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            VBox kopf = new VBox(logo);
            kopf.setPadding(new Insets(0, 0, 10, 0));
            ziel.getChildren().add(kopf);
        }

        // Titel
        Label titel = new Label("Kundendatenblatt – " + druckWert(k.getVollstaendigerName()));
        titel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #006E9C; "
                + "-fx-border-color: transparent transparent #01AAF0 transparent; -fx-border-width: 0 0 2 0; "
                + "-fx-padding: 0 0 8 0;");
        ziel.getChildren().add(druckBlock(titel, seit));

        // ---- Kundendaten: jede Zeile ein eigener Block (saubere Seitenumbrüche) ----
        ziel.getChildren().add(druckBlock(abschnitt("Kundendaten"), seit));
        String[][] felder = {
                {"Kundennummer", String.valueOf(k.getKundennummer())},
                {"Name", druckWert(k.getVollstaendigerName())},
                {"Adresse", druckWert(k.getAdresse())},
                {"Geburtsdatum", Datum.anzeige(k.getGeburtsdatum())},
                {"Telefon", druckWert(k.getTelefon())},
                {"E-Mail", druckWert(k.getEMail())},
                {"Krankenkasse", druckWert(k.getKrankenkasseNummer())},
                {"Pflegegrad", druckWert(k.getPflegegrad())},
                {"Pflegegrad seit", Datum.anzeige(k.getPflegegradSeit())},
                {"Leistungsart", druckWert(k.getLeistungsart())},
                {"Zuständiger Mitarbeiter", druckWert(k.getZustaendigerMitarbeiter())},
                {"Genehmigte Stunden", zahl(k.getGenehmigteStunden()) + " h"},
                {"Erbrachte Stunden", zahl(k.getErbrachteStunden()) + " h"},
                {"Verbleibende Stunden", zahl(k.getVerbleibendeStunden()) + " h"},
                {"Vertragsbeginn", Datum.anzeige(k.getVertragsbeginn())},
                {"Abrechnung alle", k.getAbrechnungRhythmusMonate() + " Monate"},
                {"Termin-Plan", druckWert(k.getTerminPlan())},
                {"Wochentage", druckWert(k.getWochentage())},
                {"Uhrzeit", druckWert(k.getUhrzeit())},
                {"Ort / Bereich", druckWert(k.getOrtBereich())},
                {"Hinweise / Zugang", druckWert(k.getZugang())},
                {"Status", k.isIstAktiv() ? "Aktiver Kunde" : "Ehemaliger Kunde"},
                {"Notizen", druckWert(k.getNotizen())},
        };
        for (String[] f : felder) ziel.getChildren().add(infoBlock(innen, seit, f[0], f[1]));

        // ---- Termine des Kunden mit Status ----
        ziel.getChildren().add(druckBlock(abschnitt("Termine"), seit));
        double[] tProz = {20, 22, 18, 15, 25};
        ziel.getChildren().add(zeileBlock(innen, seit, tProz,
                new String[]{"Datum", "Wochentag", "Woche", "Dauer", "Status"}, true));
        var termine = new de.reinheit.kundenverwaltung.dao.TerminDao().finde(k.getKundennummer());
        for (var termin : termine) {
            ziel.getChildren().add(zeileBlock(innen, seit, tProz, new String[]{
                    Datum.anzeige(termin.getTerminDatum()),
                    termin.getWochentag(),
                    druckWert(termin.getWoche()),
                    termin.getDauer() + " h",
                    termin.getStatus() == null ? "" : termin.getStatus()}, false));
        }
        if (termine.isEmpty())
            ziel.getChildren().add(druckBlock(hinweisLabel("Keine Termine vorhanden."), seit));

        // ---- Zahlungen des Kunden (nur für Administratoren) ----
        if (de.reinheit.kundenverwaltung.service.Session.istAdmin()) {
            ziel.getChildren().add(druckBlock(abschnitt("Zahlungen"), seit));
            double[] zProz = {25, 25, 25, 25};
            ziel.getChildren().add(zeileBlock(innen, seit, zProz,
                    new String[]{"Quartal", "Betrag", "Zahlungsart", "Status"}, true));
            var zahlungen = new de.reinheit.kundenverwaltung.dao.ZahlungDao().findeFuerKunde(k.getKundennummer());
            for (var z : zahlungen) {
                ziel.getChildren().add(zeileBlock(innen, seit, zProz, new String[]{
                        druckWert(z.getZahlungsdatum()),
                        String.format("%.2f €", z.getBetrag()),
                        druckWert(z.getZahlungsart()),
                        druckWert(z.getZahlungsstatus())}, false));
            }
            if (zahlungen.isEmpty())
                ziel.getChildren().add(druckBlock(hinweisLabel("Keine Zahlungen vorhanden."), seit));
        }

        // ---- Abschluss-Zeile ----
        Label fuss = new Label("— Erstellt mit KundenVerwaltung · " + Datum.anzeige(java.time.LocalDate.now()) + " —");
        fuss.setStyle("-fx-text-fill: #94a3b8;");
        fuss.setPadding(new Insets(16, 0, 6, 0));
        ziel.getChildren().add(druckBlock(fuss, seit));

        // ---- Firmen-Fußzeile (Kontakt/Bank) als Bild, volle Breite ----
        var footerUrl = getClass().getResource("/images/footer.jpg");
        if (footerUrl != null) {
            javafx.scene.image.ImageView footer =
                    new javafx.scene.image.ImageView(new javafx.scene.image.Image(footerUrl.toExternalForm()));
            footer.setFitWidth(breite);
            footer.setPreserveRatio(true);
            footer.setSmooth(true);
            ziel.getChildren().add(new VBox(footer));
        }
    }

    /** Wrappt einen Inhalt in einen seitlich eingerückten Block. */
    private VBox druckBlock(javafx.scene.Node inhalt, double seit) {
        VBox w = new VBox(inhalt);
        w.setPadding(new Insets(0, seit, 0, seit));
        return w;
    }

    private Label hinweisLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8;");
        return l;
    }

    /** Info-Zeile „Label : Wert" als eigener Block. */
    private VBox infoBlock(double innen, double seit, String label, String wert) {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setPrefWidth(innen); g.setMaxWidth(innen);
        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints(); c1.setPercentWidth(32);
        javafx.scene.layout.ColumnConstraints c2 = new javafx.scene.layout.ColumnConstraints(); c2.setPercentWidth(68);
        g.getColumnConstraints().addAll(c1, c2);
        Label l = new Label(label + ":");
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
        Label w = new Label(wert == null ? "" : wert);
        w.setWrapText(true);
        g.add(l, 0, 0); g.add(w, 1, 0);
        VBox blk = new VBox(g);
        blk.setPadding(new Insets(1, seit, 1, seit));
        return blk;
    }

    /** Tabellenzeile mit festen Spaltenanteilen als eigener Block. */
    private VBox zeileBlock(double innen, double seit, double[] proz, String[] werte, boolean kopf) {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setPrefWidth(innen); g.setMaxWidth(innen);
        for (double p : proz) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(p);
            g.getColumnConstraints().add(cc);
        }
        for (int c = 0; c < werte.length; c++) {
            Label l = new Label(werte[c] == null ? "" : werte[c]);
            l.setWrapText(true);
            if (kopf) l.setStyle("-fx-font-weight: bold; -fx-text-fill: #006E9C;");
            g.add(l, c, 0);
        }
        VBox blk = new VBox(g);
        blk.setPadding(new Insets(1, seit, 1, seit));
        return blk;
    }


    private String druckWert(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String zahl(double d) { return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d); }

    /** Erstellt für den ausgewählten Kunden ein Word-Datenblatt (.docx). */
    private void datenblattErstellen() {
        Kunde k = tabelle.getSelectionModel().getSelectedItem();
        if (k == null) return;

        String vorschlag = "Kundendatenblatt_" + k.getKundennummer() + "_"
                + (k.getVollstaendigerName() == null ? "Kunde" : k.getVollstaendigerName().replaceAll("[^\\p{L}0-9]+", "_"))
                + ".docx";

        FileChooser fc = new FileChooser();
        fc.setTitle("Word-Datenblatt speichern");
        fc.setInitialFileName(vorschlag);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word-Dokument (*.docx)", "*.docx"));

        File ziel = fc.showSaveDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (ziel == null) return;

        try {
            new WordExportService().erstelleDatenblatt(k, ziel);
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Datenblatt gespeichert:\n" + ziel.getAbsolutePath());
            a.setHeaderText("Word-Datei erstellt");
            // Datei direkt öffnen (falls möglich)
            if (java.awt.Desktop.isDesktopSupported()) {
                try { java.awt.Desktop.getDesktop().open(ziel); } catch (Exception ignored) { }
            }
            a.showAndWait();
        } catch (Exception ex) {
            System.err.println("Word-Erstellung fehlgeschlagen: " + ex);   // Details ins Log
            new Alert(Alert.AlertType.ERROR,
                    "Das Word-Datenblatt konnte nicht erstellt werden. Bitte erneut versuchen.").showAndWait();
        }
    }

    public BorderPane getRoot() { return root; }
}
