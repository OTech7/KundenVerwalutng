package de.reinheit.kundenverwaltung.model;

/** Datenmodell für einen automatisch erzeugten Termin (Tabelle "Termine"). */
public class Termin {
    private int    id;
    private int    kundennummer;
    private String terminDatum;   // ISO yyyy-MM-dd
    private String uhrzeit;       // z. B. "10:00"
    private double dauer = 1.5;
    private String woche;
    /** Erledigt / Abgesagt / Nicht durchgeführt – leer, solange der Termin noch offen ist. */
    private String status;
    private String notizen;
    /** Nur für die Anzeige (nicht in der Datenbank): Name des Kunden. */
    private String kundenName;

    public Termin() {}
    public Termin(int kundennummer, String terminDatum, double dauer, String woche, String notizen) {
        this.kundennummer = kundennummer;
        this.terminDatum = terminDatum;
        this.dauer = dauer;
        this.woche = woche;
        this.notizen = notizen;
    }

    public int    getId() { return id; }
    public void   setId(int id) { this.id = id; }
    public int    getKundennummer() { return kundennummer; }
    public void   setKundennummer(int k) { this.kundennummer = k; }
    public String getTerminDatum() { return terminDatum; }
    public void   setTerminDatum(String d) { this.terminDatum = d; }
    public String getUhrzeit() { return uhrzeit; }
    public void   setUhrzeit(String u) { this.uhrzeit = u; }
    public double getDauer() { return dauer; }
    public void   setDauer(double d) { this.dauer = d; }
    public String getWoche() { return woche; }
    public void   setWoche(String w) { this.woche = w; }
    public String getStatus() { return status; }
    public void   setStatus(String s) { this.status = s; }
    public String getNotizen() { return notizen; }
    public void   setNotizen(String n) { this.notizen = n; }

    public String getKundenName() { return kundenName; }
    public void   setKundenName(String n) { this.kundenName = n; }

    /** Ausgeschriebener Wochentag des Termindatums, z. B. "Mittwoch". */
    public String getWochentag() {
        return de.reinheit.kundenverwaltung.service.Datum.wochentag(terminDatum);
    }
}
