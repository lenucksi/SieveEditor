package de.febrildur.sieveeditor.system.credentials;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        @Test
        void shouldReturnFalseWhenKeyringCreationFails() {
            var provider = new OSKeychainMasterKeyProvider() {
                @Override
                Keyring createKeyring() throws BackendNotSupportedException {
                    throw new BackendNotSupportedException("No backend available");
                }
            };
            assertThat(provider.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Credential operations - native")
    @Tag("native")
    class NativeCredentialOperations {

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
    @DisplayName("Credential operations - fallback")
    class FallbackCredentialOperations {

        @Test
        void shouldThrowCredentialExceptionWhenKeyringUnavailableOnGet() {
            var provider = new OSKeychainMasterKeyProvider() {
                @Override
                Keyring createKeyring() throws BackendNotSupportedException {
                    throw new BackendNotSupportedException("No backend available");
                }
            };
            assertThatThrownBy(provider::getMasterKey)
                    .isInstanceOf(CredentialException.class)
                    .hasMessageContaining("Failed to initialize OS keychain");
        }

        @Test
        void shouldThrowCredentialExceptionWhenKeyringUnavailableOnSet() {
            var provider = new OSKeychainMasterKeyProvider() {
                @Override
                Keyring createKeyring() throws BackendNotSupportedException {
                    throw new BackendNotSupportedException("No backend available");
                }
            };
            assertThatThrownBy(() -> provider.setMasterKey("test-key"))
                    .isInstanceOf(CredentialException.class)
                    .hasMessageContaining("Failed to initialize OS keychain");
        }
    }

    @Nested
    @DisplayName("Lifecycle - native")
    @Tag("native")
    class NativeLifecycle {

        @Test
        void shouldNotThrowWhenClosing() {
            var provider = new OSKeychainMasterKeyProvider();
            assertThatCode(provider::close).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Lifecycle - fallback")
    class FallbackLifecycle {

        @Test
        void shouldNotThrowWhenClosingWithNoKeyring() {
            var provider = new OSKeychainMasterKeyProvider() {
                @Override
                Keyring createKeyring() throws BackendNotSupportedException {
                    throw new BackendNotSupportedException("No backend available");
                }
            };
            assertThatCode(provider::close).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Platform detection")
    class PlatformDetection {

        @Test
        void shouldDetectWindowsNameWhenOsPropContainsWin() {
            var provider = new OSKeychainMasterKeyProvider();
            String name = provider.getName();
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("win")) {
                assertThat(name).isEqualTo("Windows Credential Manager");
            } else if (osName.contains("mac")) {
                assertThat(name).isEqualTo("macOS Keychain");
            } else if (osName.contains("nux")) {
                assertThat(name).isEqualTo("Linux Secret Service");
            } else {
                assertThat(name).isEqualTo("System Keychain");
            }
        }
    }
}
