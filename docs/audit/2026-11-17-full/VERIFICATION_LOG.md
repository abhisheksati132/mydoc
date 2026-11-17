# Verification log

Target: `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38`, main, MyDoc 2.0.14 / 204. Baseline working tree clean. Production changes are prohibited by this audit's chosen scope; only audit documents and ignored build probes are written.

## Local Android gate

Command: `./gradlew testDebugUnitTest lintDebug assembleDebug`.

Result: failed before Gradle initialization, downloading `https://services.gradle.org/distributions/gradle-9.9.1-bin.zip`. Java `ConnectException: Connection timed out: getsockopt`. This is infrastructure failure, not a test failure and not evidence of a successful build. Do not substitute the prior revision's 589-test report.

## Focused tests executed locally

Used cached Kotlin 2.4.21 compiler and dependency jars from Gradle's module cache to compile the unmodified production `PdfToolUtils`, `PdfPermissions`, `StitchedImageLayout` and existing tests. Executed cached JUnit 4.13.2 `JUnitCore` against:

- `com.abhisheksati.mydoc.tools.PdfToolUtilsTest`
- `com.abhisheksati.mydoc.tools.engine.PdfPermissionsTest`
- `com.abhisheksati.mydoc.tools.engine.StitchedImageLayoutTest`

Result: **OK (22 tests)**, fresh execution (0.396 seconds test execution). Output: ignored `app/build/audit-probes/junit-result.txt`. This bypasses Android/Gradle configuration; it is not the full app unit suite.

Compiled the actual production sanitizer with a probe: `A + U+1F600 + U+20000 + B` => `AB`; Unicode preservation false. Ran the verbatim signature width expression with visW=72; it throws `IllegalArgumentException`. The signature probe validates the expression, not PDFBox/device integration.

Storage reviewer ran cached Kotlin stdlib traversal in a newly created Windows temporary tree using a directory junction (true symlink creation denied by OS privilege). `walkTopDown` traversed the link to an outside sentinel and deletion through the link removed it. Reproduction verifies traversal semantics; availability of actual symlinks on a user's Android filesystem remains a platform condition.

## Remote verification of the same commit

Read GitHub Actions job results for the exact target SHA using the connected GitHub API:

- CI Build: https://github.com/abhisheksati132/mydoc/actions/runs/43901248373 — success.
- Build & Test APK job: Run Unit Tests, Run Lint and job outcome success.
- Instrumented Tests (API 35) job: Run instrumented tests success.
- CodeQL: https://github.com/abhisheksati132/mydoc/actions/runs/43901248869 — success.

These are status/job results, not a fresh local device run or downloaded inspection of every test assertion. No new test total inferred from these API results. No claim that green CI disproves the audit findings.

## Repository-wide structural check

`python docs/audit/2026-11-17-full/inventory.py` enumerates all 610 tracked files, hashes their bytes, classifies paths, counts lines and parses tracked XML for well-formedness. At initial execution, no XML syntax errors were found. The inventory records binary files without claiming internal binary code review. Semantic coverage is separately recorded and incomplete until the ledger says otherwise.

## Outstanding verification

Full fresh Android unit/lint/build gates; native failure cases; API 26/29/35 device matrix; TalkBack/large font/landscape; dependency vulnerability and license provenance audit; malicious providers and archives; real symlink storage scenarios; power-loss/crash-injection during moves/restores. No claims of exhaustive safety without these.
