# Code Coverage Report (JaCoCo)

JaCoCo is used to measure test coverage in the project. It helps identify which parts of the application are covered by automated tests.

## 1. Generate the Coverage Report

Run the following command from the project root directory using Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

After the build completes successfully, check the configured JaCoCo report output directory.

If the project is configured to generate reports in `docs/jacoco/`, open:

```text
docs/jacoco/index.html
```

The HTML report shows the coverage details for packages, classes, methods, and lines of code.

## 2. Maven Commands

| Command | Purpose |
|---|---|
| `.\mvnw.cmd test` | Runs the automated tests |
| `.\mvnw.cmd clean verify` | Cleans the project, runs tests, and executes the verification phase |
| `.\mvnw.cmd verify` | Runs the Maven verification lifecycle, including configured coverage checks |

The project is configured with a 60% line coverage target in `pom.xml`. The build will enforce this threshold only if the JaCoCo coverage check is configured in the Maven verification lifecycle.

## 3. Docker and Integration Tests

The `DatabaseMigrationIntegrationTest` uses Testcontainers to start a MySQL 8 database for integration testing.

- Docker Desktop must be running for the MySQL Testcontainer test to execute.
- If Docker is unavailable, the test may be skipped depending on its configuration.
- Other automated tests can still run without the MySQL container.

## 4. Reviewing the Report

After generating the report:

1. Open `index.html` in a browser.
2. Check the overall line coverage percentage.
3. Open individual packages to view class-level coverage.
4. Review red-highlighted lines to identify code that is not covered by tests.
5. Add tests for important business rules, validation, exception handling, and security behaviour.

## 5. Report Screenshot

If a coverage report is successfully generated, take a screenshot of its summary page showing the Lines and Coverage columns.

Save it as:

```text
docs/screenshots/jacoco-summary.png
```

## 6. Current Report