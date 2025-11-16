---
description: Run complete Maven verification lifecycle
---

Run the complete Maven verification lifecycle (clean, compile, test, package).

Execute: `cd app && mvn clean verify`

This performs:
1. Clean build artifacts
2. Compile source code
3. Run all tests
4. Generate coverage report
5. Package JAR

After completion:
1. Show overall success/failure status
2. Display test results
3. Show coverage percentage
4. Confirm JAR was created
5. Provide next steps if failures occur
