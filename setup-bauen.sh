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

VERSION="1.0.0"
JAR="KundenVerwaltung-${VERSION}-app.jar"
MAIN_CLASS="de.reinheit.kundenverwaltung.Launcher"
PAKET="dist/KundenVerwaltung-Setup"

echo "==> 1/5  Fat JAR bauen"
mvn -q clean package

echo "==> 2/5  Eingabeordner vorbereiten"
rm -rf build-input dist
mkdir -p build-input
cp "target/${JAR}" build-input/

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
  --main-jar "${JAR}" \
  --main-class "${MAIN_CLASS}" \
  --java-options "-Dfile.encoding=UTF-8" \
  ${ICON_OPT} \
  --dest dist

echo "==> 4/5  Setup-Paket zusammenstellen"
mkdir -p "${PAKET}"
mv dist/KundenVerwaltung "${PAKET}/app"
cp setup-vorlage/Installieren.bat   "${PAKET}/"
cp setup-vorlage/Deinstallieren.bat "${PAKET}/"

echo "==> 5/5  ZIP erstellen"
( cd dist && powershell -NoProfile -Command \
    "Compress-Archive -Path 'KundenVerwaltung-Setup' -DestinationPath 'KundenVerwaltung-Setup.zip' -Force" )

echo
echo "Fertig:  dist/KundenVerwaltung-Setup.zip"
echo
echo "An den Kunden schicken. Er entpackt die ZIP-Datei und"
echo "doppelklickt auf 'Installieren.bat'."
