package de.febrildur.sieveeditor.system.jbr;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.logging.Logger;

public final class JBRDesktopSupport {

    private static final Logger LOG = Logger.getLogger(JBRDesktopSupport.class.getName());

    private static final boolean SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Boolean avail = (Boolean) jbrClass.getMethod("isAvailable").invoke(null);
            if (avail) {
                supported = (Boolean) jbrClass.getMethod("isDesktopActionsSupported").invoke(null);
            }
        } catch (Exception e) {
            LOG.fine("JBR not available: " + e.getMessage());
        }
        SUPPORTED = supported;
    }

    private JBRDesktopSupport() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static boolean browse(URI uri) {
        if (SUPPORTED) {
            try {
                Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
                Object da = jbrClass.getMethod("getDesktopActions").invoke(null);
                Object handler = da.getClass().getMethod("getHandler").invoke(da);
                if (handler != null) {
                    handler.getClass().getMethod("browse", URI.class).invoke(handler, uri);
                    LOG.fine("JBR DesktopActions.browse() called");
                    return true;
                }
            } catch (Exception e) {
                LOG.fine("JBR DesktopActions.browse() failed, falling back: " + e.getMessage());
            }
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
                return true;
            }
        } catch (Exception e) {
            LOG.fine("Desktop.browse() failed: " + e.getMessage());
        }
        return false;
    }

    public static boolean edit(File file) {
        if (SUPPORTED) {
            try {
                Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
                Object da = jbrClass.getMethod("getDesktopActions").invoke(null);
                Object handler = da.getClass().getMethod("getHandler").invoke(da);
                if (handler != null) {
                    handler.getClass().getMethod("edit", File.class).invoke(handler, file);
                    LOG.fine("JBR DesktopActions.edit() called");
                    return true;
                }
            } catch (Exception e) {
                LOG.fine("JBR DesktopActions.edit() failed, falling back: " + e.getMessage());
            }
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().edit(file);
                return true;
            }
        } catch (Exception e) {
            LOG.fine("Desktop.edit() failed: " + e.getMessage());
        }
        return false;
    }
}
