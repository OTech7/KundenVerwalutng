# EXE erstellen (Windows) – KundenVerwaltung

Ziel: eine Anwendung, die auf einem **anderen PC ohne installiertes Java** läuft.
`jpackage` bündelt dafür eine eigene Java-Laufzeit mit ein.

---

## 1. Voraussetzungen auf dem **Build-PC** (Ihr Rechner)

| Was | Wofür | Pflicht |
|-----|-------|---------|
| **JDK 21** | enthält `jpackage` | ✅ ja |
| **Maven** | Build (in IntelliJ enthalten) | ✅ ja |
| **WiX Toolset 3.14** | nur für `.msi` / `.exe`-**Installer** | ⬜ optional |

> Das Paket muss **unter Windows** gebaut werden, damit eine Windows-EXE entsteht
> (JavaFX bringt plattformspezifische Bibliotheken mit).

WiX (falls Installer gewünscht): <https://github.com/wixtoolset/wix3/releases> → `wix314.exe`.

## 2. Voraussetzungen auf dem **Ziel-PC** (Kunde)

- Windows 10 oder 11, 64-Bit.
- **Sonst nichts.** Kein Java, kein JavaFX – ist im Paket enthalten.

---

## 3. Fat JAR bauen

Im Projektordner:

```bat
mvn clean package
```

Ergebnis: **`target\KundenVerwaltung-1.0.0-app.jar`**
(enthält JavaFX, Apache POI, SQLite-Treiber und die Anwendung).

Kurz testen:

```bat
java -jar target\KundenVerwaltung-1.0.0-app.jar
```

## 4. Eingabeordner vorbereiten

`jpackage` nimmt **alle** JARs aus dem Eingabeordner. Deshalb nur das Fat JAR
in einen sauberen Ordner kopieren:

```bat
rmdir /s /q build-input 2>nul
mkdir build-input
copy target\KundenVerwaltung-1.0.0-app.jar build-input\
```

---

## 5a. Variante A – portabler Ordner mit `.exe` (ohne WiX)

Am einfachsten. Erzeugt `dist\KundenVerwaltung\KundenVerwaltung.exe`.

```bat
jpackage ^
  --type app-image ^
  --name KundenVerwaltung ^
  --app-version 1.0.0 ^
  --vendor "Reinheit & Sauberkeit GmbH" ^
  --input build-input ^
  --main-jar KundenVerwaltung-1.0.0-app.jar ^
  --main-class de.reinheit.kundenverwaltung.Launcher ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --dest dist
```

Den Ordner `dist\KundenVerwaltung\` komplett auf den Ziel-PC kopieren und
`KundenVerwaltung.exe` starten. Fertig.

## 5b. Variante B – richtiger Installer (`.msi`, benötigt WiX)  ⭐ empfohlen

**Schnellweg (Git Bash im Projektordner):**

```bash
./installer-bauen.sh
```

Das Skript erledigt alle Schritte und legt die fertige Datei unter
`dist/KundenVerwaltung-1.0.0.msi` ab.

**Manuell:**

```bat
jpackage ^
  --type msi ^
  --name KundenVerwaltung ^
  --app-version 1.0.0 ^
  --vendor "Reinheit & Sauberkeit GmbH" ^
  --input build-input ^
  --main-jar KundenVerwaltung-1.0.0-app.jar ^
  --main-class de.reinheit.kundenverwaltung.Launcher ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --win-per-user-install ^
  --dest dist
```

Ergebnis: `dist\KundenVerwaltung-1.0.0.msi` – doppelklicken zum Installieren.

> `--win-per-user-install` installiert ins Benutzerprofil und vermeidet
> Administratorrechte.

### Eigenes Icon (optional)

`jpackage` braucht eine **`.ico`**-Datei (kein PNG):

```bat
  --icon src\main\resources\images\app.ico ^
```

Das Firmenlogo (`images\logo.png`) lässt sich z. B. mit einem Online-Konverter
oder IrfanView nach `.ico` umwandeln.

---

## 6. Wo liegen die Daten?

Die Anwendung schreibt **nicht** in den Installationsordner (der ist
schreibgeschützt), sondern in das Benutzerprofil:

```
%APPDATA%\KundenVerwaltung\
    kunden.db     <- Datenbank (Kunden, Einsätze, Zahlungen, Rechnungen …)
    kunden.key    <- Schlüssel für die verschlüsselten Felder
```

In der Adresszeile des Explorers einfach `%APPDATA%\KundenVerwaltung` eingeben.

> Beim ersten Start werden vorhandene `kunden.db` / `kunden.key` aus dem
> Programmordner (Entwicklungsmodus) automatisch dorthin übernommen.

### ⚠ Wichtig: Backup

**`kunden.key` unbedingt zusammen mit `kunden.db` sichern.**
Ohne den Schlüssel sind die verschlüsselten Felder (Adresse, Telefon, E-Mail,
Krankenkasse, Pflegegrad, Geburtsdatum) **nicht wiederherstellbar**.

Empfehlung für den Kunden: den Ordner `%APPDATA%\KundenVerwaltung` regelmäßig
sichern und zusätzlich die **Festplattenverschlüsselung (BitLocker)** aktivieren
(siehe `SICHERHEITSAUDIT.md`, Punkt M-2).

---

## 7. Erster Start beim Kunden

1. Anmelden mit **`admin` / `admin123`**.
2. Die Anwendung erzwingt sofort ein **neues Passwort** (mind. 10 Zeichen).
3. Weitere Konten unter **Benutzer** anlegen (nur Admins sehen diesen Bereich).

---

## Fehlersuche

| Problem | Ursache / Lösung |
|---------|------------------|
| `jpackage` nicht gefunden | JDK 21 nicht im `PATH`. Prüfen: `java -version`, `jpackage --version`. |
| `Can't find WiX tools` | WiX 3.14 installieren, oder `--type app-image` verwenden (Variante A). |
| `Error: Invalid Option: [--win-per-user-install]` | Diese Option gibt es nur bei `--type msi`/`exe`, nicht bei `app-image`. |
| App startet nicht, keine Meldung | `dist\KundenVerwaltung\KundenVerwaltung.exe` aus der Eingabeaufforderung starten – Fehler erscheinen dann in der Konsole. |
| Umlaute falsch dargestellt | `--java-options "-Dfile.encoding=UTF-8"` beibehalten. |
