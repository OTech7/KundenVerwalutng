package de.reinheit.kundenverwaltung.dao;

import de.reinheit.kundenverwaltung.db.Database;
import de.reinheit.kundenverwaltung.model.Benutzer;
import de.reinheit.kundenverwaltung.service.PasswortService;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Datenzugriff für Benutzer (Registrierung + Anmeldung). */
public class BenutzerDao {

    private final PasswortService pw = new PasswortService();

    /** Prüft, ob ein Benutzername bereits vergeben ist. */
    public boolean benutzernameExistiert(String benutzername) {
        String sql = "SELECT 1 FROM Benutzer WHERE Benutzername = ? COLLATE NOCASE";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, benutzername);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            throw new RuntimeException("Benutzerprüfung fehlgeschlagen", e);
        }
    }

    /** Registriert einen neuen Benutzer mit gehashtem Passwort. */
    public void registrieren(String benutzername, String passwort, String rolle) {
        String salt = pw.neuesSalt();
        String hash = pw.hash(passwort, salt);
        String sql = """
            INSERT INTO Benutzer (Benutzername, PasswortHash, Salt, Rolle, ErstelltAm)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, benutzername);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, rolle);
            ps.setString(5, LocalDate.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Registrierung fehlgeschlagen", e);
        }
    }

    /** Meldet einen Benutzer an; gibt den Benutzer zurück oder null bei falschen Daten. */
    public Benutzer anmelden(String benutzername, String passwort) {
        String sql = "SELECT * FROM Benutzer WHERE Benutzername = ? COLLATE NOCASE";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, benutzername);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Benutzer b = mappe(rs);
                return pw.pruefen(passwort, b.getSalt(), b.getPasswortHash()) ? b : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Anmeldung fehlgeschlagen", e);
        }
    }

    public List<Benutzer> alleBenutzer() {
        List<Benutzer> list = new ArrayList<>();
        String sql = "SELECT * FROM Benutzer ORDER BY Benutzername COLLATE NOCASE";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mappe(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Benutzer laden fehlgeschlagen", e);
        }
        return list;
    }

    /**
     * Namen aller Benutzerkonten – dient als Auswahlliste für „Zuständiger
     * Mitarbeiter" (Kunden) und „Mitarbeiter" (Einsätze).
     * Es gibt bewusst keine zweite Mitarbeiterliste: Benutzer = Mitarbeiter.
     */
    public List<String> namenFuerAuswahl() {
        List<String> out = new ArrayList<>();
        String sql = "SELECT Benutzername FROM Benutzer ORDER BY Benutzername COLLATE NOCASE";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(rs.getString("Benutzername"));
        } catch (SQLException e) {
            throw new RuntimeException("Mitarbeiterliste laden fehlgeschlagen", e);
        }
        return out;
    }

    /** Setzt ein neues Passwort und löscht das Flag „muss ändern". */
    public void passwortSetzen(String benutzername, String neuesPasswort) {
        String salt = pw.neuesSalt();
        String hash = pw.hash(neuesPasswort, salt);
        String sql = "UPDATE Benutzer SET PasswortHash=?, Salt=?, MussPasswortAendern=0 WHERE Benutzername=? COLLATE NOCASE";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setString(3, benutzername);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Passwortänderung fehlgeschlagen", e);
        }
    }

    /**
     * Löscht einen Benutzer. Das eingebaute Administratorkonto „admin" ist
     * geschützt und kann nicht entfernt werden.
     */
    public void loeschen(int id) {
        String sql = "DELETE FROM Benutzer WHERE Id=? AND Benutzername <> 'admin' COLLATE NOCASE";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            if (ps.executeUpdate() == 0) {
                throw new IllegalStateException(
                        "Das Administratorkonto „admin“ kann nicht gelöscht werden.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Benutzer löschen fehlgeschlagen", e);
        }
    }

    private Benutzer mappe(ResultSet rs) throws SQLException {
        Benutzer b = new Benutzer();
        b.setId(rs.getInt("Id"));
        b.setBenutzername(rs.getString("Benutzername"));
        b.setPasswortHash(rs.getString("PasswortHash"));
        b.setSalt(rs.getString("Salt"));
        b.setRolle(rs.getString("Rolle"));
        b.setErstelltAm(rs.getString("ErstelltAm"));
        try { b.setMussPasswortAendern(rs.getInt("MussPasswortAendern") == 1); } catch (SQLException ignored) {}
        return b;
    }
}
