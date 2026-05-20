package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateStoreTest {

    private static final byte[] TEST_CERT_ENCODED = "test-certificate-data".getBytes(StandardCharsets.UTF_8);

    @Mock
    private X509Certificate mockCert;

    @TempDir
    Path tempDir;

    private String originalUserHome;
    private CertificateStore store;

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
        store = new CertificateStore();
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Nested
    @DisplayName("Fingerprint operations")
    class FingerprintTests {

        @Test
        void shouldCalculateNonEmptyFingerprint() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);

            String fingerprint = CertificateStore.getFingerprint(mockCert);

            assertThat(fingerprint).isNotNull().isNotEmpty();
        }

        @Test
        void shouldReturnSameFingerprintForSameCertificate() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);

            String first = CertificateStore.getFingerprint(mockCert);
            String second = CertificateStore.getFingerprint(mockCert);

            assertThat(first).isEqualTo(second);
        }

        @Test
        void shouldFormatFingerprintAsColonSeparatedHex() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);

            String formatted = CertificateStore.getFormattedFingerprint(mockCert);

            assertThat(formatted).matches("[0-9A-F]{2}(:[0-9A-F]{2})+");
        }

        @Test
        void shouldReturnErrorMessageWhenEncodingFailsForFormatted() throws Exception {
            when(mockCert.getEncoded()).thenThrow(new CertificateEncodingException("fail"));

            assertThat(CertificateStore.getFormattedFingerprint(mockCert))
                .isEqualTo("Unable to calculate fingerprint");
        }
    }

    @Nested
    @DisplayName("Initial state")
    class InitialStateTests {

        @Test
        void shouldNotBeTrustedForUnknownCertificate() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);

            assertThat(store.isTrusted(mockCert)).isFalse();
        }

        @Test
        void shouldNotBeRejectedForUnknownCertificate() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);

            assertThat(store.isRejected(mockCert)).isFalse();
        }
    }

    @Nested
    @DisplayName("Trust operations")
    class TrustOperations {

        @BeforeEach
        void setUp() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);
            when(mockCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=Test"));
        }

        @Test
        void shouldMarkCertificateAsTrusted() {
            store.trustCertificate(mockCert, "test-server");

            assertThat(store.isTrusted(mockCert)).isTrue();
        }

        @Test
        void shouldMarkCertificateAsRejected() {
            store.rejectCertificate(mockCert, "test-server");

            assertThat(store.isRejected(mockCert)).isTrue();
        }

        @Test
        void shouldMakeRejectedFalseWhenTrusted() {
            store.rejectCertificate(mockCert, "test-server");
            assertThat(store.isRejected(mockCert)).isTrue();

            store.trustCertificate(mockCert, "test-server");

            assertThat(store.isRejected(mockCert)).isFalse();
            assertThat(store.isTrusted(mockCert)).isTrue();
        }

        @Test
        void shouldMakeTrustedFalseWhenRejected() {
            store.trustCertificate(mockCert, "test-server");
            assertThat(store.isTrusted(mockCert)).isTrue();

            store.rejectCertificate(mockCert, "test-server");

            assertThat(store.isTrusted(mockCert)).isFalse();
            assertThat(store.isRejected(mockCert)).isTrue();
        }

        @Test
        void shouldRemoveCertificateDecision() {
            store.trustCertificate(mockCert, "test-server");
            assertThat(store.isTrusted(mockCert)).isTrue();

            store.removeCertificate(mockCert);

            assertThat(store.isTrusted(mockCert)).isFalse();
            assertThat(store.isRejected(mockCert)).isFalse();
        }

        @Test
        void shouldClearAllDecisions() {
            store.trustCertificate(mockCert, "test-server");
            assertThat(store.isTrusted(mockCert)).isTrue();

            store.clear();

            assertThat(store.isTrusted(mockCert)).isFalse();
            assertThat(store.isRejected(mockCert)).isFalse();
        }

        @Test
        void shouldPersistTrustedCertificateAcrossInstances() {
            store.trustCertificate(mockCert, "test-server");

            CertificateStore store2 = new CertificateStore();

            assertThat(store2.isTrusted(mockCert)).isTrue();
        }

        @Test
        void shouldPersistRejectedCertificateAcrossInstances() {
            store.rejectCertificate(mockCert, "test-server");

            CertificateStore store2 = new CertificateStore();

            assertThat(store2.isRejected(mockCert)).isTrue();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        void shouldReturnFalseForIsTrustedWhenEncodingFails() throws Exception {
            when(mockCert.getEncoded()).thenThrow(new CertificateEncodingException("fail"));

            assertThat(store.isTrusted(mockCert)).isFalse();
        }

        @Test
        void shouldReturnFalseForIsRejectedWhenEncodingFails() throws Exception {
            when(mockCert.getEncoded()).thenThrow(new CertificateEncodingException("fail"));

            assertThat(store.isRejected(mockCert)).isFalse();
        }
    }

    @Nested
    @DisplayName("Directory management")
    class DirectoryManagementTests {

        @Test
        void shouldCreateProfilesDirectoryOnConstruction() {
            File profilesDir = new File(tempDir.toFile(), ".sieveprofiles");

            assertThat(profilesDir).exists().isDirectory();
        }

        @Test
        void shouldCreateStoreFileOnConstruction() {
            File storeFile = new File(new File(tempDir.toFile(), ".sieveprofiles"), "certificates.properties");

            assertThat(storeFile).exists().isFile();
        }
    }
}
