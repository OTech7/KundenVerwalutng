package de.reinheit.kundenverwaltung.service;

import de.reinheit.kundenverwaltung.db.Database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Schreibt Änderungen (Anlegen/Bearbeiten/Löschen) in die Tabelle
 * "Aenderungsprotokoll" – für Nachvollziehbarkeit und Abrechnung.
 */
public final class AuditService {

    private AuditService() {}

    public static void log(String aktion, String bereich, String datensatz) {
        String sql = "INSERT INTO Aenderungsprotokoll (Zeitpunkt, Benutzer, Aktion, Bereich, Datensatz) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, Session.benutzername());
            ps.setString(3, aktion);
            ps.setString(4, bereich);
            ps.setString(5, datensatz);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Protokollfehler dürfen den eigentlichen Vorgang nicht abbrechen
            System.err.println("Audit-Log fehlgeschlagen: " + e.getMessage());
        }
    }
}
