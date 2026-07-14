package de.febrildur.sieveeditor.system.jbr;

import java.awt.Window;
import java.util.logging.Logger;

public final class JBRRoundedCorners {

    private static final Logger LOG = Logger.getLogger(JBRRoundedCorners.class.getName());

    private static final boolean SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Boolean avail = (Boolean) jbrClass.getMethod("isAvailable").invoke(null);
            if (avail) {
                supported = (Boolean) jbrClass.getMethod("isRoundedCornersManagerSupported").invoke(null);
            }
        } catch (Exception e) {
            // JBR not available
        }
        SUPPORTED = supported;
    }

    private JBRRoundedCorners() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static boolean apply(Window window) {
        if (!SUPPORTED) {
            return false;
        }
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Object rcm = jbrClass.getMethod("getRoundedCornersManager").invoke(null);
            rcm.getClass().getMethod("setRoundedCorners", Window.class, float.class)
                    .invoke(rcm, window, 12f);
            LOG.fine("Rounded corners applied via JBR");
            return true;
        } catch (Exception e) {
            LOG.fine("Could not apply rounded corners: " + e.getMessage());
            return false;
        }
    }
}
