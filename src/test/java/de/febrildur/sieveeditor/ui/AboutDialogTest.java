package de.febrildur.sieveeditor.ui;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AboutDialog")
@Tag("gui")
class AboutDialogTest {

    @Nested
    @DisplayName("app constants")
    class AppConstantsTests {

        @Test
        void shouldHaveApplicationName() {
            assertThat(AboutDialog.APP_NAME).isEqualTo("SieveEditor");
        }

        @Test
        void shouldHaveVersion() {
            assertThat(AboutDialog.APP_VERSION).isEqualTo("1.3.1-SNAPSHOT");
        }

        @Test
        void shouldHaveDescription() {
            assertThat(AboutDialog.APP_DESCRIPTION)
                .contains("Sieve")
                .contains("ManageSieve");
        }

        @Test
        void shouldHaveGitHubUrl() {
            assertThat(AboutDialog.GITHUB_URL)
                .startsWith("https://github.com/lenucksi/SieveEditor");
        }

        @Test
        void shouldHaveCopyright() {
            assertThat(AboutDialog.COPYRIGHT).contains("SieveEditor contributors");
        }

        @Test
        void shouldHaveLicenseInfo() {
            assertThat(AboutDialog.LICENSE_INFO).contains("Apache License");
        }
    }

    @Nested
    @DisplayName("dependencies list")
    class DependenciesTests {

        @Test
        void shouldHaveDependencies() {
            assertThat(AboutDialog.DEPENDENCIES).isNotEmpty();
        }

        @Test
        void eachDependencyShouldHaveName() {
            for (String[] dep : AboutDialog.DEPENDENCIES) {
                assertThat(dep[0]).isNotEmpty();
            }
        }

        @Test
        void eachDependencyShouldHaveCoordinates() {
            for (String[] dep : AboutDialog.DEPENDENCIES) {
                assertThat(dep[1]).contains(":");
            }
        }

        @Test
        void eachDependencyShouldHaveLicense() {
            for (String[] dep : AboutDialog.DEPENDENCIES) {
                assertThat(dep[2]).isNotEmpty();
            }
        }

        @Test
        void shouldIncludeRSyntaxTextArea() {
            assertThat(dependencyNames()).contains("RSyntaxTextArea");
        }

        @Test
        void shouldIncludeFlatLaf() {
            assertThat(dependencyNames()).contains("FlatLaf");
        }

        @Test
        void shouldIncludeManageSieveJ() {
            assertThat(dependencyNames()).contains("ManageSieveJ");
        }

        @Test
        void shouldIncludeJasypt() {
            assertThat(dependencyNames()).contains("Jasypt");
        }

        @Test
        void shouldIncludeCommonsCodec() {
            assertThat(dependencyNames()).contains("Commons Codec");
        }

        @Test
        void shouldIncludeJavaKeyring() {
            assertThat(dependencyNames()).contains("java-keyring");
        }

        private static String[] dependencyNames() {
            String[] names = new String[AboutDialog.DEPENDENCIES.length];
            for (int i = 0; i < AboutDialog.DEPENDENCIES.length; i++) {
                names[i] = AboutDialog.DEPENDENCIES[i][0];
            }
            return names;
        }
    }
}
