package de.febrildur.sieveeditor.system.jbr;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

public final class JBRFontSupport {

    private static final Logger LOG = Logger.getLogger(JBRFontSupport.class.getName());

    private static final boolean SUPPORTED;
    private static Font jbrMonoFont;

    static {
        boolean supported = false;
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Boolean avail = (Boolean) jbrClass.getMethod("isAvailable").invoke(null);
            if (avail) {
                Object fontExt = jbrClass.getMethod("getFontExtensions").invoke(null);
                supported = fontExt != null;
            }
        } catch (Exception e) {
            // JBR not available
        }
        SUPPORTED = supported;
        jbrMonoFont = discoverJbrMonoFont();
    }

    private JBRFontSupport() {
    }

    public static boolean isSupported() {
        return SUPPORTED;
    }

    public static Font getJbrMonoFont() {
        return jbrMonoFont;
    }

    public static Font deriveEditorFont(Font baseFont) {
        if (!SUPPORTED) {
            return baseFont;
        }
        try {
            Class<?> jbrClass = Class.forName("com.jetbrains.JBR");
            Object fontExt = jbrClass.getMethod("getFontExtensions").invoke(null);
            Font derived = (Font) fontExt.getClass()
                    .getMethod("deriveFontWithFeatures", Font.class, String[].class)
                    .invoke(fontExt, baseFont, new String[]{"liga", "calt", "zero"});
            LOG.fine("Derived font with JBR ligature features");
            return derived;
        } catch (Exception e) {
            LOG.fine("Could not derive font with features: " + e.getMessage());
            return baseFont;
        }
    }

    private static Font discoverJbrMonoFont() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            return null;
        }
        String[] candidates = {
                javaHome + File.separator + "lib" + File.separator + "fonts" + File.separator + "JetBrainsMono-Regular.ttf",
                javaHome + File.separator + "lib" + File.separator + "fonts" + File.separator + "FiraCode-Regular.ttf",
                javaHome + File.separator + "lib" + File.separator + "fonts" + File.separator + "SourceCodePro-Regular.ttf",
        };
        for (String path : candidates) {
            File fontFile = new File(path);
            if (fontFile.isFile()) {
                try (InputStream is = fontFile.toURI().toURL().openStream()) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    ge.registerFont(font);
                    LOG.fine("Registered JBR bundled font: " + fontFile.getName());
                    return font;
                } catch (Exception e) {
                    LOG.fine("Could not load font " + path + ": " + e.getMessage());
                }
            }
        }
        LOG.fine("No JBR bundled monospace font found in " + javaHome + "/lib/fonts/");
        return null;
    }
}
