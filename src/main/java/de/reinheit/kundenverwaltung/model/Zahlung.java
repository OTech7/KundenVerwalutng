package de.reinheit.kundenverwaltung.model;

/** Datenmodell für eine Zahlung (Tabelle "Zahlungen"). */
public class Zahlung {
    private int    id;
    private int    kundennummer;
    private String zahlungsdatum;
    private double betrag;
    private String zahlungsart;     // Krankenkasse / Überweisung / Bar
    private String zahlungsstatus;  // Bezahlt / Offen / In Bearbeitung
    private String notizen;
    /** Nur für die Anzeige (nicht in der Datenbank): Name des Kunden. */
    private String kundenName;

    public int    getId() { return id; }
    public void   setId(int id) { this.id = id; }
    public int    getKundennummer() { return kundennummer; }
    public void   setKundennummer(int k) { this.kundennummer = k; }
    public String getZahlungsdatum() { return zahlungsdatum; }
    public void   setZahlungsdatum(String d) { this.zahlungsdatum = d; }
    public double getBetrag() { return betrag; }
    public void   setBetrag(double b) { this.betrag = b; }
    public String getZahlungsart() { return zahlungsart; }
    public void   setZahlungsart(String a) { this.zahlungsart = a; }
    public String getZahlungsstatus() { return zahlungsstatus; }
    public void   setZahlungsstatus(String s) { this.zahlungsstatus = s; }
    public String getNotizen() { return notizen; }
    public void   setNotizen(String n) { this.notizen = n; }
    public String getKundenName() { return kundenName; }
    public void   setKundenName(String n) { this.kundenName = n; }
}
