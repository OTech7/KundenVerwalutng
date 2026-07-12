package de.reinheit.kundenverwaltung.model;

/** Ein Mitarbeiter – nur ein Name (kein Benutzerkonto). */
public class Mitarbeiter {
    private int    id;
    private String name;

    public Mitarbeiter() {}
    public Mitarbeiter(int id, String name) { this.id = id; this.name = name; }

    public int    getId() { return id; }
    public void   setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void   setName(String n) { this.name = n; }
}
