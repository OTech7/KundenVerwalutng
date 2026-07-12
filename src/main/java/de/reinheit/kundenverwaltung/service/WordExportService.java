package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.dao.EinsatzDao;
import de.reinheit.kundenverwaltung.dao.TerminDao;
import de.reinheit.kundenverwaltung.model.Einsatz;
import de.reinheit.kundenverwaltung.model.Kunde;
import de.reinheit.kundenverwaltung.model.Termin;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Erstellt das "Kundendatenblatt – Reinheit & Sauberkeit GmbH" als Word-Datei (.docx),
 * aufgebaut wie das Muster: Logo, Abschnitte Kundendaten, Vertragsdaten,
 * Terminplanung & Einsatzdetails, Jahresübersicht, Bestätigung & Unterschriften,
 * Hinweise für Verwaltung.
 */
public class WordExportService {

    private static final String BLAU = "0088C0";

    public void erstelleDatenblatt(Kunde k, File ziel) {
        try (XWPFDocument doc = new XWPFDocument()) {

            logo(doc);

            titel(doc, "Kundendatenblatt – Reinheit & Sauberkeit GmbH");

            // ---- Kundendaten ----
            abschnitt(doc, "Kundendaten");
            XWPFTable t1 = zweiSpaltenTabelle(doc);
            zeile(t1, "Kundenname:", n(k.getVollstaendigerName()));
            zeile(t1, "Kundennummer:", String.valueOf(k.getKundennummer()));
            zeile(t1, "Adresse:", n(k.getAdresse()));
            zeile(t1, "Geburtsdatum:", n(Datum.anzeige(k.getGeburtsdatum())));
            zeile(t1, "Telefonnummer:", n(k.getTelefon()));
            zeile(t1, "E-Mail:", n(k.getEMail()));
            zeile(t1, "Krankenkasse:", n(k.getKrankenkasseNummer()));
            zeile(t1, "Pflegegrad:", n(k.getPflegegrad()));
            zeile(t1, "Pflegegrad seit:", n(Datum.anzeige(k.getPflegegradSeit())));
            gridSetzen(t1, 2);

            // ---- Vertragsdaten ----
            abschnitt(doc, "Vertragsdaten");
            XWPFTable t2 = zweiSpaltenTabelle(doc);
            zeile(t2, "Leistungsart:", n(k.getLeistungsart()));
            zeile(t2, "Zuständiger Mitarbeiter:", n(k.getZustaendigerMitarbeiter()));
            zeile(t2, "Genehmigte Stunden:", zahl(k.getGenehmigteStunden()) + " h");
            zeile(t2, "Erbrachte Stunden:", zahl(k.getErbrachteStunden()) + " h");
            zeile(t2, "Verbleibende Stunden:", zahl(k.getVerbleibendeStunden()) + " h");
            zeile(t2, "Vertragsbeginn:", n(Datum.anzeige(k.getVertragsbeginn())));
            // Abrechnung: aktueller Zeitraum + nächste Abrechnung werden berechnet
            AbrechnungService abr = new AbrechnungService();
            AbrechnungService.Zeitraum z = abr.aktuellerZeitraum(k);
            zeile(t2, "Abrechnung alle:", k.getAbrechnungRhythmusMonate() + " Monate");
            zeile(t2, "Aktueller Abrechnungszeitraum:", Datum.anzeige(z.von) + " bis " + Datum.anzeige(z.bis));
            zeile(t2, "Termine im Zeitraum (geplant/erbracht):", z.geplant + " / " + z.erbracht);
            zeile(t2, "Nächste Abrechnung ab:", Datum.anzeige(z.bis.plusDays(1)));
            zeile(t2, "Termine gesamt (genutzt/verbleibend):",
                    abr.erbrachtGesamt(k) + " / " + abr.verbleibendGesamt(k));
            zeile(t2, "Status:", k.isIstAktiv() ? "Aktiver Kunde" : "Ehemaliger Kunde");
            gridSetzen(t2, 2);

            // ---- Terminplanung & Einsatzdetails ----
            abschnitt(doc, "Terminplanung & Einsatzdetails");
            XWPFTable t3 = zweiSpaltenTabelle(doc);
            zeile(t3, "Termin-Plan:", n(k.getTerminPlan()));
            zeile(t3, "Wochentage:", n(k.getWochentage()));
            zeile(t3, "Uhrzeit:", n(k.getUhrzeit()));
            zeile(t3, "Ort / Bereich:", n(k.getOrtBereich()));
            zeile(t3, "Hinweise / Zugang:", n(k.getZugang()));
            gridSetzen(t3, 2);

            // ---- Jahresübersicht (aus Terminen/Einsätzen berechnet) ----
            abschnitt(doc, "Jahresübersicht");
            jahresTabelle(doc, k);

            // ---- Bestätigung & Unterschriften ----
            abschnitt(doc, "Bestätigung & Unterschriften");
            unterschrift(doc, "Kunde:");
            unterschrift(doc, "Mitarbeiter:");
            unterschrift(doc, "Kontrolliert durch (Büro):");

            // ---- Hinweise für Verwaltung ----
            abschnitt(doc, "Hinweise für Verwaltung");
            absatz(doc,
                    "- Kundendaten regelmäßig prüfen und aktualisieren.\n" +
                    "- Änderungen an Terminen, Leistungen oder Kontaktdaten schriftlich dokumentieren.\n" +
                    "- Dieses Formular dient als Grundlage für Einsatzplanung, Nachweis und Abrechnung.\n" +
                    "- Original verbleibt im Büro, Kopie kann dem Kunden ausgehändigt werden.", false);

            absatz(doc, "Erstellt am " + Datum.anzeige(LocalDate.now()) + " mit KundenVerwaltung.", true);

            try (FileOutputStream out = new FileOutputStream(ziel)) {
                doc.write(out);
            }
        } catch (Exception e) {
            throw new RuntimeException("Word-Erstellung fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /**
     * Erstellt eine Rechnung an die Krankenkasse für einen Abrechnungszeitraum
     * als Word-Datei (.docx) mit Firmenbriefkopf.
     */
    public void erstelleRechnung(de.reinheit.kundenverwaltung.model.Kunde k,
                                 de.reinheit.kundenverwaltung.model.Rechnung r,
                                 File ziel) {
        try (XWPFDocument doc = new XWPFDocument()) {

            logo(doc);
            titel(doc, "Rechnung an die Krankenkasse");

            abschnitt(doc, "Rechnungsdaten");
            XWPFTable t1 = zweiSpaltenTabelle(doc);
            zeile(t1, "Rechnungsnummer:", String.valueOf(r.getId()));
            zeile(t1, "Rechnungsdatum:", n(Datum.anzeige(r.getErstelltAm())));
            zeile(t1, "Abrechnungszeitraum:",
                    Datum.anzeige(r.getZeitraumVon()) + " bis " + Datum.anzeige(r.getZeitraumBis()));
            zeile(t1, "Status:", n(r.getStatus()));
            gridSetzen(t1, 2);

            abschnitt(doc, "Kunde");
            XWPFTable t2 = zweiSpaltenTabelle(doc);
            zeile(t2, "Kundenname:", n(k.getVollstaendigerName()));
            zeile(t2, "Kundennummer:", String.valueOf(k.getKundennummer()));
            zeile(t2, "Adresse:", n(k.getAdresse()));
            zeile(t2, "Krankenkasse:", n(k.getKrankenkasseNummer()));
            zeile(t2, "Pflegegrad:", n(k.getPflegegrad()));
            zeile(t2, "Leistungsart:", n(k.getLeistungsart()));
            gridSetzen(t2, 2);

            abschnitt(doc, "Leistungsnachweis");
            XWPFTable t3 = zweiSpaltenTabelle(doc);
            zeile(t3, "Geplante Termine im Zeitraum:", String.valueOf(r.getAnzahlGeplant()));
            zeile(t3, "Erbrachte Termine (abgerechnet):", String.valueOf(r.getAnzahlErbracht()));
            zeile(t3, "Nicht wahrgenommene Termine:", String.valueOf(Math.max(0, r.getAnzahlGeplant() - r.getAnzahlErbracht())));
            if (r.getBetrag() > 0) {
                double preis = r.getAnzahlErbracht() > 0 ? r.getBetrag() / r.getAnzahlErbracht() : 0;
                zeile(t3, "Preis je Termin:", String.format("%.2f €", preis));
                zeile(t3, "Rechnungsbetrag:", String.format("%.2f €", r.getBetrag()));
            }
            gridSetzen(t3, 2);

            absatz(doc, "Es werden ausschließlich die tatsächlich erbrachten Termine "
                    + "des oben genannten Zeitraums abgerechnet.", false);

            abschnitt(doc, "Unterschrift");
            unterschrift(doc, "Reinheit & Sauberkeit GmbH:");

            absatz(doc, "Erstellt am " + Datum.anzeige(LocalDate.now()) + " mit KundenVerwaltung.", true);
            fusszeileBild(doc);

            try (FileOutputStream out = new FileOutputStream(ziel)) {
                doc.write(out);
            }
        } catch (Exception e) {
            throw new RuntimeException("Rechnung-Erstellung fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /** Firmen-Fußzeile (Kontakt/Bank) als Bild ans Dokumentende setzen. */
    private void fusszeileBild(XWPFDocument doc) {
        try (InputStream is = getClass().getResourceAsStream("/images/footer.jpg")) {
            if (is == null) return;
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.addPicture(is, Document.PICTURE_TYPE_JPEG, "footer.jpg",
                    Units.toEMU(450), Units.toEMU(78));
        } catch (Exception ignored) { }
    }

    // ----------------- Bausteine -----------------

    private void logo(XWPFDocument doc) {
        try (InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
            if (is == null) return;
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            // Volle Seitenbreite ausnutzen: A4 (595pt) minus je 1 Zoll (72pt) Rand
            // = 451pt nutzbare Breite. Seitenverhältnis 1600x492 -> Höhe 451*0.3075.
            r.addPicture(is, Document.PICTURE_TYPE_PNG, "logo.png",
                    Units.toEMU(451), Units.toEMU(139)); // 1600x492 -> volle Breite
        } catch (Exception ignored) { }
    }

    private void titel(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingAfter(180);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(16);
        r.setColor("006E9C");
    }

    private void abschnitt(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(180);
        p.setSpacingAfter(60);
        p.setBorderBottom(Borders.SINGLE);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
        r.setColor(BLAU);
    }

    private void absatz(XWPFDocument doc, String text, boolean grau) {
        XWPFParagraph p = doc.createParagraph();
        for (String line : text.split("\n")) {
            XWPFRun r = p.createRun();
            r.setText(line);
            if (grau) { r.setColor("888888"); r.setFontSize(9); }
            r.addBreak();
        }
    }

    // Verfolgt, ob die von POI automatisch erstellte erste Zeile schon benutzt wurde.
    private boolean ersteZeileFrei;

    private XWPFTable zweiSpaltenTabelle(XWPFDocument doc) {
        // Neue Tabelle hat automatisch 1 Zeile mit 1 Zelle.
        XWPFTable table = doc.createTable();
        table.setWidth("100%");
        ersteZeileFrei = true;
        return table;
    }

    /** Holt die nächste Zeile mit genau 'spalten' Zellen (POI-sicher). */
    private XWPFTableRow naechsteZeile(XWPFTable table, int spalten) {
        XWPFTableRow row;
        if (ersteZeileFrei) {
            row = table.getRow(0);          // vorhandene erste Zeile nutzen
            ersteZeileFrei = false;
        } else {
            row = table.createRow();        // neue Zeile (kopiert Zellenanzahl der ersten)
        }
        while (row.getTableCells().size() < spalten) row.addNewTableCell();
        while (row.getTableCells().size() > spalten) row.removeCell(row.getTableCells().size() - 1);
        return row;
    }

    private void zeile(XWPFTable table, String label, String wert) {
        XWPFTableRow row = naechsteZeile(table, 2);
        setzeZelle(row.getCell(0), label, true);
        setzeZelle(row.getCell(1), wert, false);
    }

    /**
     * Schreibt ein gültiges &lt;w:tblGrid&gt; mit gleich breiten Spalten.
     * Ohne dieses Element ist die Tabelle laut OOXML ungültig (Word „repariert“ sie).
     * Aufruf NACHDEM alle Zeilen erstellt wurden.
     */
    private void gridSetzen(XWPFTable table, int spalten) {
        CTTbl ctTbl = table.getCTTbl();
        CTTblGrid grid = ctTbl.getTblGrid() != null ? ctTbl.getTblGrid() : ctTbl.addNewTblGrid();
        // vorhandene Spalten entfernen
        while (grid.sizeOfGridColArray() > 0) grid.removeGridCol(0);
        int breite = 9360 / spalten;  // ~ Seitenbreite in Twips, gleichmäßig verteilt
        for (int i = 0; i < spalten; i++) {
            grid.addNewGridCol().setW(java.math.BigInteger.valueOf(breite));
        }
    }

    private void setzeZelle(XWPFTableCell cell, String text, boolean fett) {
        // Zelle hat immer mindestens einen Absatz – diesen wiederverwenden.
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        // evtl. vorhandene Runs leeren
        while (!p.getRuns().isEmpty()) p.removeRun(0);
        XWPFRun r = p.createRun();
        r.setText(text == null ? "" : text);
        r.setBold(fett);
        if (fett) r.setColor("334155");
    }

    private void jahresTabelle(XWPFDocument doc, Kunde k) {
        XWPFTable table = doc.createTable();
        table.setWidth("100%");
        ersteZeileFrei = true;

        String[] header = {"Jahr", "Geplante Termine", "Erbrachte Leistungen", "Offene Termine", "Bemerkungen"};
        XWPFTableRow hr = naechsteZeile(table, header.length);
        for (int i = 0; i < header.length; i++) {
            setzeZelle(hr.getCell(i), header[i], true);
            hr.getCell(i).setColor("E0F4FD");
        }

        int jahr = LocalDate.now().getYear();
        List<Termin> termine = new TerminDao().finde(k.getKundennummer());
        List<Einsatz> einsaetze = new EinsatzDao().findeAlle();

        for (int j = jahr - 1; j <= jahr; j++) {
            final int jj = j;
            long geplant = termine.stream().filter(t -> jahrVon(t.getTerminDatum()) == jj).count();
            long erbracht = einsaetze.stream()
                    .filter(e -> e.getKundennummer() == k.getKundennummer())
                    .filter(e -> "Erledigt".equals(e.getEinsatzstatus()))
                    .filter(e -> jahrVon(e.getEinsatzdatum()) == jj).count();
            long offen = Math.max(0, geplant - erbracht);

            XWPFTableRow row = naechsteZeile(table, header.length);
            setzeZelle(row.getCell(0), String.valueOf(j), false);
            setzeZelle(row.getCell(1), String.valueOf(geplant), false);
            setzeZelle(row.getCell(2), String.valueOf(erbracht), false);
            setzeZelle(row.getCell(3), String.valueOf(offen), false);
            setzeZelle(row.getCell(4), "–", false);
        }
        gridSetzen(table, header.length);
    }

    private void unterschrift(XWPFDocument doc, String rolle) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        XWPFRun r = p.createRun();
        r.setText(rolle + "  ______________________________     Datum: ____________");
    }

    // ----------------- Hilfen -----------------

    private int jahrVon(String iso) {
        try { return Integer.parseInt(iso.substring(0, 4)); } catch (Exception e) { return -1; }
    }
    private String n(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String zahl(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
