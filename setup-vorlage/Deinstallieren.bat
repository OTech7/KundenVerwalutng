@echo off
setlocal
chcp 65001 >nul

rem ================================================================
rem  KundenVerwaltung - Deinstallation
rem  Entfernt das Programm und die Verknuepfungen.
rem  Die DATEN bleiben erhalten (siehe Hinweis unten).
rem ================================================================

set "ZIEL=%LOCALAPPDATA%\KundenVerwaltung"
set "STARTMENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs\KundenVerwaltung.lnk"
set "DATEN=%APPDATA%\KundenVerwaltung"

echo.
echo   KundenVerwaltung wird entfernt.
echo.
echo   HINWEIS: Ihre Daten (Datenbank und Schluesseldatei) bleiben
echo   erhalten unter:
echo       %DATEN%
echo.

choice /C JN /N /M "   Wirklich deinstallieren? (J/N): "
if errorlevel 2 goto abbruch

taskkill /IM KundenVerwaltung.exe /F >nul 2>&1

del "%STARTMENU%" >nul 2>&1
powershell -NoProfile -Command ^
  "Remove-Item ([Environment]::GetFolderPath('Desktop') + '\KundenVerwaltung.lnk') -ErrorAction SilentlyContinue"

rem Ordner aus sich selbst heraus loeschen: verzoegert per cmd
start "" cmd /c "timeout /t 2 >nul & rmdir /S /Q ""%ZIEL%"""

echo.
echo   Das Programm wurde entfernt.
echo   Zum vollstaendigen Loeschen der Daten diesen Ordner manuell entfernen:
echo       %DATEN%
echo.
pause
exit /b 0

:abbruch
echo   Abgebrochen.
pause
endlocal
