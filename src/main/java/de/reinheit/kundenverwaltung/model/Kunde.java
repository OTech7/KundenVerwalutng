package de.reinheit.kundenverwaltung.model;

import javafx.beans.property.*;

/** Datenmodell für einen Kunden (Tabelle "Kunden"). */
public class Kunde {
    private final IntegerProperty kundennummer = new SimpleIntegerProperty();
    private final StringProperty  vollstaendigerName = new SimpleStringProperty();
    private final StringProperty  adresse = new SimpleStringProperty();
    private final StringProperty  krankenkasseNummer = new SimpleStringProperty();
    private final StringProperty  leistungsart = new SimpleStringProperty();
    private final DoubleProperty  genehmigteStunden = new SimpleDoubleProperty();
    private final DoubleProperty  erbrachteStunden = new SimpleDoubleProperty();
    private final DoubleProperty  verbleibendeStunden = new SimpleDoubleProperty();
    private final StringProperty  zustaendigerMitarbeiter = new SimpleStringProperty();
    private final StringProperty  notizen = new SimpleStringProperty();
    private final BooleanProperty istAktiv = new SimpleBooleanProperty(true);
    private final StringProperty  terminPlan = new SimpleStringProperty();

    // Zusätzliche Felder für das Word-Datenblatt (Muster)
    private String geburtsdatum;
    private String telefon;
    private String eMail;
    private String versicherungsnummer;
    private String pflegegrad;
    private String pflegegradSeit;
    private String vertragsbeginn;
    private String abrechnungszeitraum;
    private String naechsteAbrechnung;
    private String wochentage;
    private String uhrzeit;
    private String ortBereich;
    private String zugang;
    private double standardTerminDauer = 1.5;
    // Abrechnung
    private int    abrechnungRhythmusMonate = 3;   // z. B. alle 3 Monate abrechnen
    private String letzteAbrechnungBis;            // Enddatum der letzten abgerechneten Periode
    private double preisProTermin = 0;             // optional: Preis je Termin für den Rechnungsbetrag

    public int    getKundennummer() { return kundennummer.get(); }
    public void   setKundennummer(int v) { kundennummer.set(v); }
    public IntegerProperty kundennummerProperty() { return kundennummer; }

    public String getVollstaendigerName() { return vollstaendigerName.get(); }
    public void   setVollstaendigerName(String v) { vollstaendigerName.set(v); }
    public StringProperty vollstaendigerNameProperty() { return vollstaendigerName; }

    public String getAdresse() { return adresse.get(); }
    public void   setAdresse(String v) { adresse.set(v); }
    public StringProperty adresseProperty() { return adresse; }

    public String getKrankenkasseNummer() { return krankenkasseNummer.get(); }
    public void   setKrankenkasseNummer(String v) { krankenkasseNummer.set(v); }
    public StringProperty krankenkasseNummerProperty() { return krankenkasseNummer; }

    public String getLeistungsart() { return leistungsart.get(); }
    public void   setLeistungsart(String v) { leistungsart.set(v); }
    public StringProperty leistungsartProperty() { return leistungsart; }

    public double getGenehmigteStunden() { return genehmigteStunden.get(); }
    public void   setGenehmigteStunden(double v) { genehmigteStunden.set(v); }
    public DoubleProperty genehmigteStundenProperty() { return genehmigteStunden; }

    public double getErbrachteStunden() { return erbrachteStunden.get(); }
    public void   setErbrachteStunden(double v) { erbrachteStunden.set(v); }
    public DoubleProperty erbrachteStundenProperty() { return erbrachteStunden; }

    public double getVerbleibendeStunden() { return verbleibendeStunden.get(); }
    public void   setVerbleibendeStunden(double v) { verbleibendeStunden.set(v); }
    public DoubleProperty verbleibendeStundenProperty() { return verbleibendeStunden; }

    public String getZustaendigerMitarbeiter() { return zustaendigerMitarbeiter.get(); }
    public void   setZustaendigerMitarbeiter(String v) { zustaendigerMitarbeiter.set(v); }
    public StringProperty zustaendigerMitarbeiterProperty() { return zustaendigerMitarbeiter; }

    public String getNotizen() { return notizen.get(); }
    public void   setNotizen(String v) { notizen.set(v); }
    public StringProperty notizenProperty() { return notizen; }

    public boolean isIstAktiv() { return istAktiv.get(); }
    public void    setIstAktiv(boolean v) { istAktiv.set(v); }
    public BooleanProperty istAktivProperty() { return istAktiv; }

    public String getTerminPlan() { return terminPlan.get(); }
    public void   setTerminPlan(String v) { terminPlan.set(v); }
    public StringProperty terminPlanProperty() { return terminPlan; }

    // --- Zusätzliche Datenblatt-Felder ---
    public String getGeburtsdatum() { return geburtsdatum; }
    public void   setGeburtsdatum(String v) { this.geburtsdatum = v; }
    public String getTelefon() { return telefon; }
    public void   setTelefon(String v) { this.telefon = v; }
    public String getEMail() { return eMail; }
    public void   setEMail(String v) { this.eMail = v; }
    public String getVersicherungsnummer() { return versicherungsnummer; }
    public void   setVersicherungsnummer(String v) { this.versicherungsnummer = v; }
    public String getPflegegrad() { return pflegegrad; }
    public void   setPflegegrad(String v) { this.pflegegrad = v; }
    public String getPflegegradSeit() { return pflegegradSeit; }
    public void   setPflegegradSeit(String v) { this.pflegegradSeit = v; }
    public String getVertragsbeginn() { return vertragsbeginn; }
    public void   setVertragsbeginn(String v) { this.vertragsbeginn = v; }
    public String getAbrechnungszeitraum() { return abrechnungszeitraum; }
    public void   setAbrechnungszeitraum(String v) { this.abrechnungszeitraum = v; }
    public String getNaechsteAbrechnung() { return naechsteAbrechnung; }
    public void   setNaechsteAbrechnung(String v) { this.naechsteAbrechnung = v; }
    public String getWochentage() { return wochentage; }
    public void   setWochentage(String v) { this.wochentage = v; }
    public String getUhrzeit() { return uhrzeit; }
    public void   setUhrzeit(String v) { this.uhrzeit = v; }
    public String getOrtBereich() { return ortBereich; }
    public void   setOrtBereich(String v) { this.ortBereich = v; }
    public String getZugang() { return zugang; }
    public void   setZugang(String v) { this.zugang = v; }
    public double getStandardTerminDauer() { return standardTerminDauer; }
    public void   setStandardTerminDauer(double v) { this.standardTerminDauer = v; }

    public int    getAbrechnungRhythmusMonate() { return abrechnungRhythmusMonate; }
    public void   setAbrechnungRhythmusMonate(int v) { this.abrechnungRhythmusMonate = v; }
    public String getLetzteAbrechnungBis() { return letzteAbrechnungBis; }
    public void   setLetzteAbrechnungBis(String v) { this.letzteAbrechnungBis = v; }
    public double getPreisProTermin() { return preisProTermin; }
    public void   setPreisProTermin(double v) { this.preisProTermin = v; }
}
