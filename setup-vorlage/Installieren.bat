@echo off
setlocal
chcp 65001 >nul

rem ================================================================
rem  KundenVerwaltung - Installation
rem  Reinheit & Sauberkeit GmbH
rem
rem  Installiert das Programm in das Benutzerprofil.
rem  Es werden KEINE Administratorrechte benoetigt.
rem ================================================================

set "QUELLE=%~dp0app"
set "ZIEL=%LOCALAPPDATA%\KundenVerwaltung"
set "EXE=%ZIEL%\KundenVerwaltung.exe"
set "STARTMENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs"

echo.
echo   KundenVerwaltung wird installiert...
echo   Zielordner: %ZIEL%
echo.

if not exist "%QUELLE%\KundenVerwaltung.exe" (
    echo   FEHLER: Der Ordner "app" wurde nicht gefunden.
    echo   Bitte das ZIP-Archiv vollstaendig entpacken und erneut starten.
    echo.
    pause
    exit /b 1
)

rem --- Laufendes Programm beenden, damit Dateien ersetzt werden koennen ---
taskkill /IM KundenVerwaltung.exe /F >nul 2>&1

rem --- Dateien kopieren ---
if exist "%ZIEL%" rmdir /S /Q "%ZIEL%"
mkdir "%ZIEL%" 2>nul
xcopy "%QUELLE%" "%ZIEL%" /E /I /Q /Y >nul
if errorlevel 1 (
    echo   FEHLER beim Kopieren der Dateien.
    pause
    exit /b 1
)

rem --- Verknuepfungen anlegen (Desktop + Startmenue) ---
powershell -NoProfile -Command ^
  "$w = New-Object -ComObject WScript.Shell;" ^
  "$s = $w.CreateShortcut([Environment]::GetFolderPath('Desktop') + '\KundenVerwaltung.lnk');" ^
  "$s.TargetPath = '%EXE%'; $s.WorkingDirectory = '%ZIEL%'; $s.IconLocation = '%EXE%'; $s.Save();" ^
  "$s2 = $w.CreateShortcut('%STARTMENU%\KundenVerwaltung.lnk');" ^
  "$s2.TargetPath = '%EXE%'; $s2.WorkingDirectory = '%ZIEL%'; $s2.IconLocation = '%EXE%'; $s2.Save();"

rem --- Deinstallation bereitstellen ---
copy /Y "%~dp0Deinstallieren.bat" "%ZIEL%\Deinstallieren.bat" >nul 2>&1

echo.
echo   ============================================
echo    Installation erfolgreich abgeschlossen.
echo   ============================================
echo.
echo   Starten ueber das Desktop-Symbol oder das Startmenue.
echo.
echo   Erste Anmeldung:  Benutzer: admin   Passwort: admin123
echo   (Das Passwort muss danach sofort geaendert werden.)
echo.

choice /C JN /N /M "   Programm jetzt starten? (J/N): "
if errorlevel 2 goto ende
start "" "%EXE%"

:ende
echo.
pause
endlocal
