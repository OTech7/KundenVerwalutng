package de.reinheit.kundenverwaltung.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Verwaltet die SQLite-Verbindung und legt das Schema beim ersten Start an.
 * Die Datenbankdatei "kunden.db" liegt im Benutzer-Datenverzeichnis
 * (siehe {@link AppDaten}), damit die Anwendung auch aus einem
 * schreibgeschützten Installationsordner heraus funktioniert.
 */
public final class Database {

    public static final String DB_DATEI = "kunden.db";
    private static Connection connection;

    private Database() {}

    /** JDBC-URL auf die Datenbank im Benutzer-Datenverzeichnis. */
    private static String url() {
        // Vorwärts-Schrägstriche sind für SQLite auch unter Windows sicher
        String pfad = AppDaten.datei(DB_DATEI).toString().replace('\\', '/');
        return "jdbc:sqlite:" + pfad;
    }

    public static Connection get() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url());
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON;");
                }
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Datenbankverbindung fehlgeschlagen", e);
        }
    }

    /** Legt alle Tabellen an, falls sie noch nicht existieren. */
    public static void init() {
        // Alte Dateien aus dem Arbeitsverzeichnis übernehmen – muss VOR dem
        // ersten Verbindungsaufbau geschehen, sonst wird eine leere DB angelegt.
        AppDaten.migriereAusArbeitsverzeichnis(DB_DATEI);
        AppDaten.migriereAusArbeitsverzeichnis("kunden.key");

        String[] ddl = {
            """
            CREATE TABLE IF NOT EXISTS Kunden (
                Kundennummer          INTEGER PRIMARY KEY AUTOINCREMENT,
                VollstaendigerName    TEXT    NOT NULL,
                Adresse               TEXT,
                KrankenkasseNummer    TEXT,
                Leistungsart          TEXT,
                GenehmigteStunden     REAL    DEFAULT 0,
                ErbrachteStunden      REAL    DEFAULT 0,
                VerbleibendeStunden   REAL    DEFAULT 0,
                ZustaendigerMitarbeiter TEXT,
                Notizen               TEXT,
                IstAktiv              INTEGER DEFAULT 1,
                TerminPlan            TEXT,
                Geburtsdatum          TEXT,
                Telefon               TEXT,
                EMail                 TEXT,
                Pflegegrad            TEXT,
                PflegegradSeit        TEXT,
                Vertragsbeginn        TEXT,
                Abrechnungszeitraum   TEXT,
                NaechsteAbrechnung    TEXT,
                Wochentage            TEXT,
                Uhrzeit               TEXT,
                OrtBereich            TEXT,
                Zugang                TEXT
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Termine (
                Id           INTEGER PRIMARY KEY AUTOINCREMENT,
                Kundennummer INTEGER NOT NULL,
                TerminDatum  TEXT    NOT NULL,
                Dauer        REAL    DEFAULT 1.5,
                Woche        TEXT,
                Status       TEXT,
                Notizen      TEXT,
                FOREIGN KEY (Kundennummer) REFERENCES Kunden(Kundennummer) ON DELETE CASCADE
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Einsaetze (
                Id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                Kundennummer        INTEGER NOT NULL,
                Einsatzdatum        TEXT    NOT NULL,
                Mitarbeiter         TEXT,
                EinsatzdauerStunden REAL    DEFAULT 1.5,
                Leistungsart        TEXT,
                Einsatzstatus       TEXT,
                Notizen             TEXT,
                FOREIGN KEY (Kundennummer) REFERENCES Kunden(Kundennummer) ON DELETE CASCADE
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Zahlungen (
                Id             INTEGER PRIMARY KEY AUTOINCREMENT,
                Kundennummer   INTEGER NOT NULL,
                Zahlungsdatum  TEXT    NOT NULL,
                Betrag         REAL    DEFAULT 0,
                Zahlungsart    TEXT,
                Zahlungsstatus TEXT,
                Notizen        TEXT,
                FOREIGN KEY (Kundennummer) REFERENCES Kunden(Kundennummer) ON DELETE CASCADE
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Benutzer (
                Id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                Benutzername       TEXT    NOT NULL UNIQUE,
                PasswortHash       TEXT    NOT NULL,
                Salt               TEXT    NOT NULL,
                Rolle              TEXT    NOT NULL DEFAULT 'Mitarbeiter',
                ErstelltAm         TEXT,
                MussPasswortAendern INTEGER DEFAULT 0
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Aenderungsprotokoll (
                Id         INTEGER PRIMARY KEY AUTOINCREMENT,
                Zeitpunkt  TEXT NOT NULL,
                Benutzer   TEXT,
                Aktion     TEXT,
                Bereich    TEXT,
                Datensatz  TEXT
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Stammdaten (
                Id        INTEGER PRIMARY KEY AUTOINCREMENT,
                Kategorie TEXT NOT NULL,
                Wert      TEXT NOT NULL,
                UNIQUE(Kategorie, Wert)
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Mitarbeiter (
                Id   INTEGER PRIMARY KEY AUTOINCREMENT,
                Name TEXT NOT NULL UNIQUE
            );
            """,
            """
            CREATE TABLE IF NOT EXISTS Rechnungen (
                Id             INTEGER PRIMARY KEY AUTOINCREMENT,
                Kundennummer   INTEGER NOT NULL,
                ZeitraumVon    TEXT NOT NULL,
                ZeitraumBis    TEXT NOT NULL,
                AnzahlGeplant  INTEGER DEFAULT 0,
                AnzahlErbracht INTEGER DEFAULT 0,
                Betrag         REAL    DEFAULT 0,
                Status         TEXT    DEFAULT 'Abgerechnet',
                ErstelltAm     TEXT,
                FOREIGN KEY (Kundennummer) REFERENCES Kunden(Kundennummer) ON DELETE CASCADE
            );
            """
        };
        try (Statement st = get().createStatement()) {
            for (String sql : ddl) st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Schema-Erstellung fehlgeschlagen", e);
        }
        migriereKunden();   // fehlende Spalten in bestehender DB ergänzen
        migriereBenutzer(); // MussPasswortAendern in bestehender DB ergänzen
        migriereTermine();  // Status in bestehender DB ergänzen
        seedIfEmpty();
        seedAdminIfEmpty();
        seedStammdatenIfEmpty();
    }

    /** Legt Standard-Listen an (nur wenn Kategorie noch leer). */
    private static void seedStammdatenIfEmpty() {
        seedKategorie("Leistungsart", "Grundpflege", "Hauswirtschaft", "Betreuung");
        seedKategorie("Krankenkasse", "AOK", "Techniker Krankenkasse", "Barmer", "DAK-Gesundheit", "Privat");

        // Frühere Mitarbeiterliste aus den Stammdaten wird nicht mehr verwendet.
        try (Statement st = get().createStatement()) {
            st.execute("DELETE FROM Stammdaten WHERE Kategorie = 'Mitarbeiter'");
        } catch (SQLException e) {
            throw new RuntimeException("Alte Mitarbeiterliste konnte nicht entfernt werden", e);
        }

        // Mitarbeiter (nur Namen) beim ersten Start vorbelegen.
        try (Statement st = get().createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM Mitarbeiter")) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (var ps = get().prepareStatement("INSERT OR IGNORE INTO Mitarbeiter (Name) VALUES (?)")) {
                    for (String n : new String[]{"Anna Becker", "Tobias Klein", "Maria Hoffmann", "Jonas Wagner"}) {
                        ps.setString(1, n); ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Mitarbeiter-Seed fehlgeschlagen", e);
        }

        // Rolle „Mitarbeiter" heißt jetzt „Benutzer".
        try (Statement st = get().createStatement()) {
            st.execute("UPDATE Benutzer SET Rolle='Benutzer' WHERE Rolle='Mitarbeiter'");
        } catch (SQLException e) {
            throw new RuntimeException("Rollen-Migration fehlgeschlagen", e);
        }
    }

    private static void seedKategorie(String kategorie, String... werte) {
        try (var zaehl = get().prepareStatement("SELECT COUNT(*) FROM Stammdaten WHERE Kategorie=?")) {
            zaehl.setString(1, kategorie);
            boolean leer;
            try (var rs = zaehl.executeQuery()) {
                leer = rs.next() && rs.getInt(1) == 0;
            }
            if (leer) {
                try (var ps = get().prepareStatement("INSERT OR IGNORE INTO Stammdaten (Kategorie, Wert) VALUES (?,?)")) {
                    for (String w : werte) { ps.setString(1, kategorie); ps.setString(2, w); ps.addBatch(); }
                    ps.executeBatch();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Stammdaten-Seed fehlgeschlagen", e);
        }
    }

    /** Ergänzt die Status-Spalte in einer bestehenden Termine-Tabelle. */
    private static void migriereTermine() {
        java.util.Set<String> vorhanden = new java.util.HashSet<>();
        try (Statement st = get().createStatement();
             var rs = st.executeQuery("PRAGMA table_info(Termine)")) {
            while (rs.next()) vorhanden.add(rs.getString("name"));
        } catch (SQLException e) {
            throw new RuntimeException("Termine-Schema-Prüfung fehlgeschlagen", e);
        }
        if (!vorhanden.contains("Status")) {
            try (Statement st = get().createStatement()) {
                st.execute("ALTER TABLE Termine ADD COLUMN Status TEXT");
            } catch (SQLException e) {
                throw new RuntimeException("Spalte Status konnte nicht ergänzt werden", e);
            }
        }
    }

    /** Ergänzt fehlende Spalten in einer bestehenden Benutzer-Tabelle. */
    private static void migriereBenutzer() {
        java.util.Set<String> vorhanden = new java.util.HashSet<>();
        try (Statement st = get().createStatement();
             var rs = st.executeQuery("PRAGMA table_info(Benutzer)")) {
            while (rs.next()) vorhanden.add(rs.getString("name"));
        } catch (SQLException e) {
            throw new RuntimeException("Benutzer-Schema-Prüfung fehlgeschlagen", e);
        }
        if (!vorhanden.contains("MussPasswortAendern")) {
            try (Statement st = get().createStatement()) {
                st.execute("ALTER TABLE Benutzer ADD COLUMN MussPasswortAendern INTEGER DEFAULT 0");
            } catch (SQLException e) {
                throw new RuntimeException("Spalte MussPasswortAendern konnte nicht ergänzt werden", e);
            }
        }
    }

    /**
     * Ergänzt in einer bereits vorhandenen Kunden-Tabelle fehlende Spalten
     * (z. B. nach einem Update). So gehen bestehende Daten nicht verloren.
     */
    private static void migriereKunden() {
        String[][] spalten = {
            {"Geburtsdatum", "TEXT"}, {"Telefon", "TEXT"}, {"EMail", "TEXT"},
            {"Pflegegrad", "TEXT"}, {"PflegegradSeit", "TEXT"},
            {"Vertragsbeginn", "TEXT"}, {"Abrechnungszeitraum", "TEXT"},
            {"NaechsteAbrechnung", "TEXT"}, {"Wochentage", "TEXT"},
            {"Uhrzeit", "TEXT"}, {"OrtBereich", "TEXT"}, {"Zugang", "TEXT"},
            {"StandardTerminDauer", "REAL DEFAULT 1.5"},
            {"AbrechnungRhythmusMonate", "INTEGER DEFAULT 3"},
            {"LetzteAbrechnungBis", "TEXT"},
            {"PreisProTermin", "REAL DEFAULT 0"}
        };
        java.util.Set<String> vorhanden = new java.util.HashSet<>();
        try (Statement st = get().createStatement();
             var rs = st.executeQuery("PRAGMA table_info(Kunden)")) {
            while (rs.next()) vorhanden.add(rs.getString("name"));
        } catch (SQLException e) {
            throw new RuntimeException("Schema-Prüfung fehlgeschlagen", e);
        }
        for (String[] sp : spalten) {
            if (!vorhanden.contains(sp[0])) {
                try (Statement st = get().createStatement()) {
                    st.execute("ALTER TABLE Kunden ADD COLUMN " + sp[0] + " " + sp[1]);
                } catch (SQLException e) {
                    throw new RuntimeException("Spalte " + sp[0] + " konnte nicht ergänzt werden", e);
                }
            }
        }
    }

    /** Legt beim ersten Start einen Standard-Admin an: admin / admin123. */
    private static void seedAdminIfEmpty() {
        try (Statement st = get().createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) AS c FROM Benutzer")) {
            if (rs.next() && rs.getInt("c") == 0) {
                var pw = new de.reinheit.kundenverwaltung.service.PasswortService();
                String salt = pw.neuesSalt();
                String hash = pw.hash("admin123", salt);
                String sql = "INSERT INTO Benutzer (Benutzername, PasswortHash, Salt, Rolle, ErstelltAm, MussPasswortAendern) VALUES (?,?,?,?,?,1)";
                try (var ps = get().prepareStatement(sql)) {
                    ps.setString(1, "admin");
                    ps.setString(2, hash);
                    ps.setString(3, salt);
                    ps.setString(4, "Admin");
                    ps.setString(5, java.time.LocalDate.now().toString());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Admin-Erstellung fehlgeschlagen", e);
        }
    }

    /** Beispieldaten beim ersten Start, damit die Demo nicht leer ist. */
    private static void seedIfEmpty() {
        try (Statement st = get().createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) AS c FROM Kunden")) {
            if (rs.next() && rs.getInt("c") == 0) {
                st.execute("""
                    INSERT INTO Kunden
                    (VollstaendigerName, Adresse, KrankenkasseNummer, Leistungsart,
                     GenehmigteStunden, ErbrachteStunden, VerbleibendeStunden,
                     ZustaendigerMitarbeiter, Notizen, IstAktiv, TerminPlan)
                    VALUES
                    ('Helga Schmidt','Lindenstr. 14, 50667 Köln','AOK-883201','Grundpflege',
                     40,18,22,'Anna Becker','Mobilität eingeschränkt',1,'Woche 1 und 3'),
                    ('Werner Braun','Hauptstr. 7, 50670 Köln','TK-110945','Hauswirtschaft',
                     24,24,0,'Tobias Klein','',1,'Woche 2 und 4'),
                    ('Ingrid Vogel','Am Markt 2, 50676 Köln','BARMER-552310','Grundpflege',
                     60,46.5,13.5,'Maria Hoffmann','Diabetiker',1,'Woche 1 und 3');
                """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Beispieldaten fehlgeschlagen", e);
        }
    }
}
