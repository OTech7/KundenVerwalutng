package de.reinheit.kundenverwaltung.model;

/** Eine Abrechnung (Rechnung an die Krankenkasse) für einen Zeitraum. */
public class Rechnung {
    private int    id;
    private int    kundennummer;
    private String zeitraumVon;     // ISO yyyy-MM-dd
    private String zeitraumBis;
    private int    anzahlGeplant;   // geplante Termine im Zeitraum
    private int    anzahlErbracht;  // tatsächlich erbrachte Einsätze im Zeitraum
    private double betrag;          // AnzahlErbracht * PreisProTermin (0 = nur Anzahl)
    private String status;          // "Abgerechnet"
    private String erstelltAm;

    public int    getId() { return id; }
    public void   setId(int id) { this.id = id; }
    public int    getKundennummer() { return kundennummer; }
    public void   setKundennummer(int k) { this.kundennummer = k; }
    public String getZeitraumVon() { return zeitraumVon; }
    public void   setZeitraumVon(String v) { this.zeitraumVon = v; }
    public String getZeitraumBis() { return zeitraumBis; }
    public void   setZeitraumBis(String v) { this.zeitraumBis = v; }
    public int    getAnzahlGeplant() { return anzahlGeplant; }
    public void   setAnzahlGeplant(int v) { this.anzahlGeplant = v; }
    public int    getAnzahlErbracht() { return anzahlErbracht; }
    public void   setAnzahlErbracht(int v) { this.anzahlErbracht = v; }
    public double getBetrag() { return betrag; }
    public void   setBetrag(double v) { this.betrag = v; }
    public String getStatus() { return status; }
    public void   setStatus(String v) { this.status = v; }
    public String getErstelltAm() { return erstelltAm; }
    public void   setErstelltAm(String v) { this.erstelltAm = v; }

    /** Anzeige "01.01.2026 – 31.03.2026" */
    public String getZeitraum() {
        return de.reinheit.kundenverwaltung.service.Datum.anzeige(zeitraumVon)
                + " – " + de.reinheit.kundenverwaltung.service.Datum.anzeige(zeitraumBis);
    }

    /** Erstellungsdatum im Anzeigeformat. */
    public String getErstelltAmAnzeige() {
        return de.reinheit.kundenverwaltung.service.Datum.anzeige(erstelltAm);
    }
}
