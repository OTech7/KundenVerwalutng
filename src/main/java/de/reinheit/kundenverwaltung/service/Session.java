package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.model.Benutzer;

/** Hält den aktuell angemeldeten Benutzer während der Sitzung. */
public final class Session {
    private static Benutzer aktuell;

    private Session() {}

    public static void anmelden(Benutzer b) { aktuell = b; }
    public static void abmelden() { aktuell = null; }
    public static Benutzer aktuellerBenutzer() { return aktuell; }
    public static boolean istAdmin() { return aktuell != null && aktuell.istAdmin(); }

    /** Benutzername des angemeldeten Benutzers (für Protokoll), sonst "System". */
    public static String benutzername() {
        return aktuell != null ? aktuell.getBenutzername() : "System";
    }

    /** Wirft eine Ausnahme, wenn der aktuelle Benutzer kein Administrator ist. */
    public static void requireAdmin() {
        if (!istAdmin()) {
            throw new SecurityException("Diese Aktion ist nur für Administratoren erlaubt.");
        }
    }
}
