package de.reinheit.kundenverwaltung.model;

/** Datenmodell für einen Benutzer (Tabelle "Benutzer"). */
public class Benutzer {
    private int    id;
    private String benutzername;
    private String passwortHash;
    private String salt;
    private String rolle;        // "Admin" oder "Mitarbeiter"
    private String erstelltAm;
    private boolean mussPasswortAendern;

    public int    getId() { return id; }
    public void   setId(int id) { this.id = id; }
    public String getBenutzername() { return benutzername; }
    public void   setBenutzername(String b) { this.benutzername = b; }
    public String getPasswortHash() { return passwortHash; }
    public void   setPasswortHash(String h) { this.passwortHash = h; }
    public String getSalt() { return salt; }
    public void   setSalt(String s) { this.salt = s; }
    public String getRolle() { return rolle; }
    public void   setRolle(String r) { this.rolle = r; }
    public String getErstelltAm() { return erstelltAm; }
    public void   setErstelltAm(String e) { this.erstelltAm = e; }
    public boolean isMussPasswortAendern() { return mussPasswortAendern; }
    public void   setMussPasswortAendern(boolean v) { this.mussPasswortAendern = v; }

    public boolean istAdmin() { return "Admin".equals(rolle); }
}
