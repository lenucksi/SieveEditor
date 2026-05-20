package de.febrildur.sieveeditor.ui;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.x500.X500Principal;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateInfoFormatterTest {

    @Mock
    private X509Certificate mockCert;

    @Nested
    @DisplayName("formatFingerprintMultiline")
    class FormatFingerprintMultilineTests {

        @Test
        void shouldFormatFingerprintIntoGroupsOfEight() {
            String input = "01:23:45:67:89:AB:CD:EF:FE:DC:BA:98:76:54:32:10";
            String expected = "01:23:45:67:89:AB:CD:EF:\nFE:DC:BA:98:76:54:32:10";

            String result = CertificateInfoFormatter.formatFingerprintMultiline(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldNotAddNewlineForLessThanEightGroups() {
            String input = "01:23:45:67";
            String expected = "01:23:45:67";

            String result = CertificateInfoFormatter.formatFingerprintMultiline(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldAddNewlineAtEveryEighthGroup() {
            String input = "01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23";
            String expected = "01:23:45:67:89:AB:CD:EF:\n01:23:45:67:89:AB:CD:EF:\n01:23";

            String result = CertificateInfoFormatter.formatFingerprintMultiline(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldPreserveSingleGroup() {
            String result = CertificateInfoFormatter.formatFingerprintMultiline("AB");

            assertThat(result).isEqualTo("AB");
        }

        @Test
        void shouldHandleEmptyString() {
            String result = CertificateInfoFormatter.formatFingerprintMultiline("");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleNull() {
            String result = CertificateInfoFormatter.formatFingerprintMultiline(null);

            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleOddNumberOfPairs() {
            String input = "01:23:45:67:89:AB:CD:EF:01:23:45";
            String expected = "01:23:45:67:89:AB:CD:EF:\n01:23:45";

            String result = CertificateInfoFormatter.formatFingerprintMultiline(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldAddNewlineAfterExactlySixteenPairs() {
            String input = "01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF";
            String expected = "01:23:45:67:89:AB:CD:EF:\n01:23:45:67:89:AB:CD:EF";

            String result = CertificateInfoFormatter.formatFingerprintMultiline(input);

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Certificate field extraction")
    class CertificateFieldTests {

        @Test
        void shouldFormatSubject() {
            when(mockCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test.example.com"));

            String result = CertificateInfoFormatter.formatSubject(mockCert);

            assertThat(result).isEqualTo("CN=test.example.com");
        }

        @Test
        void shouldFormatSubjectAsEmptyForNullCert() {
            assertThat(CertificateInfoFormatter.formatSubject(null)).isEmpty();
        }

        @Test
        void shouldFormatIssuer() {
            when(mockCert.getIssuerX500Principal()).thenReturn(new X500Principal("CN=Test CA"));

            String result = CertificateInfoFormatter.formatIssuer(mockCert);

            assertThat(result).isEqualTo("CN=Test CA");
        }

        @Test
        void shouldFormatIssuerAsEmptyForNullCert() {
            assertThat(CertificateInfoFormatter.formatIssuer(null)).isEmpty();
        }

        @Test
        void shouldFormatValidityFrom() throws Exception {
            Date date = new Date(1700000000000L);
            when(mockCert.getNotBefore()).thenReturn(date);

            String result = CertificateInfoFormatter.formatValidityFrom(mockCert);

            assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        }

        @Test
        void shouldFormatValidityFromAsNaForNullCert() {
            assertThat(CertificateInfoFormatter.formatValidityFrom(null)).isEqualTo("N/A");
        }

        @Test
        void shouldFormatValidityTo() throws Exception {
            Date date = new Date(1730000000000L);
            when(mockCert.getNotAfter()).thenReturn(date);

            String result = CertificateInfoFormatter.formatValidityTo(mockCert);

            assertThat(result).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        }

        @Test
        void shouldFormatValidityToAsNaForNullCert() {
            assertThat(CertificateInfoFormatter.formatValidityTo(null)).isEqualTo("N/A");
        }

        @Test
        void shouldFormatSerialNumber() {
            when(mockCert.getSerialNumber()).thenReturn(java.math.BigInteger.valueOf(65537));

            String result = CertificateInfoFormatter.formatSerialNumber(mockCert);

            assertThat(result).isEqualTo("10001");
        }

        @Test
        void shouldFormatSerialNumberAsNaForNullCert() {
            assertThat(CertificateInfoFormatter.formatSerialNumber(null)).isEqualTo("N/A");
        }

        @Test
        void shouldFormatFingerprintDisplay() throws Exception {
            when(mockCert.getEncoded()).thenReturn("test-fingerprint-data".getBytes(StandardCharsets.UTF_8));

            String result = CertificateInfoFormatter.formatFingerprintDisplay(mockCert);

            assertThat(result).contains("\n");
            String[] lines = result.split("\n");
            assertThat(lines).hasSize(4);
            for (String line : lines) {
                assertThat(line).matches("([0-9A-F]{2}:)*[0-9A-F]{2}:?");
                String[] hexPairs = line.split(":");
                long pairCount = java.util.Arrays.stream(hexPairs).filter(s -> !s.isEmpty()).count();
                assertThat(pairCount).isLessThanOrEqualTo(8);
            }
        }

        @Test
        void shouldFormatFingerprintDisplayAsEmptyForNullCert() {
            assertThat(CertificateInfoFormatter.formatFingerprintDisplay(null)).isEmpty();
        }
    }
}
