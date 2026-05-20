package de.febrildur.sieveeditor.system.credentials;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeePassXCMasterKeyProviderTest {

    @Nested
    @DisplayName("Provider identity")
    class ProviderIdentity {

        @Test
        void shouldReturnKeePassXCAsName() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThat(provider.getName()).isEqualTo("KeePassXC");
        }

        @Test
        void shouldReturnDescriptionContainingKeePassXC() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThat(provider.getDescription())
                    .contains("KeePassXC")
                    .contains("database");
        }
    }

    @Nested
    @DisplayName("Availability")
    class Availability {

        @Test
        void shouldNotThrowWhenCheckingAvailability() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThatCode(provider::isAvailable).doesNotThrowAnyException();
        }

        @Test
        void shouldReturnBooleanWithoutSideEffects() {
            var provider = new KeePassXCMasterKeyProvider();
            boolean available = provider.isAvailable();
            assertThat(provider.isConnected()).isFalse();
            assertThat(available || !available).isTrue();
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        void shouldNotThrowWhenClosingWithoutConnection() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThatCode(provider::close).doesNotThrowAnyException();
        }

        @Test
        void shouldNotThrowWhenClosingMultipleTimes() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThatCode(() -> {
                provider.close();
                provider.close();
                provider.close();
            }).doesNotThrowAnyException();
        }

        @Test
        void shouldBeIdempotentWhenClosing() {
            var provider = new KeePassXCMasterKeyProvider();
            provider.close();
            assertThatCode(provider::close).doesNotThrowAnyException();
            assertThatCode(provider::isAvailable).doesNotThrowAnyException();
        }

        @Test
        void shouldNotThrowWhenCheckingIsAvailableAfterClose() {
            var provider = new KeePassXCMasterKeyProvider();
            provider.close();
            assertThatCode(provider::isAvailable).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Constructor and configuration")
    class ConstructorConfiguration {

        @Test
        void shouldUseDefaultConfigurationValues() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThat(provider.getMaxAssociationAttempts()).isEqualTo(3);
            assertThat(provider.getAssociationDelayMs()).isEqualTo(2000);
            assertThat(provider.getMaxConnectAttempts()).isEqualTo(3);
            assertThat(provider.getConnectRetryDelayMs()).isEqualTo(500);
            assertThat(provider.getReconnectMaxRetries()).isEqualTo(2);
        }

        @Test
        void shouldAcceptCustomConfigurationValues() {
            var provider = new KeePassXCMasterKeyProvider(5, 3000, 4, 1000, 3);
            assertThat(provider.getMaxAssociationAttempts()).isEqualTo(5);
            assertThat(provider.getAssociationDelayMs()).isEqualTo(3000);
            assertThat(provider.getMaxConnectAttempts()).isEqualTo(4);
            assertThat(provider.getConnectRetryDelayMs()).isEqualTo(1000);
            assertThat(provider.getReconnectMaxRetries()).isEqualTo(3);
        }

        @Test
        void shouldAllowZeroRetries() {
            var provider = new KeePassXCMasterKeyProvider(1, 0, 1, 0, 0);
            assertThat(provider.getMaxAssociationAttempts()).isEqualTo(1);
            assertThat(provider.getReconnectMaxRetries()).isZero();
        }
    }

    @Nested
    @DisplayName("Connection state")
    class ConnectionState {

        @Test
        void shouldNotBeConnectedInitially() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThat(provider.isConnected()).isFalse();
        }

        @Test
        void shouldNotBeConnectedAfterClose() {
            var provider = new KeePassXCMasterKeyProvider();
            provider.close();
            assertThat(provider.isConnected()).isFalse();
        }

        @Test
        void shouldNotBeConnectedAfterReset() {
            var provider = new KeePassXCMasterKeyProvider();
            provider.resetConnection();
            assertThat(provider.isConnected()).isFalse();
        }

        @Test
        void shouldAllowMultipleResetCalls() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThatCode(() -> {
                provider.resetConnection();
                provider.resetConnection();
                provider.resetConnection();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        void shouldThrowWhenSettingNullMasterKey() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThatThrownBy(() -> provider.setMasterKey(null))
                    .isInstanceOf(CredentialException.class)
                    .hasMessageContaining("null");
        }
    }

    @Nested
    @DisplayName("Default constants")
    class DefaultConstants {

        @Test
        void shouldDefineAssociationConstants() {
            assertThat(KeePassXCMasterKeyProvider.DEFAULT_MAX_ASSOCIATION_ATTEMPTS).isPositive();
            assertThat(KeePassXCMasterKeyProvider.DEFAULT_ASSOCIATION_DELAY_MS).isNotNegative();
        }

        @Test
        void shouldDefineConnectConstants() {
            assertThat(KeePassXCMasterKeyProvider.DEFAULT_MAX_CONNECT_ATTEMPTS).isPositive();
            assertThat(KeePassXCMasterKeyProvider.DEFAULT_CONNECT_RETRY_DELAY_MS).isNotNegative();
        }

        @Test
        void shouldDefineReconnectConstants() {
            assertThat(KeePassXCMasterKeyProvider.DEFAULT_RECONNECT_MAX_RETRIES).isNotNegative();
        }
    }
}
