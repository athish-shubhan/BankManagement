import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test naming convention: [UnitOfWork]_[StateUnderTest]_[ExpectedBehavior]
 * This suite follows Roy Osherove's naming standard (Osherove, "Naming standards for
 * unit tests," osherove.com, 2005), a widely cited convention in enterprise Java unit
 * testing (see also Osherove, "The Art of Unit Testing," Manning Publications). Each
 * test name states the method under test, the specific condition/input being tested,
 * and the expected outcome, so a test's purpose is clear without reading its body -
 * e.g. transfer_withNegativeAmount_shouldBeRejected reads as a complete sentence
 * describing behaviour, not implementation detail.
 * This is functionally equivalent to the Given-When-Then (BDD) style, just with the
 * three parts in a different order; Osherove's ordering was chosen here as it is the
 * more established convention for method-level unit tests in enterprise Java, and it
 * pairs naturally with the Arrange-Act-Assert structure used inside every test below.
 */
class BankTest {

    private final InputStream originalSystemIn = System.in;

    @AfterEach
    void restoreSystemIn() {
        System.setIn(originalSystemIn);
    }

    private void provideInput(String data) {
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
    void addNewRecord_withValidInput_addsAccountToList() {
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
     * This tests the critical happy-path functionality of transfer(), and uses a stub
     * (provideInput() redirecting System.in) to simulate the four values a user would
     * type (sender account, sender PIN, receiver account, amount). It verifies both
     * accounts' balances together using assertAll(), since checking only the sender or
     * only the receiver wouldn't be enough to confirm the transfer actually worked as a
     * single, coherent operation.
     */
    @Test
    void transfer_withValidSenderReceiverAndBalance_updatesBothBalances() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Athish2", 22222222, "2222", 0.0));  // balance = 1000
        provideInput("11111111\n1111\n22222222\n300\n");

        // Act
        bank.transfer();

        // Assert
        assertAll("Balances correct after successful transfer",
                () -> assertEquals(700.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(1300.0, bank.AL.get(1).getAmount())
        );
    }

    /**
     * This is a boundary test, checking the exact edge of the condition
     * "if(balance >= amount)" in transfer(). This matters because boundary values are
     * where off-by-one style logic errors (e.g. using ">" instead of ">=") most commonly
     * hide, even though this specific case happens to already pass with the current code.
     */
    @Test
    void transfer_withAmountEqualToBalance_leavesSenderAtZero() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Athish2", 22222222, "2222", 0.0));  // balance = 1000
        provideInput("11111111\n1111\n22222222\n1000\n");

        // Act
        bank.transfer();

        // Assert
        assertAll("Sender left with 0, receiver gets the full amount",
                () -> assertEquals(0.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(2000.0, bank.AL.get(1).getAmount())
        );
    }

    /**
     * This is another boundary/edge case. The console prompt in addNewRecord() already
     * treats 0 as a meaningful, expected input, so it is worth confirming a zero-amount
     * transfer behaves sensibly here too.
     */
    @Test
    void transfer_withZeroAmount_leavesBalancesUnchanged() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Athish2", 22222222, "2222", 0.0));  // balance = 1000
        provideInput("11111111\n1111\n22222222\n0\n");

        // Act
        bank.transfer();

        // Assert
        assertAll("Both balances unchanged after a zero-amount transfer",
                () -> assertEquals(1000.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(1000.0, bank.AL.get(1).getAmount())
        );
    }

    /**
     * This tests the "account not found" error path, an invalid input scenario where
     * the sender's account number doesn't exist anywhere in the system. This confirms
     * the method fails safely without touching any account data when given a bad sender.
     */
    @Test
    void transfer_withUnknownSenderAccount_returnsWithoutChanges() {
        // Arrange
        Bank bank = new Bank();
        // No accounts exist yet, so sender lookup will fail
        provideInput("99999999\n0000\n");

        // Act
        bank.transfer();

        // Assert
        assertEquals(0, bank.AL.size());
    }

    /**
     * This tests the same "not found" error path as the previous test, but for the
     * receiver instead of the sender, and this is not duplicated but is necessary to
     * check if receiver's account number is not right and sender is validated.
     */
    @Test
    void transfer_withUnknownReceiverAccount_returnsWithoutChanges() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\n1111\n99999999\n");

        // Act
        bank.transfer();

        // Assert
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    /**
     * This tests the else branch of if(balance >= amount). It confirms neither balance
     * changes when the transfer is correctly rejected on insufficient funds.
     */
    @Test
    void transfer_withInsufficientSenderBalance_isRejected() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Athish2", 22222222, "2222", 0.0));  // balance = 1000
        provideInput("11111111\n1111\n22222222\n5000\n");

        // Act
        bank.transfer();

        // Assert
        assertAll("Balances unchanged after failed transfer",
                () -> assertEquals(1000.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(1000.0, bank.AL.get(1).getAmount())
        );
    }

    /**
     * This tests a specific combination of the compound lookup condition
     * (accountNumber == s_acc && pin.equals(s_pin)) that isn't exercised by the other
     * tests. Without this test, that half of the && condition would never actually be
     * evaluated as false.
     */
    @Test
    void transfer_withIncorrectPin_isRejected() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\nWRONGPIN\n");

        // Act
        bank.transfer();

        // Assert
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    /**
     * This test asserts the correct, intended behaviour for a negative transfer amount,
     * as required by the assignment to test positive, negative and boundary values
     * wherever possible. Both balances should remain unchanged, since a negative
     * transfer makes no real-world sense.
     */
    @Test
    void transfer_withNegativeAmount_shouldBeRejected() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Athish2", 22222222, "2222", 0.0));  // balance = 1000
        provideInput("11111111\n1111\n22222222\n-300\n");

        // Act
        bank.transfer();

        // Assert
        assertAll("Negative transfer amounts must be rejected",
                () -> assertEquals(1000.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(1000.0, bank.AL.get(1).getAmount())
        );
    }

    /**
     * This tests an exceptional/error condition not covered by any other test: what
     * happens when the user types non-numeric text where an account number is
     * expected. Scanner.nextInt() throws InputMismatchException in this case, which the
     * method does not catch, so it propagates up.
     */
    @Test
    void transfer_withNonNumericAccountNumber_throwsInputMismatchException() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));
        provideInput("notanumber\n");

        // Act & Assert
        assertThrows(java.util.InputMismatchException.class, () -> bank.transfer());
    }

    /**
     * This tests the core happy-path functionality of withdraw(), mirroring the
     * successful transfer test but for a single-account operation.
     */
    @Test
    void withdraw_withValidAccountAndSufficientBalance_decreasesBalance() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\n1111\n300\n");

        // Act
        bank.withdraw();

        // Assert
        assertEquals(700.0, bank.AL.get(0).getAmount());
    }

    /**
     * This is a boundary test, mirroring the equivalent transfer test but for
     * withdrawal.
     */
    @Test
    void withdraw_withAmountEqualToBalance_leavesAccountAtZero() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\n1111\n1000\n");

        // Act
        bank.withdraw();

        // Assert
        assertEquals(0.0, bank.AL.get(0).getAmount());
    }

    /**
     * This is an edge case, mirroring the equivalent transfer test for withdrawal.
     * Withdrawing exactly 0 should leave the balance unchanged.
     */
    @Test
    void withdraw_withZeroAmount_leavesBalanceUnchanged() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\n1111\n0\n");

        // Act
        bank.withdraw();

        // Assert
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    /**
     * This test specifically forces the lookup loop to iterate past a non-matching
     * account before finding the correct one, since every other withdraw test only
     * ever has a single account in the list. Without this, the loop's "keep checking
     * the next account" behaviour with multiple accounts present would never actually
     * be exercised.
     */
    @Test
    void withdraw_whenMultipleAccountsExist_updatesCorrectAccount() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Athish2", 22222222, "2222", 0.0));  // balance = 1000
        provideInput("22222222\n2222\n200\n");

        // Act
        bank.withdraw();

        // Assert
        assertAll(
                () -> assertEquals(1000.0, bank.AL.get(0).getAmount()), // unaffected
                () -> assertEquals(800.0, bank.AL.get(1).getAmount())   // withdrew 200
        );
    }

    /**
     * This tests the "account not found" error path for withdraw().
     */
    @Test
    void withdraw_withUnknownAccount_returnsWithoutChanges() {
        // Arrange
        Bank bank = new Bank();
        // No accounts exist yet, so lookup will fail
        provideInput("99999999\n0000\n");

        // Act
        bank.withdraw();

        // Assert
        assertEquals(0, bank.AL.size());
    }

    /**
     * This tests the else branch of if(balance >= amount) for withdrawals, mirroring
     * the equivalent transfer test.
     */
    @Test
    void withdraw_withInsufficientBalance_isRejected() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\n1111\n5000\n");

        // Act
        bank.withdraw();

        // Assert
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    /**
     * This tests the same compound-condition gap as the equivalent transfer test.
     */
    @Test
    void withdraw_withIncorrectPin_isRejected() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\nWRONGPIN\n");

        // Act
        bank.withdraw();

        // Assert
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    /**
     * This test asserts the correct, intended behaviour for a negative withdrawal
     * amount, mirroring the equivalent transfer test.
     */
    @Test
    void withdraw_withNegativeAmount_shouldBeRejected() {
        // Arrange
        Bank bank = new Bank();
        bank.AL.add(new Account("Athish1", 11111111, "1111", 0.0)); // balance = 1000
        provideInput("11111111\n1111\n-500\n");

        // Act
        bank.withdraw();

        // Assert
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    /**
     * print() has no return value and no state change of its own. To meaningfully test
     * this, System.out itself is redirected to a ByteArrayOutputStream.
     */
    @Test
    void print_withOneAccount_outputsCorrectDetails() {
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
     * latter in a finally block) to keep the test isolated and avoid leaving data
     * behind.
     */
    @Test
    void saveAndLoad_roundTrip_restoresAllAccountData() {
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
    void load_whenFileMissing_leavesListEmptyWithoutThrowing() {
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
    void save_whenTargetPathIsBlocked_printsErrorMessage() {
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
    void load_whenNullObjectInStream_stopsReadingGracefully() throws Exception {
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