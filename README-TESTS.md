# Test Infrastructure

## Overview

This directory contains the test infrastructure for SieveEditor. The test framework uses:

- **JUnit 5** (5.10.1) - Test framework
- **Mockito** (5.8.0) - Mocking framework
- **AssertJ** (3.24.2) - Fluent assertions
- **JaCoCo** (0.8.11) - Code coverage (temporarily disabled)

## Running Tests

### Basic Test Execution

```bash
cd app
mvn test
```

### With Coverage Report (when JaCoCo is enabled)

```bash
cd app
mvn clean test
# View report at: target/site/jacoco/index.html
```

### Quick Build Without Tests

```bash
cd app
mvn clean package -DskipTests
```

## Test Structure

```text
app/src/test/java/de/febrildur/sieveeditor/
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

- ✅ Test dependencies added to pom.xml
- ✅ Test directory structure created
- ✅ Smoke test created
- ✅ Test utilities created (TestConfiguration)
- ✅ Maven Surefire plugin configured

### In Progress

- ⏳ JaCoCo plugin (temporarily disabled due to network issues)
- ⏳ Writing actual test implementations

### Pending

- ⏳ Security tests
- ⏳ Core business logic tests
- ⏳ UI action tests
- ⏳ Tokenizer tests

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

1. You're in the `app/` directory
2. Java 21 is installed: `java -version`
3. Maven dependencies are downloaded: `mvn dependency:resolve`

### JaCoCo Issues

JaCoCo is temporarily disabled. To re-enable:

1. Uncomment the JaCoCo plugin in `pom.xml`
2. Run `mvn clean test`

## Next Steps

1. Enable JaCoCo when network is available
2. Write security tests (SSL validation, encryption)
3. Write core business logic tests
4. Write UI action tests
5. Achieve 70%+ overall coverage

See `TEST-COVERAGE-ANALYSIS.md` in the project root for detailed test planning.
