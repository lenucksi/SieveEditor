package de.febrildur.sieveeditor.system.credentials;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OSKeychainMasterKeyProviderTest {

    @Nested
    @DisplayName("Provider identity")
    class ProviderIdentity {

        @Test
        void shouldReturnPlatformSpecificName() {
            var provider = new OSKeychainMasterKeyProvider();
            assertThat(provider.getName()).isNotNull();
            assertThat(provider.getName()).isNotEmpty();
        }

        @Test
        void shouldReturnPlatformSpecificDescription() {
            var provider = new OSKeychainMasterKeyProvider();
            assertThat(provider.getDescription()).isNotNull();
            assertThat(provider.getDescription()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Availability")
    class Availability {

        @Test
        void shouldNotThrowWhenCheckingAvailability() {
            var provider = new OSKeychainMasterKeyProvider();
            assertThatCode(provider::isAvailable).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Credential operations")
    class CredentialOperations {

        @Test
        void shouldNotThrowWhenGettingMasterKey() {
            var provider = new OSKeychainMasterKeyProvider();
            assertThatCode(provider::getMasterKey).doesNotThrowAnyException();
        }

        @Test
        void shouldNotThrowWhenSettingMasterKey() {
            var provider = new OSKeychainMasterKeyProvider();
            assertThatCode(() -> provider.setMasterKey("test-key"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        void shouldNotThrowWhenClosing() {
            var provider = new OSKeychainMasterKeyProvider();
            assertThatCode(provider::close).doesNotThrowAnyException();
        }
    }
}
