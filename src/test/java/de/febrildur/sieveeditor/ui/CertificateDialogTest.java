package de.febrildur.sieveeditor.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CertificateDialog")
class CertificateDialogTest {

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
}
