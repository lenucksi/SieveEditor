package de.febrildur.sieveeditor.system.jbr;

import java.awt.Window;
import java.util.logging.Logger;

public final class JBRWindowDecorations {

    private static final Logger LOG = Logger.getLogger(JBRWindowDecorations.class.getName());

    private static final boolean SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Boolean avail = (Boolean) jbrClass.getMethod("isAvailable").invoke(null);
            if (avail) {
                supported = (Boolean) jbrClass.getMethod("isWindowDecorationsSupported").invoke(null);
            }
        } catch (Exception e) {
            // JBR not available
        }
        SUPPORTED = supported;
    }

    private JBRWindowDecorations() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static boolean applyCustomTitleBar(Window window, float titleBarHeight) {
        if (!SUPPORTED) {
            return false;
        }
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Object wd = jbrClass.getMethod("getWindowDecorations").invoke(null);
            Object customTitleBar = wd.getClass().getMethod("createCustomTitleBar").invoke(wd);
            customTitleBar.getClass().getMethod("setHeight", float.class).invoke(customTitleBar, titleBarHeight);
            wd.getClass().getMethod("setCustomTitleBar", Window.class, Object.class)
                    .invoke(wd, window, customTitleBar);
            LOG.fine("Custom title bar applied via JBR");
            return true;
        } catch (Exception e) {
            LOG.fine("Could not apply custom title bar: " + e.getMessage());
            return false;
        }
    }
}
