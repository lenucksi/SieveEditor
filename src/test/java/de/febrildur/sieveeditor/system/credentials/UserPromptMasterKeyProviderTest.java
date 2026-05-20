package de.febrildur.sieveeditor.system.credentials;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class UserPromptMasterKeyProviderTest {

    @Nested
    @DisplayName("Provider identity")
    class ProviderIdentity {

        @Test
        void shouldReturnManualPasswordEntryAsName() {
            var provider = new UserPromptMasterKeyProvider();
            assertThat(provider.getName()).isEqualTo("Manual Password Entry");
        }

        @Test
        void shouldReturnDescription() {
            var provider = new UserPromptMasterKeyProvider();
            assertThat(provider.getDescription())
                    .contains("master password")
                    .contains("SieveEditor");
        }
    }

    @Nested
    @DisplayName("Availability")
    class Availability {

        @Test
        void shouldAlwaysBeAvailable() {
            var provider = new UserPromptMasterKeyProvider();
            assertThat(provider.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Key caching")
    class KeyCaching {

        @Test
        void shouldReturnCachedKeyAfterSetMasterKey() throws CredentialException {
            var provider = new UserPromptMasterKeyProvider();
            provider.setMasterKey("my-cached-key");
            assertThat(provider.getMasterKey()).isEqualTo("my-cached-key");
        }

        @Test
        void shouldOverwritePreviouslyCachedKey() throws CredentialException {
            var provider = new UserPromptMasterKeyProvider();
            provider.setMasterKey("first-key");
            provider.setMasterKey("second-key");
            assertThat(provider.getMasterKey()).isEqualTo("second-key");
        }

        @Test
        void shouldPersistKeyAcrossMultipleGetCalls() throws CredentialException {
            var provider = new UserPromptMasterKeyProvider();
            provider.setMasterKey("persistent-key");
            assertThat(provider.getMasterKey()).isEqualTo("persistent-key");
            assertThat(provider.getMasterKey()).isEqualTo("persistent-key");
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        void shouldAllowNewKeyToBeSetAfterClose() throws CredentialException {
            var provider = new UserPromptMasterKeyProvider();
            provider.setMasterKey("sensitive-key");
            provider.close();
            provider.setMasterKey("replacement-key");
            assertThat(provider.getMasterKey()).isEqualTo("replacement-key");
        }

        @Test
        void shouldNotThrowWhenClosingWithoutCachedKey() {
            var provider = new UserPromptMasterKeyProvider();
            assertThatCode(provider::close).doesNotThrowAnyException();
        }

        @Test
        void shouldClearCachedKeyOnClose() throws Exception {
            var provider = new UserPromptMasterKeyProvider();
            provider.setMasterKey("some-key");
            provider.close();
            var field = UserPromptMasterKeyProvider.class.getDeclaredField("cachedMasterKey");
            field.setAccessible(true);
            assertThat(field.get(provider)).isNull();
        }
    }
}
