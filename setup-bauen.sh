#!/usr/bin/env bash
# ---------------------------------------------------------------
# Baut ein Setup-Paket OHNE zusaetzliche Werkzeuge (kein WiX noetig).
#
# Ausfuehren in Git Bash im Projektordner:
#     bash setup-bauen.sh
#
# Voraussetzungen:  JDK 21 (enthaelt jpackage) + Maven
#
# Ergebnis:  dist/KundenVerwaltung-Setup.zip
#            Diese ZIP-Datei an den Kunden schicken.
#            Kunde: entpacken -> "Installieren.bat" doppelklicken.
# ---------------------------------------------------------------
set -e

# Version automatisch aus pom.xml lesen (erste <version>…</version>-Zeile)
VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)
[ -z "$VERSION" ] && VERSION="1.0.0"
echo "==> Version laut pom.xml: ${VERSION}"
JAR="KundenVerwaltung-${VERSION}-app.jar"   # von Maven erzeugt (mit Version im Namen)
STABIL="KundenVerwaltung-app.jar"            # fester Name im Programm (für kleine Updates)
MAIN_CLASS="de.reinheit.kundenverwaltung.Launcher"
PAKET="dist/KundenVerwaltung-Setup"

echo "==> 1/5  Fat JAR bauen"
mvn -q clean package

echo "==> 2/5  Eingabeordner vorbereiten (JAR mit festem Namen)"
rm -rf build-input dist
mkdir -p build-input
# JAR unter festem Namen ablegen -> Updates ersetzen später nur diese eine Datei
cp "target/${JAR}" "build-input/${STABIL}"

echo "==> 3/5  Anwendung erzeugen (app-image)"
ICON_OPT=""
if [ -f "src/main/resources/images/app.ico" ]; then
  ICON_OPT="--icon src/main/resources/images/app.ico"
fi

jpackage \
  --type app-image \
  --name KundenVerwaltung \
  --app-version "${VERSION}" \
  --vendor "Reinheit & Sauberkeit GmbH" \
  --input build-input \
  --main-jar "${STABIL}" \
  --main-class "${MAIN_CLASS}" \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "-Djava.net.useSystemProxies=true" \
  ${ICON_OPT} \
  --dest dist

echo "==> 4/5  Setup-Paket zusammenstellen"
mkdir -p "${PAKET}"
mv dist/KundenVerwaltung "${PAKET}/app"
cp setup-vorlage/Installieren.bat   "${PAKET}/"
cp setup-vorlage/Deinstallieren.bat "${PAKET}/"

echo "==> 5/5  ZIP + kleines Update-JAR erstellen"
( cd dist && powershell -NoProfile -Command \
    "Compress-Archive -Path 'KundenVerwaltung-Setup' -DestinationPath 'KundenVerwaltung-Setup.zip' -Force" )
# Kleines Update-Paket (~wenige MB): nur die App, ohne Java-Laufzeit
cp "target/${JAR}" "dist/${STABIL}"

echo
echo "Fertig:"
echo "  dist/KundenVerwaltung-Setup.zip   (voller Installer – Erstinstallation)"
echo "  dist/KundenVerwaltung-app.jar     (kleines Update – an die Release anhängen)"
echo
echo "Beide Dateien an die GitHub-Release anhängen."
