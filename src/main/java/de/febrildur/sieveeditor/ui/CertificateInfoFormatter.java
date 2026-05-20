package de.febrildur.sieveeditor.ui;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

import de.febrildur.sieveeditor.system.CertificateStore;

public class CertificateInfoFormatter {

    private CertificateInfoFormatter() {
    }

    public static String formatFingerprintMultiline(String fingerprint) {
        if (fingerprint == null || fingerprint.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        String[] parts = fingerprint.split(":");
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append(":");
                if ((i + 1) % 8 == 0) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static String formatSubject(X509Certificate cert) {
        return cert != null ? cert.getSubjectX500Principal().getName() : "";
    }

    public static String formatIssuer(X509Certificate cert) {
        return cert != null ? cert.getIssuerX500Principal().getName() : "";
    }

    public static String formatValidityFrom(X509Certificate cert) {
        if (cert == null || cert.getNotBefore() == null) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cert.getNotBefore());
    }

    public static String formatValidityTo(X509Certificate cert) {
        if (cert == null || cert.getNotAfter() == null) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cert.getNotAfter());
    }

    public static String formatSerialNumber(X509Certificate cert) {
        if (cert == null || cert.getSerialNumber() == null) return "N/A";
        return cert.getSerialNumber().toString(16).toUpperCase();
    }

    public static String formatFingerprintDisplay(X509Certificate cert) {
        if (cert == null) return "";
        return formatFingerprintMultiline(CertificateStore.getFormattedFingerprint(cert));
    }
}
