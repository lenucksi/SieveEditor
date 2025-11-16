---
description: Generate and display test coverage report
---

Generate JaCoCo test coverage report for SieveEditor.

Execute: ` mvn clean test jacoco:report`

After generation:
1. Display overall coverage percentage
2. Show coverage by package/class
3. Identify areas needing more tests (< 70% coverage)
4. Provide path to HTML report: `target/site/jacoco/index.html`
5. Highlight critical components with low coverage
