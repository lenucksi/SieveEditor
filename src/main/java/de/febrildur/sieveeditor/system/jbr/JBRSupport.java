package de.febrildur.sieveeditor.system.jbr;

import java.util.logging.Logger;

public final class JBRSupport {

    private static final Logger LOG = Logger.getLogger(JBRSupport.class.getName());

    private static final boolean AVAILABLE;
    private static final String API_VERSION;
    private static final String IMPL_VERSION;

    static {
        boolean avail = false;
        String api = "unknown";
        String impl = "unknown";
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            avail = (Boolean) jbrClass.getMethod("isAvailable").invoke(null);
            api = (String) jbrClass.getMethod("getApiVersion").invoke(null);
            impl = (String) jbrClass.getMethod("getImplVersion").invoke(null);
        } catch (Exception e) {
            LOG.fine("JBR API not in classpath or not supported: " + e.getMessage());
        }
        AVAILABLE = avail;
        API_VERSION = api;
        IMPL_VERSION = impl;
    }

    private JBRSupport() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static String getApiVersion() {
        return API_VERSION;
    }

    public static String getImplVersion() {
        return IMPL_VERSION;
    }

    public static boolean logStatus() {
        if (AVAILABLE) {
            LOG.info("JBR API available: v" + API_VERSION + " (impl: " + IMPL_VERSION + ")");
        } else {
            LOG.info("JBR API not available");
        }
        return AVAILABLE;
    }
}
