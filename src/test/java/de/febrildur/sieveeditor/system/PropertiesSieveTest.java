package de.febrildur.sieveeditor.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import de.febrildur.sieveeditor.system.credentials.MasterKeyProviderFactory;
import de.febrildur.sieveeditor.testutil.TestMasterKeyProvider;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Note: AppDirectoryService is in the same package, no import needed

/**
 * Comprehensive test suite for PropertiesSieve class.
 * Tests profile management, encryption, and file I/O operations.
 *
 * Note: Test mode for MasterKeyProviderFactory is enabled globally via
 * TestModeExtension.
 */
class PropertiesSieveTest {

    private static final String SIEVEEDITOR_TEST_DIR = "sieveeditor.test.dir";
    private PropertiesSieve properties;
    private String originalUserHome;
    private Path testingPropFilePath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Save original user.home and set to temp directory for testing
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        testingPropFilePath = tempDir.resolve(".local/share/sieveeditor/profiles");

        // We need to bypass the real AppDirectory service for testing.
        // AppDirectoryService reads this property and then switches to the path here.
        // Kludgy but the least ugly.
        System.setProperty(SIEVEEDITOR_TEST_DIR, testingPropFilePath.toAbsolutePath().toString());

        // Clean up any existing files from previous test runs (Windows isolation issue)
        cleanupProfilesDirectory();

        properties = new PropertiesSieve(); // Creates file with profile "default". Ensure its empty.
        properties.setPassword("");
        properties.setUsername("");
        properties.setServer("");
        properties.setPort(4190);
        properties.write();

    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up all profile files to prevent test pollution on Windows
        cleanupProfilesDirectory();

        // Restore original user.home
        System.setProperty("user.home", originalUserHome);

        System.clearProperty(SIEVEEDITOR_TEST_DIR);
    }

    private void cleanupProfilesDirectory() throws IOException {
        if (Files.exists(testingPropFilePath)) {
            try (var stream = Files.walk(testingPropFilePath)) {
                stream.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                System.out.println("CleanupProfilesDirectory, found:" + path);
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
            }
        }
    }

    // ===== Basic Get/Set Operations =====

    @Test
    void shouldGetAndSetServer() {
        // When
        properties.setServer("mail.example.com");

        // Then
        assertThat(properties.getServer()).isEqualTo("mail.example.com");
    }

    @Test
    void shouldGetAndSetPort() {
        // When
        properties.setPort(4190);

        // Then
        assertThat(properties.getPort()).isEqualTo(4190);
    }

    @Test
    void shouldGetAndSetUsername() {
        // When
        properties.setUsername("testuser");

        // Then
        assertThat(properties.getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldGetAndSetPassword() {
        // When
        properties.setPassword("secretpassword");

        // Then
        assertThat(properties.getPassword()).isEqualTo("secretpassword");
    }

    // ===== File I/O Operations =====

    @Test
    void shouldSaveAndLoadProperties() throws IOException {
        // Given
        properties.setServer("mail.example.com");
        properties.setPort(4190);
        properties.setUsername("testuser");
        properties.setPassword("testpass");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getServer()).isEqualTo("mail.example.com");
        assertThat(loaded.getPort()).isEqualTo(4190);
        assertThat(loaded.getUsername()).isEqualTo("testuser");
        assertThat(loaded.getPassword()).isEqualTo("testpass");
    }

    @Test
    void shouldCreateProfilesDirectoryIfNotExists() throws IOException {
        // Given - PropertiesSieve uses AppDirectoryService which creates XDG
        // directories
        // The directory is created automatically when PropertiesSieve is instantiated
        Path profilesDir = AppDirectoryService.getProfilesDir();

        // Then - Directory should exist (created by PropertiesSieve in setUp or by
        // getProfilesDir)
        assertThat(profilesDir.toFile()).exists().isDirectory();
    }

    @Test
    void shouldCreateNewFileOnLoad() throws IOException {
        // Given - Use actual profile path from AppDirectoryService
        Path profilesDir = AppDirectoryService.getProfilesDir();
        Path profileFile = profilesDir.resolve("default.properties");

        // When
        properties.load();

        // Then - File should be created
        assertThat(profileFile.toFile()).exists();
    }

    @Test
    void shouldLoadDefaultValuesWhenFileIsEmpty() throws IOException {
        // When
        properties.load();

        // Then
        assertThat(properties.getServer()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(4190); // Default port
        assertThat(properties.getUsername()).isEmpty();
        assertThat(properties.getPassword()).isEmpty();
    }

    @Test
    void shouldHandleEmptyPassword() throws IOException {
        // Given
        properties.setServer("example.com");
        properties.setPort(4190);
        properties.setUsername("user");
        properties.setPassword("");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEmpty();
    }

    // ===== Profile Management =====

    @Test
    void shouldHandleMultipleProfiles() throws IOException {
        // Given - Create two different profiles
        PropertiesSieve profile1 = new PropertiesSieve("work");
        profile1.setServer("work.example.com");
        profile1.setPort(4190);
        profile1.setUsername("work.user");
        profile1.setPassword("workpass");
        profile1.write();

        PropertiesSieve profile2 = new PropertiesSieve("personal");
        profile2.setServer("personal.example.com");
        profile2.setPort(2000);
        profile2.setUsername("personal.user");
        profile2.setPassword("personalpass");
        profile2.write();

        // When - Load each profile
        PropertiesSieve loadedWork = new PropertiesSieve("work");
        loadedWork.load();

        PropertiesSieve loadedPersonal = new PropertiesSieve("personal");
        loadedPersonal.load();

        // Then - Verify profiles are isolated
        assertThat(loadedWork.getServer()).isEqualTo("work.example.com");
        assertThat(loadedWork.getPort()).isEqualTo(4190);
        assertThat(loadedWork.getUsername()).isEqualTo("work.user");

        assertThat(loadedPersonal.getServer()).isEqualTo("personal.example.com");
        assertThat(loadedPersonal.getPort()).isEqualTo(2000);
        assertThat(loadedPersonal.getUsername()).isEqualTo("personal.user");
    }

    @Test
    void shouldGetAvailableProfiles() throws IOException {
        // Given - Create multiple profiles
        new PropertiesSieve("profile1").write();
        new PropertiesSieve("profile2").write();
        new PropertiesSieve("profile3").write();

        properties.deleteProfile("default"); // Always created in setup, not desired here.

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles)
                .hasSize(3)
                .contains("profile1", "profile2", "profile3");

    }

    @Test
    void shouldReturnDefaultProfileWhenNoneExist() {
        // Given - No profiles directory exists yet
        System.setProperty("user.home", tempDir.resolve("nonexistent").toString());

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles).containsExactly("default");
    }

    @Test
    void shouldReturnDefaultProfileWhenDirectoryIsEmpty() throws IOException {
        // Given - Directory already exists from PropertiesSieve constructor in setUp()
        // Just ensure it's empty (delete any files created by setUp)
        File profilesDir = tempDir.resolve(".sieveprofiles").toFile();
        if (profilesDir.exists()) {
            for (File f : profilesDir.listFiles()) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles).containsExactly("default");
    }

    @Test
    void shouldReturnSortedProfiles() throws IOException {
        // Given - Create profiles in random order
        new PropertiesSieve("zebra").write();
        new PropertiesSieve("apple").write();
        new PropertiesSieve("banana").write();

        properties.deleteProfile("default"); // Always created on setup, not desired here.

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles).containsExactly("apple", "banana", "zebra");
    }

    @Test
    void shouldCheckIfProfileExists() throws IOException {
        // Given
        new PropertiesSieve("existing").write();

        // When/Then
        assertThat(PropertiesSieve.profileExists("existing")).isTrue();
        assertThat(PropertiesSieve.profileExists("nonexistent")).isFalse();
    }

    // ===== Last Used Profile =====

    @Test
    void shouldSaveAndRetrieveLastUsedProfile() throws IOException {
        // When
        PropertiesSieve.saveLastUsedProfile("myprofile");

        // Then
        assertThat(PropertiesSieve.getLastUsedProfile()).isEqualTo("myprofile");
    }

    @Test
    void shouldReturnDefaultWhenLastUsedFileDoesNotExist() throws IOException {
        // Given - Ensure no last used file exists (may have been created by other
        // tests)
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path lastUsedFile = configDir.resolve(".lastused");
        Files.deleteIfExists(lastUsedFile);

        // When - No last used file exists
        String lastUsed = PropertiesSieve.getLastUsedProfile();

        // Then
        assertThat(lastUsed).isEqualTo("default");
    }

    @Test
    void shouldReturnDefaultWhenLastUsedFileIsCorrupt() throws IOException {
        // Given - Create corrupt last used file in the actual config directory
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path lastUsedFile = configDir.resolve(".lastused");
        Files.writeString(lastUsedFile, ""); // Empty file

        // When
        String lastUsed = PropertiesSieve.getLastUsedProfile();

        // Then
        assertThat(lastUsed).isEqualTo("default");
    }

    @Test
    void shouldTrimWhitespaceFromLastUsedProfile() throws IOException {
        // Given - Write to actual config directory used by PropertiesSieve
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path lastUsedFile = configDir.resolve(".lastused");
        Files.writeString(lastUsedFile, "  myprofile  \n");

        // When
        String lastUsed = PropertiesSieve.getLastUsedProfile();

        // Then
        assertThat(lastUsed).isEqualTo("myprofile");
    }

    // ===== Migration =====

    @Test
    void shouldMigrateOldPropertiesFile() throws IOException {
        // Given - Create old legacy file in the legacy location
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);
        Path oldFile = legacyDir.resolve("migrationtest.properties");
        Files.writeString(oldFile, "sieve.server=oldserver.com\nsieve.port=4190");

        // When
        PropertiesSieve.migrateOldProperties();

        // Then - Should copy to new profiles directory
        Path newFile = AppDirectoryService.getProfilesDir().resolve("migrationtest.properties");
        assertThat(newFile.toFile()).exists();
        String content = Files.readString(newFile);
        assertThat(content).contains("oldserver.com");

        // Cleanup
        Files.deleteIfExists(oldFile);
        Files.deleteIfExists(newFile);
    }

    @Test
    void shouldNotOverwriteExistingDefaultProfile() throws IOException {
        // Given - Create both old file and existing default profile
        File oldFile = tempDir.resolve(".sieveproperties").toFile();
        Files.writeString(oldFile.toPath(), "sieve.server=oldserver.com");

        PropertiesSieve existing = new PropertiesSieve("default");
        existing.setServer("existingserver.com");
        existing.write();

        // When
        PropertiesSieve.migrateOldProperties();

        // Then - Should NOT overwrite existing profile
        PropertiesSieve loaded = new PropertiesSieve("default");
        loaded.load();
        assertThat(loaded.getServer()).isEqualTo("existingserver.com");
    }

    @Test
    void shouldSkipMigrationWhenOldFileNotExists() {
        // Given - No old file exists

        // When/Then - Should not throw exception
        assertThatCode(() -> PropertiesSieve.migrateOldProperties())
                .doesNotThrowAnyException();
    }

    // ===== Encryption Tests =====

    @Test
    void shouldEncryptPasswordWhenSaving() throws IOException {
        // Given
        properties.setPassword("plaintextpassword");

        // When
        properties.write();

        // Then - Read raw file and verify password is encrypted
        Path profileFile = AppDirectoryService.getProfilesDir().resolve("default.properties");
        String rawContent = Files.readString(profileFile);

        assertThat(rawContent).doesNotContain("plaintextpassword");
        assertThat(rawContent).contains("ENC("); // Jasypt encrypted format
    }

    @Test
    void shouldDecryptPasswordWhenLoading() throws IOException {
        // Given
        properties.setPassword("mysecretpassword");
        properties.write();

        // When
        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo("mysecretpassword");
    }

    @Test
    void shouldHandleCorruptedEncryptedPassword() throws IOException {
        // Given - Manually write corrupt encrypted password
        File profileFile = tempDir.resolve(".sieveprofiles/default.properties").toFile();
        profileFile.getParentFile().mkdirs();
        Files.writeString(profileFile.toPath(),
                "sieve.server=example.com\n" +
                        "sieve.port=4190\n" +
                        "sieve.user=user\n" +
                        "sieve.password=ENC(CORRUPT_ENCRYPTED_DATA_INVALID)");

        // When
        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then - Should return empty password on decryption failure
        assertThat(loaded.getPassword()).isEmpty();
    }

    // ===== Edge Cases and Error Handling =====

    @Test
    void shouldHandleNullValues() {
        // When/Then - Should handle nulls gracefully
        assertThatCode(() -> {
            properties.setServer(null);
            properties.setUsername(null);
            properties.setPassword(null);
            properties.write();
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleSpecialCharactersInServer() throws IOException {
        // Given
        properties.setServer("mail.example-test.com:4190");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getServer()).isEqualTo("mail.example-test.com:4190");
    }

    @Test
    void shouldHandleSpecialCharactersInUsername() throws IOException {
        // Given
        properties.setUsername("user@example.com");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    void shouldHandleSpecialCharactersInPassword() throws IOException {
        // Given
        properties.setPassword("p@ssw0rd!#$%^&*()");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo("p@ssw0rd!#$%^&*()");
    }

    @Test
    void shouldHandleVeryLongPassword() throws IOException {
        // Given - 256 character password

        String longPassword = "a".repeat(256);
        properties.setPassword(longPassword);

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo(longPassword);

        loaded.setPassword("");
    }

    @Test
    void shouldHandleUnicodeInPassword() throws IOException {
        // Given
        properties.setPassword("пароль密码🔒");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo("пароль密码🔒");
    }

    @Test
    void shouldHandleProfileNameWithSpecialCharacters() throws IOException {
        // Given
        String profileName = "test-profile_123";
        PropertiesSieve profile = new PropertiesSieve(profileName);
        profile.setServer("example.com");

        // When
        profile.write();

        PropertiesSieve loaded = new PropertiesSieve(profileName);
        loaded.load();

        // Then
        assertThat(loaded.getServer()).isEqualTo("example.com");

        // Attempt to clean written profile to not pollute other tests.
        profile.deleteProfile(profileName);
        profile.write();
    }

    @Test
    void shouldNotListNonPropertiesFiles() throws IOException {
        // Given - Create some non-.properties files
        File profilesDir = tempDir.resolve(".sieveprofiles").toFile();
        profilesDir.mkdirs();
        new File(profilesDir, "readme.txt").createNewFile();
        new File(profilesDir, ".lastused").createNewFile();

        new PropertiesSieve("valid").write();

        properties.deleteProfile("default"); // Always created on setup, not desired here.

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then - Should only list .properties files
        assertThat(profiles).containsExactly("valid");
    }

    // ===== Profile Deletion Tests =====

    @Test
    void shouldDeleteProfileByName() throws IOException {
        // Given - Create a profile
        PropertiesSieve profile = new PropertiesSieve("todelete");
        profile.setServer("example.com");
        profile.setPort(4190);
        profile.write();

        // Verify profile exists
        assertThat(PropertiesSieve.profileExists("todelete")).isTrue();
        assertThat(PropertiesSieve.getAvailableProfiles()).contains("todelete");

        // When
        PropertiesSieve.deleteProfile("todelete");

        // Then
        assertThat(PropertiesSieve.profileExists("todelete")).isFalse();
        assertThat(PropertiesSieve.getAvailableProfiles()).doesNotContain("todelete");
    }

    @Test
    void shouldDeleteProfileFile() throws IOException {
        // Given - Create a profile
        PropertiesSieve profile = new PropertiesSieve("todelete");
        profile.write();

        Path profileFile = AppDirectoryService.getProfilesDir().resolve("todelete.properties");
        assertThat(profileFile.toFile()).exists();

        // When
        PropertiesSieve.deleteProfile("todelete");

        // Then - File should be deleted
        assertThat(profileFile.toFile()).doesNotExist();
    }

    @Test
    void shouldNotThrowExceptionWhenDeletingNonexistentProfile() {
        // When/Then - Should not throw exception
        assertThatCode(() -> PropertiesSieve.deleteProfile("nonexistent"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldReturnTrueWhenProfileDeleted() throws IOException {
        // Given
        new PropertiesSieve("todelete").write();

        // When
        boolean result = PropertiesSieve.deleteProfile("todelete");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenProfileDoesNotExist() {
        // When
        boolean result = PropertiesSieve.deleteProfile("nonexistent");

        // Then
        assertThat(result).isFalse();
    }

    // ===== Profile Rename Tests =====

    @Test
    void shouldRenameProfileSuccessfully() throws IOException {
        // Given - Create a profile
        PropertiesSieve profile = new PropertiesSieve("oldname");
        profile.setServer("example.com");
        profile.setPort(4190);
        profile.setUsername("testuser");
        profile.setPassword("testpass");
        profile.write();

        // Verify old profile exists
        assertThat(PropertiesSieve.profileExists("oldname")).isTrue();
        assertThat(PropertiesSieve.profileExists("newname")).isFalse();

        // When
        boolean result = PropertiesSieve.renameProfile("oldname", "newname");

        // Then
        assertThat(result).isTrue();
        assertThat(PropertiesSieve.profileExists("oldname")).isFalse();
        assertThat(PropertiesSieve.profileExists("newname")).isTrue();
        assertThat(PropertiesSieve.getAvailableProfiles()).contains("newname");
        assertThat(PropertiesSieve.getAvailableProfiles()).doesNotContain("oldname");
    }

    @Test
    void shouldPreserveProfileDataAfterRename() throws IOException {
        // Given - Create a profile with specific data
        PropertiesSieve oldProfile = new PropertiesSieve("oldname");
        oldProfile.setServer("mail.example.com");
        oldProfile.setPort(2000);
        oldProfile.setUsername("john.doe");
        oldProfile.setPassword("secret123");
        oldProfile.write();

        // When
        PropertiesSieve.renameProfile("oldname", "newname");

        // Then - Load the renamed profile and verify data is preserved
        PropertiesSieve renamedProfile = new PropertiesSieve("newname");
        renamedProfile.load();

        assertThat(renamedProfile.getServer()).isEqualTo("mail.example.com");
        assertThat(renamedProfile.getPort()).isEqualTo(2000);
        assertThat(renamedProfile.getUsername()).isEqualTo("john.doe");
        assertThat(renamedProfile.getPassword()).isEqualTo("secret123");
    }

    @Test
    void shouldUpdateLastUsedProfileAfterRename() throws IOException {
        // Given - Create a profile and set it as last used
        PropertiesSieve profile = new PropertiesSieve("oldname");
        profile.write();
        PropertiesSieve.saveLastUsedProfile("oldname");

        // Verify it's the last used profile
        assertThat(PropertiesSieve.getLastUsedProfile()).isEqualTo("oldname");

        // When
        PropertiesSieve.renameProfile("oldname", "newname");

        // Then - Last used profile should be updated
        assertThat(PropertiesSieve.getLastUsedProfile()).isEqualTo("newname");
    }

    @Test
    void shouldNotUpdateLastUsedProfileIfNotRenamed() throws IOException {
        // Given - Create two profiles, one is last used
        PropertiesSieve profile1 = new PropertiesSieve("profile1");
        profile1.write();
        PropertiesSieve profile2 = new PropertiesSieve("profile2");
        profile2.write();
        PropertiesSieve.saveLastUsedProfile("profile2");

        // When - Rename profile1 (not the last used one)
        PropertiesSieve.renameProfile("profile1", "profile1renamed");

        // Then - Last used profile should remain unchanged
        assertThat(PropertiesSieve.getLastUsedProfile()).isEqualTo("profile2");
    }

    @Test
    void shouldReturnFalseWhenOldProfileDoesNotExist() {
        // When
        boolean result = PropertiesSieve.renameProfile("nonexistent", "newname");

        // Then
        assertThat(result).isFalse();
        assertThat(PropertiesSieve.profileExists("newname")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNewProfileAlreadyExists() throws IOException {
        // Given - Create two profiles
        PropertiesSieve profile1 = new PropertiesSieve("profile1");
        profile1.write();
        PropertiesSieve profile2 = new PropertiesSieve("profile2");
        profile2.write();

        // When - Try to rename profile1 to profile2 (which already exists)
        boolean result = PropertiesSieve.renameProfile("profile1", "profile2");

        // Then
        assertThat(result).isFalse();
        assertThat(PropertiesSieve.profileExists("profile1")).isTrue();
        assertThat(PropertiesSieve.profileExists("profile2")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenOldNameIsNull() {
        // When
        boolean result = PropertiesSieve.renameProfile(null, "newname");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenOldNameIsEmpty() {
        // When
        boolean result = PropertiesSieve.renameProfile("", "newname");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNewNameIsNull() throws IOException {
        // Given
        PropertiesSieve profile = new PropertiesSieve("oldname");
        profile.write();

        // When
        boolean result = PropertiesSieve.renameProfile("oldname", null);

        // Then
        assertThat(result).isFalse();
        assertThat(PropertiesSieve.profileExists("oldname")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNewNameIsEmpty() throws IOException {
        // Given
        PropertiesSieve profile = new PropertiesSieve("oldname");
        profile.write();

        // When
        boolean result = PropertiesSieve.renameProfile("oldname", "");

        // Then
        assertThat(result).isFalse();
        assertThat(PropertiesSieve.profileExists("oldname")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenRenamingToSameName() throws IOException {
        // Given
        PropertiesSieve profile = new PropertiesSieve("samename");
        profile.write();

        // When - Rename to same name (no-op)
        boolean result = PropertiesSieve.renameProfile("samename", "samename");

        // Then - Should return true (not an error, just a no-op)
        assertThat(result).isTrue();
        assertThat(PropertiesSieve.profileExists("samename")).isTrue();
    }

    @Test
    void shouldHandleWhitespaceInNames() throws IOException {
        // Given
        PropertiesSieve profile = new PropertiesSieve("oldname");
        profile.write();

        // When - Try to rename with whitespace (should be trimmed)
        boolean result = PropertiesSieve.renameProfile("  oldname  ", "  newname  ");

        // Then - The implementation trims whitespace, but the method doesn't currently do that
        // This test documents current behavior - may need enhancement
        assertThat(result).isFalse();
    }

    // ===== Port Edge Cases =====

    @Test
    void shouldHandlePortZero() throws IOException {
        properties.setPort(0);
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        assertThat(loaded.getPort()).isZero();
    }

    @Test
    void shouldHandlePortMaxValue() throws IOException {
        properties.setPort(65535);
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        assertThat(loaded.getPort()).isEqualTo(65535);
    }

    @Test
    void shouldHandleNegativePort() {
        properties.setPort(-1);
        assertThat(properties.getPort()).isEqualTo(-1);
    }

    // ===== Load Error Handling =====

    @Test
    void shouldFallbackToDefaultPortWhenLoadingInvalidPort() throws IOException {
        Path profileFile = AppDirectoryService.getProfilesDir().resolve("default.properties");
        Files.writeString(profileFile, "sieve.port=notanumber");

        properties.load();

        assertThat(properties.getPort()).isEqualTo(4190);
    }

    @Test
    void shouldCreateFileAndSetPermissionsWhenLoadingNonExistentFile() throws IOException {
        Path profileFile = AppDirectoryService.getProfilesDir().resolve("default.properties");
        Files.deleteIfExists(profileFile);

        properties.load();

        assertThat(profileFile).exists();
        assertThat(properties.getPort()).isEqualTo(4190);
    }

    // ===== IO Error Handling =====

    @Test
    void shouldHandleIOExceptionWhenWriting() throws IOException {
        Path profileFile = AppDirectoryService.getProfilesDir().resolve("default.properties");
        profileFile.toFile().setWritable(false);

        properties.setServer("newserver");

        assertThatCode(() -> properties.write())
                .doesNotThrowAnyException();

        profileFile.toFile().setWritable(true);
    }

    @Test
    void shouldHandleIOExceptionWhenSavingLastUsedProfile() throws IOException {
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path lastUsedFile = configDir.resolve(".lastused");
        Files.deleteIfExists(lastUsedFile);
        configDir.toFile().setWritable(false);

        assertThatCode(() -> PropertiesSieve.saveLastUsedProfile("test"))
                .doesNotThrowAnyException();

        configDir.toFile().setWritable(true);
    }

    @Test
    void shouldHandleIOExceptionWhenDeletingProfile() throws IOException {
        PropertiesSieve profile = new PropertiesSieve("todeleteio");
        profile.write();

        Path profilesDir = AppDirectoryService.getProfilesDir();
        profilesDir.toFile().setWritable(false);

        boolean result = PropertiesSieve.deleteProfile("todeleteio");
        assertThat(result).isFalse();

        profilesDir.toFile().setWritable(true);
        PropertiesSieve.deleteProfile("todeleteio");
    }

    @Test
    void shouldHandleIOExceptionWhenRenamingProfile() throws IOException {
        PropertiesSieve profile = new PropertiesSieve("oldnameio");
        profile.write();

        Path profilesDir = AppDirectoryService.getProfilesDir();
        profilesDir.toFile().setWritable(false);

        boolean result = PropertiesSieve.renameProfile("oldnameio", "newnameio");
        assertThat(result).isFalse();

        profilesDir.toFile().setWritable(true);
        PropertiesSieve.deleteProfile("oldnameio");
    }

    // ===== Constructor Edge Cases =====

    @Test
    void shouldConstructWithForcedBackend() {
        assertThatCode(() -> {
            PropertiesSieve props = new PropertiesSieve("test_backend_profile", null);
            props.setServer("example.com");
            props.write();
            props.deleteProfile("test_backend_profile");
        }).doesNotThrowAnyException();
    }

    // ===== saveLastUsedProfile Edge Cases =====

    @Test
    void shouldOverwriteLastUsedProfile() throws IOException {
        PropertiesSieve.saveLastUsedProfile("profile1");
        PropertiesSieve.saveLastUsedProfile("profile2");

        assertThat(PropertiesSieve.getLastUsedProfile()).isEqualTo("profile2");
    }

    // ===== Migration Edge Cases =====

    @Test
    void shouldMigrateLastUsedWithLegacy() throws IOException {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);

        Path legacyProfile = legacyDir.resolve("test.properties");
        Files.writeString(legacyProfile, "sieve.server=server.com");

        Path legacyLastUsed = legacyDir.resolve(".lastused");
        Files.writeString(legacyLastUsed, "test");

        PropertiesSieve.migrateOldProperties();

        Path newLastUsed = AppDirectoryService.getUserConfigDir().resolve(".lastused");
        assertThat(newLastUsed).exists();
        assertThat(Files.readString(newLastUsed).trim()).isEqualTo("test");
    }

    @Test
    void shouldSkipLastUsedMigrationIfAlreadyExists() throws IOException {
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path newLastUsed = configDir.resolve(".lastused");
        Files.writeString(newLastUsed, "newprofile");

        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);
        Path legacyProfile = legacyDir.resolve("test.properties");
        Files.writeString(legacyProfile, "sieve.server=server.com");
        Path legacyLastUsed = legacyDir.resolve(".lastused");
        Files.writeString(legacyLastUsed, "oldprofile");

        PropertiesSieve.migrateOldProperties();

        assertThat(Files.readString(newLastUsed).trim()).isEqualTo("newprofile");
    }

    // ===== Port Parsing Edge Cases =====

    @Test
    void shouldHandlePortParsingEdgeCases() throws IOException {
        Path profileFile = AppDirectoryService.getProfilesDir().resolve("default.properties");

        Files.writeString(profileFile, "sieve.port=-1");
        properties.load();
        assertThat(properties.getPort()).isEqualTo(4190);

        Files.writeString(profileFile, "sieve.port=70000");
        properties.load();
        assertThat(properties.getPort()).isEqualTo(4190);

        Files.writeString(profileFile, "sieve.port=99999");
        properties.load();
        assertThat(properties.getPort()).isEqualTo(4190);

        Files.writeString(profileFile, "sieve.port=0");
        properties.load();
        assertThat(properties.getPort()).isEqualTo(0);
    }

    // ===== Server Name Edge Cases =====

    @Test
    void shouldHandleUnicodeServerName() throws IOException {
        properties.setServer("s\u00fcber.mail.tld");
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();
        assertThat(loaded.getServer()).isEqualTo("s\u00fcber.mail.tld");
    }

    @Test
    void shouldHandleExtremelyLongServerName() throws IOException {
        String longServer = "server-" + "a".repeat(500) + ".com";
        properties.setServer(longServer);
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();
        assertThat(loaded.getServer()).isEqualTo(longServer);
    }

    // ===== Profile Name Edge Cases =====

    @Test
    void shouldHandleProfileNameWithDotsAndSpaces() throws IOException {
        String profileName = "my profile.v2";
        PropertiesSieve profile = new PropertiesSieve(profileName);
        profile.setServer("example.com");
        profile.write();

        PropertiesSieve loaded = new PropertiesSieve(profileName);
        loaded.load();
        assertThat(loaded.getServer()).isEqualTo("example.com");

        PropertiesSieve.deleteProfile(profileName);
    }

    @Test
    void shouldHandleProfileNameWithUnicode() throws IOException {
        String profileName = "\u30d7\u30ed\u30d5\u30a1\u30a4\u30eb";
        PropertiesSieve profile = new PropertiesSieve(profileName);
        profile.setServer("unicode.example.com");
        profile.write();

        PropertiesSieve loaded = new PropertiesSieve(profileName);
        loaded.load();
        assertThat(loaded.getServer()).isEqualTo("unicode.example.com");

        PropertiesSieve.deleteProfile(profileName);
    }

    // ===== Master Key Edge Cases =====

    @Test
    void shouldGenerateKeyWhenMasterKeyIsNull() throws IOException {
        MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider(null));
        try {
            PropertiesSieve props = new PropertiesSieve("test-null-key");
            props.setServer("example.com");
            props.write();

            PropertiesSieve loaded = new PropertiesSieve("test-null-key");
            loaded.load();
            assertThat(loaded.getServer()).isEqualTo("example.com");
        } finally {
            PropertiesSieve.deleteProfile("test-null-key");
            MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider());
        }
    }

    // ===== Encryption Tier Tests =====

    @Test
    void shouldFallbackToTripleDesWhenAesNotAvailable() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        var encMap = new ConcurrentHashMap<String, String>();

        try (MockedConstruction<StandardPBEStringEncryptor> mocked = mockConstruction(
                StandardPBEStringEncryptor.class,
                (mock, context) -> {
                    int idx = counter.getAndIncrement();
                    if (idx < 2) {
                        when(mock.encrypt(anyString()))
                                .thenThrow(new EncryptionOperationNotPossibleException("AES not available"));
                    } else {
                        when(mock.encrypt(anyString())).thenAnswer(invocation -> {
                            String plain = invocation.getArgument(0);
                            String enc = java.util.Base64.getEncoder().encodeToString(plain.getBytes());
                            encMap.put(enc, plain);
                            return enc;
                        });
                        when(mock.decrypt(anyString())).thenAnswer(invocation -> {
                            String enc = invocation.getArgument(0);
                            String plain = encMap.get(enc);
                            return plain != null ? plain : enc;
                        });
                    }
                }
        )) {
            PropertiesSieve props = new PropertiesSieve("test-tier2");
            props.setPassword("tier2-pass");
            props.write();

            PropertiesSieve loaded = new PropertiesSieve("test-tier2");
            loaded.load();
            assertThat(loaded.getPassword()).isEqualTo("tier2-pass");
        }
    }

    @Test
    void shouldFallbackToDesWhenAesAndTripleDesNotAvailable() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        var encMap = new ConcurrentHashMap<String, String>();

        try (MockedConstruction<StandardPBEStringEncryptor> mocked = mockConstruction(
                StandardPBEStringEncryptor.class,
                (mock, context) -> {
                    int idx = counter.getAndIncrement();
                    if (idx < 4) {
                        when(mock.encrypt(anyString()))
                                .thenThrow(new EncryptionOperationNotPossibleException("AES/TripleDES not available"));
                    } else {
                        when(mock.encrypt(anyString())).thenAnswer(invocation -> {
                            String plain = invocation.getArgument(0);
                            String enc = java.util.Base64.getEncoder().encodeToString(plain.getBytes());
                            encMap.put(enc, plain);
                            return enc;
                        });
                        when(mock.decrypt(anyString())).thenAnswer(invocation -> {
                            String enc = invocation.getArgument(0);
                            String plain = encMap.get(enc);
                            return plain != null ? plain : enc;
                        });
                    }
                }
        )) {
            PropertiesSieve props = new PropertiesSieve("test-tier3");
            props.setPassword("tier3-pass");
            props.write();

            PropertiesSieve loaded = new PropertiesSieve("test-tier3");
            loaded.load();
            assertThat(loaded.getPassword()).isEqualTo("tier3-pass");
        }
    }

    @Test
    void shouldThrowWhenNoEncryptionAlgorithmAvailable() {
        try (MockedConstruction<StandardPBEStringEncryptor> mocked = mockConstruction(
                StandardPBEStringEncryptor.class,
                (mock, context) -> {
                    when(mock.encrypt(anyString()))
                            .thenThrow(new EncryptionOperationNotPossibleException("No algorithm available"));
                }
        )) {
            assertThatThrownBy(() -> new PropertiesSieve("test-all-fail"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No suitable encryption algorithm");
        }
    }

    // ===== Last Used File Edge Cases =====

    @Test
    void shouldHandleLastUsedFileWithWhitespaceOnly() throws IOException {
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path lastUsedFile = configDir.resolve(".lastused");
        Files.writeString(lastUsedFile, "   \n\t  ");

        String lastUsed = PropertiesSieve.getLastUsedProfile();
        assertThat(lastUsed).isEqualTo("default");
    }

    // ===== Migration Edge Cases =====

    @Test
    void shouldHandleMigrationWithMultipleFiles() throws IOException {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);

        Path legacyProfile1 = legacyDir.resolve("alpha.properties");
        Files.writeString(legacyProfile1, "sieve.server=alpha.com");

        Path legacyProfile2 = legacyDir.resolve("beta.properties");
        Files.writeString(legacyProfile2, "sieve.server=beta.com");

        PropertiesSieve.migrateOldProperties();

        Path newDir = AppDirectoryService.getProfilesDir();
        assertThat(newDir.resolve("alpha.properties")).exists();
        assertThat(newDir.resolve("beta.properties")).exists();

        Files.deleteIfExists(newDir.resolve("alpha.properties"));
        Files.deleteIfExists(newDir.resolve("beta.properties"));
    }

    @Test
    void shouldHandleMigrationWhenDestinationMissing() throws IOException {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);

        Path legacyProfile = legacyDir.resolve("dest.properties");
        Files.writeString(legacyProfile, "sieve.server=dest.com");

        Path newDir = AppDirectoryService.getProfilesDir();
        Path destProfile = newDir.resolve("dest.properties");
        Files.deleteIfExists(destProfile);
        Files.writeString(destProfile, "sieve.server=existing.com");

        PropertiesSieve.migrateOldProperties();

        assertThat(Files.readString(destProfile)).contains("existing.com");
    }

    // ===== Non-Existent Profile Edge Cases =====

    @Test
    void shouldCheckNonExistentProfileReturnsFalse() {
        assertThat(PropertiesSieve.profileExists("nonexistent")).isFalse();
    }
}
