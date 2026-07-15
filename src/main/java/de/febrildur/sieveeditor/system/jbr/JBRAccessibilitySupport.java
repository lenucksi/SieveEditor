package de.febrildur.sieveeditor.system.jbr;

import java.awt.Window;
import java.util.logging.Logger;

import javax.accessibility.Accessible;

public final class JBRAccessibilitySupport {

    private static final Logger LOG = Logger.getLogger(JBRAccessibilitySupport.class.getName());

    private static final boolean SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Boolean avail = (Boolean) jbrClass.getMethod("isAvailable").invoke(null);
            if (avail) {
                supported = (Boolean) jbrClass.getMethod("isAccessibleAnnouncerSupported").invoke(null);
            }
        } catch (Exception e) {
            LOG.fine("JBR not available: " + e.getMessage());
        }
        SUPPORTED = supported;
    }

    private JBRAccessibilitySupport() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static void announce(String text, Window window) {
        if (!SUPPORTED) {
            return;
        }
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Object announcer = jbrClass.getMethod("getAccessibleAnnouncer").invoke(null);
            announcer.getClass()
                    .getMethod("announce", Accessible.class, String.class, int.class)
                    .invoke(announcer, window, text, 1);
            LOG.fine("JBR announcement: " + text);
        } catch (Exception e) {
            LOG.fine("JBR announcement failed: " + e.getMessage());
        }
    }
}
