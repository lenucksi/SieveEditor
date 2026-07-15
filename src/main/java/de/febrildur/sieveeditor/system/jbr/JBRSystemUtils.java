package de.febrildur.sieveeditor.system.jbr;

import java.util.logging.Logger;

public final class JBRSystemUtils {

    private static final Logger LOG = Logger.getLogger(JBRSystemUtils.class.getName());

    private static final boolean SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Boolean avail = (Boolean) jbrClass.getMethod("isAvailable").invoke(null);
            if (avail) {
                supported = (Boolean) jbrClass.getMethod("isSystemUtilsSupported").invoke(null);
            }
        } catch (Exception e) {
            LOG.fine("JBR not available: " + e.getMessage());
        }
        SUPPORTED = supported;
    }

    private JBRSystemUtils() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static void tryCompactMemory() {
        if (!SUPPORTED) {
            return;
        }
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Object su = jbrClass.getMethod("getSystemUtils").invoke(null);
            su.getClass().getMethod("fullGC").invoke(su);
            su.getClass().getMethod("shrinkingGC").invoke(su);
            LOG.fine("JBR memory compaction triggered");
        } catch (Exception e) {
            LOG.fine("JBR memory compaction failed: " + e.getMessage());
        }
    }
}
