# Improvements log

## Round 1 — 2026-11-05

### What changed
1. **The build didn't work from a clean clone.** `pom.xml` depended on `com.inflectra.spiratest:junit5-extension:1.0`, which isn't on Maven Central, so `mvn test` stopped at dependency resolution and no test could compile. The default build now uses an `activeByDefault` profile that compiles the tests against a no-op copy of the two annotations they use (`src/test/spira-stub/`). `mvn test -Pspira` switches back to the real jar for anyone who has installed it locally. The test sources are unchanged.
2. **Test CI that can go green honestly.** The suite deliberately has 3 failing tests that document defects in the code under test, so "all tests pass" can't be the gate. The new `.github/workflows/tests.yml` runs on every PR and push to main. It runs the suite, then `.github/scripts/check_known_defects.py` requires the failing set to be exactly `known-defects.txt`. Any other failure fails the build, and so does a documented defect that starts passing (so the list stays accurate). No test is skipped or disabled. The workflow also uploads the coverage report.
3. **Coverage is now measured in the build.** I added JaCoCo. This confirms the "100% line/branch coverage on Account and Bank" claim from the commit history (numbers below).
4. **README (there wasn't one).** It covers where the code under test comes from, a Quick start that works, the test classes, the known-defects table, coverage, how to report to Spira, and the SpotBugs findings.
5. **Removed dead files.** `src/main/java/Account.java` and `Bank.java` were entirely commented-out copies (including broken `public public void` lines) of the classes that actually live in `Task_Driver.java`.

The code under test (`Task_Driver.java`) and all test code are unchanged.

### Why it matters
Before this, someone cloning the repo couldn't run a single test, and the only CI workflow (SpotBugs) had failed on all 17 runs. Now `mvn test` works out of the box, and CI separates "the documented defects are still there" from "something regressed".

### How it was verified
```
# main, clean clone
$ mvn -B test
[ERROR] Could not find artifact com.inflectra.spiratest:junit5-extension:jar:1.0 in central

# this branch, clean clone
$ mvn -B test
[ERROR] Tests run: 27, Failures: 3, Errors: 0, Skipped: 0      # the 3 documented defects
$ mvn -B test -Dmaven.test.failure.ignore=true && python3 .github/scripts/check_known_defects.py
27 tests, 3 failing, 3 known defects
  expected failure (known defect): AccountTest#setAmount_withNegativeValue_shouldBeRejected
  expected failure (known defect): BankTransactionsTest#transfer_withNegativeAmount_shouldBeRejected
  expected failure (known defect): BankTransactionsTest#withdraw_withNegativeAmount_shouldBeRejected
exit 0
# checker with one defect removed from the list -> "UNEXPECTED FAILURE ...", exit 1
# checker with a passing test added to the list -> "KNOWN DEFECT NOW PASSES ...", exit 1

$ mvn help:active-profiles          ->  spira-stub
$ mvn -Pspira test                  ->  needs the real jar (not available here), as documented
```
JaCoCo (`target/site/jacoco/jacoco.csv`):

| Class | Lines | Branches |
|---|---|---|
| Account | 24/24 (100%) | n/a |
| Bank | 87/87 (100%) | 32/32 (100%) |
| Task_Driver | 0/32 | 0/12 |

`mvn compile spotbugs:check` (what `static-analysis.yml` runs): `Total bugs: 8`, all in `Task_Driver.java`: 4 × DM_DEFAULT_ENCODING, 2 × OS_OPEN_STREAM, 2 × REC_CATCH_EXCEPTION. This is why that workflow has failed on every run since it was added. I left it unchanged because the findings are in the deliberately unmodified code under test.

**Not verified:** reporting to SpiraTest with the real extension. The jar isn't publicly downloadable through Maven and no Spira credentials were used.

### Files touched
`pom.xml`, `README.md` (new), `IMPROVEMENTS.md` (new), `known-defects.txt` (new), `.github/workflows/tests.yml` (new), `.github/scripts/check_known_defects.py` (new), `src/test/spira-stub/…/SpiraTestCase.java` (new), `src/test/spira-stub/…/SpiraTestConfiguration.java` (new); deleted `src/main/java/Account.java`, `src/main/java/Bank.java`.
