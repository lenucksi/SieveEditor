package de.febrildur.sieveeditor.system;

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
import javax.net.ssl.X509TrustManager;
import javax.swing.SwingUtilities;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import de.febrildur.sieveeditor.ui.CertificateDialog;

@ExtendWith(MockitoExtension.class)
class InteractiveTrustManagerTest {

    private static final byte[] TEST_CERT_ENCODED = "test-certificate-data".getBytes(StandardCharsets.UTF_8);

    @Mock
    private X509TrustManager mockDefaultTrustManager;

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
    @DisplayName("Delegation behavior")
    class DelegationTests {

        @Test
        void shouldDelegateGetAcceptedIssuers() {
            X509Certificate[] expected = { mockCert };
            when(mockDefaultTrustManager.getAcceptedIssuers()).thenReturn(expected);

            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            assertThat(tm.getAcceptedIssuers()).containsExactly(mockCert);
        }

        @Test
        void shouldDelegateCheckClientTrusted() throws Exception {
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            tm.checkClientTrusted(chain, "RSA");

            verify(mockDefaultTrustManager).checkClientTrusted(chain, "RSA");
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidationTests {

        @Test
        void shouldThrowWhenServerChainIsNull() {
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            assertThatThrownBy(() -> tm.checkServerTrusted(null, "RSA"))
                .isInstanceOf(CertificateException.class);
        }

        @Test
        void shouldThrowWhenServerChainIsEmpty() {
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            assertThatThrownBy(() -> tm.checkServerTrusted(new X509Certificate[0], "RSA"))
                .isInstanceOf(CertificateException.class);
        }
    }

    @Nested
    @DisplayName("Previously stored decisions")
    class StoredDecisionTests {

        @BeforeEach
        void setUp() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);
            when(mockCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=Test"));
        }

        @Test
        void shouldAcceptPreviouslyTrustedCertificate() throws Exception {
            store.trustCertificate(mockCert, "test-server");
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            assertThatCode(() -> tm.checkServerTrusted(chain, "RSA"))
                .doesNotThrowAnyException();
        }

        @Test
        void shouldNotCallDefaultManagerWhenCertIsTrusted() throws Exception {
            store.trustCertificate(mockCert, "test-server");
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            tm.checkServerTrusted(chain, "RSA");

            verify(mockDefaultTrustManager, never()).checkServerTrusted(any(), any());
        }

        @Test
        void shouldThrowWhenCertificateWasPreviouslyRejected() throws Exception {
            store.rejectCertificate(mockCert, "test-server");
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            assertThatThrownBy(() -> tm.checkServerTrusted(chain, "RSA"))
                .isInstanceOf(CertificateException.class);
        }
    }

    @Nested
    @DisplayName("Interactive dialog after CA rejection")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class InteractiveDialogTests {

        @BeforeEach
        void setUp() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);
            when(mockCert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=Test"));
            doThrow(new CertificateException("Not trusted by system CAs"))
                .when(mockDefaultTrustManager).checkServerTrusted(any(), any());
        }

        @Test
        void shouldTrustWhenUserAccepts() throws Exception {
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);
            Throwable[] error = new Throwable[1];
            SwingUtilities.invokeAndWait(() -> {
                try (MockedStatic<CertificateDialog> dialog = mockStatic(CertificateDialog.class)) {
                    dialog.when(() -> CertificateDialog.showCertificateDialog(any(), any(), any()))
                        .thenReturn(CertificateDialog.UserDecision.TRUST);
                    tm.checkServerTrusted(chain, "RSA");
                } catch (CertificateException e) {
                    error[0] = e;
                }
            });
            assertThat(error[0]).isNull();
        }

        @Test
        void shouldRejectWhenUserRejects() throws Exception {
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);
            Throwable[] error = new Throwable[1];
            SwingUtilities.invokeAndWait(() -> {
                try (MockedStatic<CertificateDialog> dialog = mockStatic(CertificateDialog.class)) {
                    dialog.when(() -> CertificateDialog.showCertificateDialog(any(), any(), any()))
                        .thenReturn(CertificateDialog.UserDecision.REJECT);
                    tm.checkServerTrusted(chain, "RSA");
                } catch (CertificateException e) {
                    error[0] = e;
                }
            });
            assertThat(error[0]).isNotNull();
        }

        @Test
        void shouldCancelWhenUserCancels() throws Exception {
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);
            Throwable[] error = new Throwable[1];
            SwingUtilities.invokeAndWait(() -> {
                try (MockedStatic<CertificateDialog> dialog = mockStatic(CertificateDialog.class)) {
                    dialog.when(() -> CertificateDialog.showCertificateDialog(any(), any(), any()))
                        .thenReturn(CertificateDialog.UserDecision.CANCEL);
                    tm.checkServerTrusted(chain, "RSA");
                } catch (CertificateException e) {
                    error[0] = e;
                }
            });
            assertThat(error[0]).isNotNull();
        }

        @Test
        void shouldStoreTrustedCertAfterUserAccepts() throws Exception {
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);
            SwingUtilities.invokeAndWait(() -> {
                try (MockedStatic<CertificateDialog> dialog = mockStatic(CertificateDialog.class)) {
                    dialog.when(() -> CertificateDialog.showCertificateDialog(any(), any(), any()))
                        .thenReturn(CertificateDialog.UserDecision.TRUST);
                    tm.checkServerTrusted(chain, "RSA");
                } catch (CertificateException e) {
                    throw new RuntimeException(e);
                }
            });
            assertThatCode(() -> tm.checkServerTrusted(chain, "RSA"))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("System CA validation")
    class SystemCAValidationTests {

        @Test
        void shouldAcceptWhenDefaultTrustManagerValidates() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            assertThatCode(() -> tm.checkServerTrusted(chain, "RSA"))
                .doesNotThrowAnyException();
        }

        @Test
        void shouldCallDefaultManagerWhenCertNotInStore() throws Exception {
            when(mockCert.getEncoded()).thenReturn(TEST_CERT_ENCODED);
            X509Certificate[] chain = { mockCert };
            InteractiveTrustManager tm = new InteractiveTrustManager(mockDefaultTrustManager, "test-server", null);

            tm.checkServerTrusted(chain, "RSA");

            verify(mockDefaultTrustManager).checkServerTrusted(chain, "RSA");
        }
    }
}
