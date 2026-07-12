package de.reinheit.kundenverwaltung.model;

/** Datenmodell für einen Einsatz (Tabelle "Einsaetze"). */
public class Einsatz {
    private int    id;
    private int    kundennummer;
    private String einsatzdatum;
    private String mitarbeiter;
    private double einsatzdauerStunden = 1.5;
    private String leistungsart;
    private String einsatzstatus;   // Erledigt / Abgesagt / Nicht durchgeführt
    private String notizen;
    /** Nur für die Anzeige (nicht in der Datenbank): Name des Kunden. */
    private String kundenName;

    public int    getId() { return id; }
    public void   setId(int id) { this.id = id; }
    public int    getKundennummer() { return kundennummer; }
    public void   setKundennummer(int k) { this.kundennummer = k; }
    public String getEinsatzdatum() { return einsatzdatum; }
    public void   setEinsatzdatum(String d) { this.einsatzdatum = d; }
    public String getMitarbeiter() { return mitarbeiter; }
    public void   setMitarbeiter(String m) { this.mitarbeiter = m; }
    public double getEinsatzdauerStunden() { return einsatzdauerStunden; }
    public void   setEinsatzdauerStunden(double d) { this.einsatzdauerStunden = d; }
    public String getLeistungsart() { return leistungsart; }
    public void   setLeistungsart(String l) { this.leistungsart = l; }
    public String getEinsatzstatus() { return einsatzstatus; }
    public void   setEinsatzstatus(String s) { this.einsatzstatus = s; }
    public String getNotizen() { return notizen; }
    public void   setNotizen(String n) { this.notizen = n; }
    public String getKundenName() { return kundenName; }
    public void   setKundenName(String n) { this.kundenName = n; }
}
