import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;
import com.inflectra.spiratest.addons.junitextension.SpiraTestCase;
import com.inflectra.spiratest.addons.junitextension.SpiraTestConfiguration;

/**
 * Test naming convention: [UnitOfWork]_[StateUnderTest]_[ExpectedBehavior]
 *
 * This suite follows Roy Osherove's naming standard (Osherove, "Naming standards for
 * unit tests," osherove.com, 2005), a widely cited convention in enterprise Java unit
 * testing (see also Osherove, "The Art of Unit Testing," Manning Publications). Each
 * test name states the method under test, the specific condition/input being tested,
 * and the expected outcome, so a test's purpose is clear without reading its body -
 * e.g. transfer_withNegativeAmount_shouldBeRejected reads as a complete sentence
 * describing behaviour, not implementation detail.
 *
 * This is functionally equivalent to the Given-When-Then (BDD) style, just with the
 * three parts in a different order; Osherove's ordering was chosen here as it is the
 * more established convention for method-level unit tests in enterprise Java, and it
 * pairs naturally with the Arrange-Act-Assert structure used inside every test below.
 *
 * This class covers the "Bank Persistence and Display" Spira Test Set [TX:6489]:
 * addNewRecord(), print(), save(), and load().
 */
@SpiraTestConfiguration(
        url = "https://rmit.spiraservice.net/",
        login = "s4139882",
        rssToken = "{25CBA1A6-10CC-41EC-99B0-7D4B1AD3AC10}",
        projectId = 1046,
        releaseId = 2718,
        testSetId = 6489
)
public class BankPersistenceTest {

    private final InputStream originalSystemIn = System.in;

    @AfterEach
    public void
 restoreSystemIn() {
        System.setIn(originalSystemIn);
    }

    private void
 provideInput(String data) {
        ByteArrayInputStream testInput = new ByteArrayInputStream(data.getBytes());
        System.setIn(testInput);
    }

    /**
     * This is testing the critical functionality of addNewRecord, and uses a "stub" by
     * redirecting System.in to a ByteArrayInputStream containing pre-written input, via
     * the provideInput() helper. This lets the test simulate a user typing values
     * without needing a real keyboard, while still exercising the real addNewRecord()
     * method exactly as written.
     */
    @Test
    @SpiraTestCase(testCaseId = 50977)
    public void
 addNewRecord_withValidInput_addsAccountToList() {
        // Arrange
        provideInput("Athish\n12345678\n1234\n500\n");
        Bank bank = new Bank();

        // Act
        bank.addNewRecord();

        // Assert
        assertEquals(1, bank.AL.size());
        assertEquals("Athish", bank.AL.get(0).getName());
        assertEquals(12345678, bank.AL.get(0).getAccountNumber());
        assertEquals("1234", bank.AL.get(0).getPIN());
        assertEquals(1500.0, bank.AL.get(0).getAmount()); // 1000 default + 500
    }

    /**
     * print() has no return value and no state change of its own. To meaningfully test
     * this, System.out itself is redirected to a ByteArrayOutputStream.
     */
    @Test
    @SpiraTestCase(testCaseId = 50979)
    public void
 print_withOneAccount_outputsCorrectDetails() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

        // Act
        bank.print();
        System.setOut(originalOut);

        // Assert
        String output = outContent.toString();
        assertAll("Printed account details are correct",
                () -> assertTrue(output.contains("Athish1")),
                () -> assertTrue(output.contains("11111111")),
                () -> assertTrue(output.contains("1000.0"))
        );
    }

    /**
     * This tests save() and load() together as a genuine round trip: data is saved by
     * one Bank object, then loaded into a completely fresh Bank object, proving the
     * data actually persisted to a real file on disk and back, rather than just
     * staying in memory. The file is deleted both before and after the test (the
     * latter in a finally block) to keep the test isolated and apublic void
 leaving data
     * behind.
     */
    @Test
    @SpiraTestCase(testCaseId = 50980)
    public void
 saveAndLoad_roundTrip_restoresAllAccountData() {
        // Arrange
        java.io.File file = new java.io.File("BankRecord.txt");
        file.delete();

        try {
            Bank bankToSave = new Bank();
            bankToSave.AL.add(new Account("Athish1", 11111111, "1111", 500.0)); // balance = 1500
            bankToSave.AL.add(new Account("Athish2", 22222222, "2222", 0.0));   // balance = 1000

            // Act
            bankToSave.save();
            Bank bankToLoad = new Bank();
            bankToLoad.load();

            // Assert
            assertAll("Loaded accounts match what was saved",
                    () -> assertEquals(2, bankToLoad.AL.size()),
                    () -> assertEquals("Athish1", bankToLoad.AL.get(0).getName()),
                    () -> assertEquals(11111111, bankToLoad.AL.get(0).getAccountNumber()),
                    () -> assertEquals(1500.0, bankToLoad.AL.get(0).getAmount()),
                    () -> assertEquals("Athish2", bankToLoad.AL.get(1).getName()),
                    () -> assertEquals(22222222, bankToLoad.AL.get(1).getAccountNumber()),
                    () -> assertEquals(1000.0, bankToLoad.AL.get(1).getAmount())
            );
        } finally {
            file.delete();
        }
    }

    /**
     * This tests load()'s error-handling path when the file doesn't exist at all.
     */
    @Test
    @SpiraTestCase(testCaseId = 50981)
    public void
 load_whenFileMissing_leavesListEmptyWithoutThrowing() {
        // Arrange
        java.io.File file = new java.io.File("BankRecord.txt");
        file.delete();
        Bank bank = new Bank();

        // Act
        bank.load();

        // Assert
        assertEquals(0, bank.AL.size());
    }

    /**
     * This forces save()'s catch block to trigger, by pre-creating "BankRecord.txt" as
     * a directory rather than a file, since FileOutputStream cannot write to a path
     * that's already a directory.
     */
    @Test
    @SpiraTestCase(testCaseId = 50982)
    public void
 save_whenTargetPathIsBlocked_printsErrorMessage() {
        // Arrange
        java.io.File asDirectory = new java.io.File("BankRecord.txt");
        asDirectory.delete(); // clear any leftover file/directory from a previous run first
        boolean created = asDirectory.mkdir();
        assertTrue(created, "Test setup failed: could not create directory to block save()");

        try {
            java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
            java.io.PrintStream originalOut = System.out;
            System.setOut(new java.io.PrintStream(outContent));
            Bank bank = new Bank();
            bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));

            // Act
            bank.save();
            System.setOut(originalOut);

            // Assert
            String output = outContent.toString();
            assertTrue(output.contains("Error Saving Data to File"));
        } finally {
            asDirectory.delete();
        }
    }

    /**
     * This test triggers the if(temp == null) break; branch inside load()'s loop,
     * which can never occur through a normal save-then-load cycle, since
     * ObjectInputStream.readObject() throws an EOFException at end-of-file rather than
     * returning null. To exercise this specific branch anyway, the file is built
     * manually with a real Account followed by an explicitly-written null, without
     * modifying Bank.java.
     */
    @Test
    @SpiraTestCase(testCaseId = 50983)
    public void
 load_whenNullObjectInStream_stopsReadingGracefully() throws Exception {
        // Arrange
        java.io.File file = new java.io.File("BankRecord.txt");
        file.delete();

        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(fos);
            out.writeObject(new Account("Athish1", 11111111, "1111", 0.0));
            out.writeObject(null);
            out.close();
            Bank bank = new Bank();

            // Act
            bank.load();

            // Assert
            assertEquals(1, bank.AL.size());
            assertEquals("Athish1", bank.AL.get(0).getName());
        } finally {
            file.delete();
        }
    }
}