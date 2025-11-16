package de.febrildur.sieveeditor.testutil;

import de.febrildur.sieveeditor.system.PropertiesSieve;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for creating test configurations with temporary files.
 * This helps isolate tests from production configuration files.
 */
public class TestConfiguration {

    private final Path tempDir;
    private final PropertiesSieve properties;

    /**
     * Creates a test configuration with a temporary directory.
     */
    public TestConfiguration() throws IOException {
        this.tempDir = Files.createTempDirectory("sievetest-");
        this.properties = new PropertiesSieve("test");
    }

    /**
     * Creates a test configuration with a specific profile name.
     */
    public TestConfiguration(String profileName) throws IOException {
        this.tempDir = Files.createTempDirectory("sievetest-");
        this.properties = new PropertiesSieve(profileName);
    }

    public PropertiesSieve getProperties() {
        return properties;
    }

    public Path getTempDir() {
        return tempDir;
    }

    /**
     * Cleans up temporary files created during testing.
     */
    public void cleanup() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }
}
