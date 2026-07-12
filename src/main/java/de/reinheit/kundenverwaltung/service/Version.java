package de.reinheit.kundenverwaltung.service;

import java.io.InputStream;
import java.util.Properties;

/** Liefert die aktuelle App-Version (aus version.properties, gefüllt von Maven). */
public final class Version {

    private static final String WERT = lade();

    private Version() {}

    public static String aktuell() { return WERT; }

    private static String lade() {
        try (InputStream in = Version.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty("app.version", "").trim();
                // Bei einem Start aus der IDE ohne Maven-Filter steht evtl. noch ${...} drin.
                if (!v.isEmpty() && !v.startsWith("${")) return v;
            }
        } catch (Exception ignored) { }
        return "1.0.0";
    }
}
