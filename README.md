# BankManagement — unit testing case study

A JUnit 5 test suite for a small console banking program (create account, transfer, withdraw, list accounts, save/load to a file).

The code under test, in `src/main/java/Task_Driver.java`, was taken from a public project, "Bank Management System" in the GitHub repository `Adan-Asim/Java-Projects` (that account no longer exists, so the original link is dead). It is deliberately kept as it was. The work in this repository is the tests in `src/test/java`, which include tests that document real defects in that code.

## Quick start

Needs JDK 17+ and Maven 3.8+.

```bash
git clone https://github.com/athish-shubhan/BankManagement.git
cd BankManagement
mvn test
```

Expect `Tests run: 27, Failures: 3` and a Maven `BUILD FAILURE`. The 3 failures are the documented defects listed below, not broken tests. To check that the failing set is exactly those 3:

```bash
mvn test -Dmaven.test.failure.ignore=true
python3 .github/scripts/check_known_defects.py
```

The coverage report is written to `target/site/jacoco/index.html`.

## Test suite

27 tests across three classes. All follow Arrange-Act-Assert and Osherove naming (`unitOfWork_stateUnderTest_expectedBehaviour`):

| Class | Covers |
|---|---|
| `AccountTest` | constructors, getters/setters, the +1000 opening balance, serialization |
| `BankTransactionsTest` | `transfer()` and `withdraw()`: valid, insufficient funds, exact balance, zero, wrong PIN, unknown account, bad input |
| `BankPersistenceTest` | `addNewRecord()`, `print()`, `save()` and `load()`, including a blocked save path |

Console input is simulated by redirecting `System.in` to a `ByteArrayInputStream`. The code under test saves to `BankRecord.txt` in the current directory, and the persistence tests create and delete that file. Surefire therefore runs the tests in `target/test-work/`, so `mvn test` never touches a `BankRecord.txt` you created by running the app.

### Known defects (intentionally failing tests)

| Test | Defect in the code under test |
|---|---|
| `AccountTest#setAmount_withNegativeValue_shouldBeRejected` | `Account` accepts negative amounts (constructor and `setAmount`) |
| `BankTransactionsTest#transfer_withNegativeAmount_shouldBeRejected` | A transfer of -300 moves 300 from the receiver to the sender |
| `BankTransactionsTest#withdraw_withNegativeAmount_shouldBeRejected` | A withdrawal of -500 adds 500 to the balance |

The list is also in `known-defects.txt`. CI fails if any other test fails, or if one of these starts passing.

### Coverage

Measured with JaCoCo (`mvn test -Dmaven.test.failure.ignore=true`):

| Class | Lines | Branches |
|---|---|---|
| `Account` | 24/24 (100%) | no branches |
| `Bank` | 87/87 (100%) | 32/32 (100%) |
| `Task_Driver` (console `main` menu) | 0/32 | 0/12 |

## SpiraTest reporting

The tests carry `@SpiraTestConfiguration` / `@SpiraTestCase` annotations so results can be reported to a SpiraTest instance. The SpiraTest JUnit 5 extension is not published to Maven Central, so by default the build compiles against a no-op copy of the two annotations in `src/test/spira-stub`. To build against the real extension, download its jar from Inflectra, install it into your local Maven repository, then use the `spira` profile:

```bash
mvn install:install-file -Dfile=<path-to-extension>.jar \
  -DgroupId=com.inflectra.spiratest -DartifactId=junit5-extension -Dversion=1.0 -Dpackaging=jar
mvn test -Pspira
```

## Static analysis

`mvn compile spotbugs:check` runs SpotBugs (also run by `.github/workflows/static-analysis.yml`). It currently reports 8 findings, all in the unmodified code under test: default-encoding `Scanner`, streams not closed in `load()`/`save()`, and broad `catch (Exception)`.

## Project layout

```
src/main/java/Task_Driver.java    code under test (Account, Bank, Task_Driver)
src/test/java/                    the test suite
src/test/spira-stub/              no-op SpiraTest annotations for the default build
known-defects.txt                 tests expected to fail, one per documented defect
.github/workflows/tests.yml       tests + known-defect check + coverage report
.github/workflows/static-analysis.yml  SpotBugs
```
