# Test Infrastructure

## Overview

This project contains comprehensive test infrastructure for SieveEditor using:

- **JUnit 5** (6.0.1) - Test framework
- **Mockito** (5.20.0) - Mocking framework
- **AssertJ** (3.27.6) - Fluent assertions
- **JaCoCo** (0.8.14) - Code coverage

## Running Tests

### Basic Test Execution

```bash
mvn test
```

### With Coverage Report

```bash
mvn clean verify
# View report at: target/site/jacoco/index.html
```

### Quick Build Without Tests

```bash
mvn clean package -DskipTests
```

## Test Structure

```text
src/test/java/de/febrildur/sieveeditor/
├── SmokeTest.java                 # Infrastructure verification
├── system/                         # Core business logic tests
│   ├── PropertiesSieveTest.java
│   ├── ConnectAndListScriptsTest.java
│   └── SieveTokenMakerTest.java
├── actions/                        # UI action tests
│   ├── ActionConnectTest.java
│   ├── ActionSaveScriptTest.java
│   ├── ActionLoadScriptTest.java
│   └── ...
├── security/                       # Security-specific tests
│   ├── SSLValidationTest.java
│   ├── EncryptionTest.java
│   └── PasswordFieldTest.java
└── testutil/                       # Test utilities
    └── TestConfiguration.java
```

## Current Status

### Completed

- Some small tests
- JaCoCo plugin enabled and configured

### In Progress

- Writing actual test implementations
- Increasing test coverage

### Pending

- Security tests
- Core business logic tests
- UI action tests
- Tokenizer tests

## Test Coverage Goals

| Component | Lines | Target Coverage |
|-----------|-------|----------------|
| ConnectAndListScripts.java | 143 | 80% |
| PropertiesSieve.java | 184 | 80% |
| ActionConnect.java | 181 | 70% |
| Other actions/* | 381 | 70% |
| SieveTokenMaker.java | 233 | 60% |
| Application.java | 165 | 40% |
| **OVERALL** | ~1,313 | **70%+** |

## Writing Tests

### Test Naming Convention

```java
@Test
void shouldDoSomethingWhenCondition() {
    // Use descriptive names that explain what is tested
}
```

### AAA Pattern (Arrange-Act-Assert)

```java
@Test
void shouldSaveProperties() {
    // Arrange (Given)
    PropertiesSieve config = new PropertiesSieve();
    config.setServer("example.com");

    // Act (When)
    config.write();
    config.load();

    // Assert (Then)
    assertThat(config.getServer()).isEqualTo("example.com");
}
```

### Using Test Utilities

```java
@Test
void shouldHandleTemporaryConfiguration() throws Exception {
    // Create isolated test configuration
    TestConfiguration testConfig = new TestConfiguration();

    try {
        PropertiesSieve props = testConfig.getProperties();
        props.setServer("test.example.com");
        props.write();

        // Test assertions...

    } finally {
        testConfig.cleanup();
    }
}
```

## Troubleshooting

### Maven Can't Download Dependencies

If you see network errors:

```bash
# Build project first to cache all production dependencies
mvn clean package -DskipTests

# Then try tests
mvn test
```

### Tests Won't Run

Check that:

1. You're in the project root directory
2. Java 21 is installed: `java -version`
3. Maven dependencies are downloaded: `mvn dependency:resolve`

### Coverage Report Not Generated

Run the verify phase to generate coverage:

```bash
mvn clean verify
```

The report will be at `target/site/jacoco/index.html`

## Next Steps

1. Write security tests (SSL validation, encryption)
2. Write core business logic tests
3. Write UI action tests
4. Achieve 70%+ overall coverage

For more information, see [CONTRIBUTING.md](CONTRIBUTING.md).
