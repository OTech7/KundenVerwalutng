# KundenVerwaltung – JavaFX + SQLite (Maven)

Desktop app for **Reinheit & Sauberkeit GmbH**, built in **Java 21 / JavaFX 21**
with a local **SQLite** database. German UI, per the specification.
Build tool: **Maven**.

Features include login/sign-up (hashed passwords, Admin/Mitarbeiter roles), the
seven management screens, company-branded PDF reports, and **per-customer Word
data sheets** (Kundendatenblatt .docx, matching the company template).

> Note: the project is **classpath-based** (no `module-info.java`). This is
> intentional — Apache POI (used for Word generation) is awkward under the Java
> module system, and JavaFX runs fine on the classpath via the Maven plugin.

## What's included

| Layer | File(s) | Purpose |
|-------|---------|---------|
| Entry | `App.java` | Starts JavaFX, initializes the DB |
| DB | `db/Database.java` | SQLite connection + full schema (4 tables) + sample data (`kunden.db`) |
| Model | `model/Kunde, Termin, Einsatz, Zahlung` | Data classes for each table |
| DAO | `dao/KundeDao, EinsatzDao, ZahlungDao, TerminDao` | JDBC queries |
| Service | `service/TerminGenerator` | Builds 1.5 h appointments from `TerminPlan` (Woche 1+3 / 2+4) |
| Service | `service/StundenService` | Auto-updates Erbrachte/Verbleibende Stunden on a completed Einsatz |
| Service | `service/DashboardService` | COUNT/SUM queries for the 7 dashboard statistics |
| UI | `ui/Hauptfenster` | Main window with logo + nav (Dashboard, Kunden, Einsätze, Zahlungen, Termine, Druck, Beenden) |
| UI | `ui/KundenFenster` | Table, Alle/Aktive/Ehemalige filter, add-customer dialog (auto-creates Termine) |
| UI | `ui/EinsaetzeFenster` | Einsatz list + add dialog; status "Erledigt" auto-books hours |
| UI | `ui/ZahlungenFenster` | Payment list + add dialog, with paid/open totals |
| UI | `ui/TermineFenster` | Auto-generated appointments, filterable by customer |
| UI | `ui/DashboardFenster` | The 7 spec statistics as tiles |
| UI | `ui/DruckFenster` | Printable reports with company letterhead + footer, via `PrinterJob` → "Microsoft Print to PDF" |

The theme colors and the logo/letterhead are taken directly from the company logo
(`src/main/resources/images/`).

## Prerequisites

- **JDK 21** (Temurin/Adoptium recommended).
- **IntelliJ IDEA** (Community Edition is free and sufficient). Maven is bundled
  with IntelliJ, so you do **not** need to install Maven separately.

## Run it from IntelliJ IDEA

1. **File → Open…** and select this `KundenVerwaltung` folder (the one containing
   `pom.xml`). IntelliJ detects the Maven project and downloads the dependencies
   automatically (first time takes a minute — watch the progress bar at the bottom).
2. Make sure the project SDK is **21**: **File → Project Structure → Project → SDK → 21**.
   (If 21 isn't listed, click *Add SDK → Download JDK → version 21*.)
3. Open the **Maven** tool window on the right (the little "m" icon), then expand
   **KundenVerwaltung → Plugins → javafx** and double-click **`javafx:run`**.
   The app window opens.

   *Alternative:* open `src/main/java/de/reinheit/kundenverwaltung/App.java` and click
   the green ▶ next to `class App`. If you get a "JavaFX components are missing"
   error when running `App` directly, use the `javafx:run` Maven goal instead — it
   sets up the JavaFX module path for you.

A `kunden.db` SQLite file is created automatically next to the app on first run,
seeded with three sample customers.

## Run it from the command line (optional)

```bash
# from the KundenVerwaltung/ folder
mvn javafx:run        # macOS/Linux
mvn javafx:run        # Windows (mvn.cmd if needed)
```

(Uses the Maven wrapper if present, otherwise your installed Maven.)

## Build a Windows .exe / .msi (no Java needed on the target PC)

`mvn clean package` produces a fat jar (`target/KundenVerwaltung-1.0.0-app.jar`),
which **jpackage** (bundled with JDK 21) turns into a self-contained Windows app.

👉 **Full step-by-step instructions: [BUILD.md](BUILD.md)**

## Where the data lives

The app writes to the user profile, not the install folder:

```
%APPDATA%\KundenVerwaltung\
    kunden.db     <- database
    kunden.key    <- encryption key for the sensitive fields
```

⚠ **Back up `kunden.key` together with `kunden.db`** — without the key the
encrypted fields cannot be recovered.

## Architecture notes

- **Plain JDBC + DAO** (no ORM) — minimal and transparent for 4 tables.
- **`VerbleibendeStunden` = GenehmigteStunden − ErbrachteStunden**, recomputed by
  `StundenService` on every completed Einsatz.
- **Foreign keys** enforced (`PRAGMA foreign_keys = ON`): Termine/Einsaetze/Zahlungen
  each reference `Kunden.Kundennummer` (1:N) with `ON DELETE CASCADE`.

## Optional enhancements

The core app is complete. For production you may still want:

1. **Edit / delete** rows (currently add + view).
2. **Validation** — required-field checks and numeric guards in the dialogs.
3. **Backup/export** — copy `kunden.db` or export reports to a chosen folder.
