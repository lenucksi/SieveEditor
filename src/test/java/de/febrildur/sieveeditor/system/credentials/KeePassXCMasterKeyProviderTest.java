package de.febrildur.sieveeditor.system.credentials;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        void shouldNotThrowWhenClosingWithoutConnection() {
            var provider = new KeePassXCMasterKeyProvider();
            assertThatCode(provider::close).doesNotThrowAnyException();
        }
    }
}
