package de.febrildur.sieveeditor.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.X509Certificate;

import de.febrildur.sieveeditor.system.CertificateStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateDialogTest {

    @Mock
    private X509Certificate mockCert;

    @TempDir
    Path tempDir;

    private String originalUserHome;

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Nested
    @DisplayName("UserDecision enum")
    class UserDecisionTests {

        @Test
        void shouldHaveThreeDecisionValues() {
            assertThat(CertificateDialog.UserDecision.values())
                .hasSize(3);
        }

        @Test
        void shouldContainTrustValue() {
            assertThat(CertificateDialog.UserDecision.valueOf("TRUST"))
                .isEqualTo(CertificateDialog.UserDecision.TRUST);
        }

        @Test
        void shouldContainRejectValue() {
            assertThat(CertificateDialog.UserDecision.valueOf("REJECT"))
                .isEqualTo(CertificateDialog.UserDecision.REJECT);
        }

        @Test
        void shouldContainCancelValue() {
            assertThat(CertificateDialog.UserDecision.valueOf("CANCEL"))
                .isEqualTo(CertificateDialog.UserDecision.CANCEL);
        }

        @Test
        void shouldHaveCancelAsThirdEnumConstant() {
            assertThat(CertificateDialog.UserDecision.CANCEL.ordinal()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Fingerprint formatting")
    class FingerprintFormattingTests {

        @Test
        void shouldFormatFingerprintIntoGroupsOfEight() {
            String input = "01:23:45:67:89:AB:CD:EF:FE:DC:BA:98:76:54:32:10";
            String expected = "01:23:45:67:89:AB:CD:EF:\nFE:DC:BA:98:76:54:32:10";

            String result = formatMultiline(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldNotAddNewlineForLessThanEightGroups() {
            String input = "01:23:45:67";
            String expected = "01:23:45:67";

            String result = formatMultiline(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldAddNewlineAtEveryEighthGroup() {
            String input = "01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23";
            String expected = "01:23:45:67:89:AB:CD:EF:\n01:23:45:67:89:AB:CD:EF:\n01:23";

            String result = formatMultiline(input);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldPreserveSingleGroup() {
            String result = formatMultiline("AB");

            assertThat(result).isEqualTo("AB");
        }

        @Test
        void shouldHandleEmptyString() {
            String result = formatMultiline("");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldProduceValidFingerprintFromCertificateStore() throws Exception {
            when(mockCert.getEncoded()).thenReturn("test-fingerprint-data".getBytes(StandardCharsets.UTF_8));

            String formatted = CertificateStore.getFormattedFingerprint(mockCert);

            assertThat(formatted).matches("[0-9A-F]{2}(:[0-9A-F]{2})+");
            String[] hexPairs = formatted.split(":");
            assertThat(hexPairs).hasSize(32);
        }
    }

    private static String formatMultiline(String fingerprint) {
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
}
