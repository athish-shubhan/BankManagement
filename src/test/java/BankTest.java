import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testAddNewRecord() {
        // Simulates user typing: name, account number, PIN, then amount
        provideInput("Alice\n12345678\n1234\n500\n");

        Bank bank = new Bank();
        bank.addNewRecord();

        assertEquals(1, bank.AL.size());
        assertEquals("Alice", bank.AL.get(0).getName());
        assertEquals(12345678, bank.AL.get(0).getAccountNumber());
        assertEquals("1234", bank.AL.get(0).getPIN());
        assertEquals(1500.0, bank.AL.get(0).getAmount()); // 1000 default + 500
    }

    @Test
    void testTransferSenderNotFound() {
        Bank bank = new Bank();
        // No accounts exist yet, so sender lookup will fail
        provideInput("99999999\n0000\n");
        bank.transfer();

        // Nothing should have changed; list remains empty
        assertEquals(0, bank.AL.size());
    }

    @Test
    void testTransferReceiverNotFound() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        // Sender found (Alice), but receiver account number doesn't exist
        provideInput("11111111\n1111\n99999999\n");
        bank.transfer();

        // Alice's balance should be unchanged since transfer never completed
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testTransferInsufficientBalance() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0));   // balance = 1000
        bank.AL.add(new Account("Bob", 22222222, "2222", 0.0));     // balance = 1000

        // Alice tries to send more than she has
        provideInput("11111111\n1111\n22222222\n5000\n");
        bank.transfer();

        assertAll("Balances unchanged after failed transfer",
                () -> assertEquals(1000.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(1000.0, bank.AL.get(1).getAmount())
        );
    }

    @Test
    void testTransferSuccess() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0));   // balance = 1000
        bank.AL.add(new Account("Bob", 22222222, "2222", 0.0));     // balance = 1000

        provideInput("11111111\n1111\n22222222\n300\n");
        bank.transfer();

        assertAll("Balances correct after successful transfer",
                () -> assertEquals(700.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(1300.0, bank.AL.get(1).getAmount())
        );
    }

    @Test
    void testWithdrawAccountNotFound() {
        Bank bank = new Bank();
        // No accounts exist yet, so lookup will fail
        provideInput("99999999\n0000\n");
        bank.withdraw();

        assertEquals(0, bank.AL.size());
    }

    @Test
    void testWithdrawInsufficientBalance() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        // Alice tries to withdraw more than she has
        provideInput("11111111\n1111\n5000\n");
        bank.withdraw();

        // Balance should be unchanged since withdrawal never completed
        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testWithdrawSuccess() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        provideInput("11111111\n1111\n300\n");
        bank.withdraw();

        assertEquals(700.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testPrint() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        // Capture what print() actually writes to System.out
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

        bank.print();

        System.setOut(originalOut); // restore real output

        String output = outContent.toString();
        assertAll("Printed account details are correct",
                () -> assertTrue(output.contains("Alice")),
                () -> assertTrue(output.contains("11111111")),
                () -> assertTrue(output.contains("1000.0"))
        );
    }

    @Test
    void testWithdrawNegativeAmountIncreasesBalance() {
        // This test documents a real defect identified in Activity 2, Part B
        // (Manual Inspection): withdraw() only checks "balance >= amount", so a
        // negative amount incorrectly INCREASES the balance instead of being
        // rejected. This test asserts the actual (buggy) current behaviour -
        // it is not asserting that this is correct or desirable. See the
        // Activity 2 report for the full defect analysis and severity rating.
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        provideInput("11111111\n1111\n-500\n");
        bank.withdraw();

        // Correct behaviour would keep this at 1000.0; the defect causes 1500.0
        assertEquals(1500.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testTransferNegativeAmountIncreasesSenderBalance() {
        // Same defect as above, in transfer(): a negative amount incorrectly
        // increases the sender's balance and decreases the receiver's, instead
        // of being rejected. Asserts actual (buggy) current behaviour - see
        // Activity 2 report for full analysis.
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Bob", 22222222, "2222", 0.0));    // balance = 1000

        provideInput("11111111\n1111\n22222222\n-300\n");
        bank.transfer();

        // Correct behaviour would leave both unchanged; the defect causes this
        assertAll("Defect: negative transfer inflates sender, deflates receiver",
                () -> assertEquals(1300.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(700.0, bank.AL.get(1).getAmount())
        );
    }

    @Test
    void testWithdrawExactBalance() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        // Boundary case: withdrawing exactly the full balance (the ">=" threshold itself)
        provideInput("11111111\n1111\n1000\n");
        bank.withdraw();

        assertEquals(0.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testTransferExactBalance() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Bob", 22222222, "2222", 0.0));    // balance = 1000

        // Boundary case: transferring exactly the sender's full balance
        provideInput("11111111\n1111\n22222222\n1000\n");
        bank.transfer();

        assertAll("Sender left with 0, receiver gets the full amount",
                () -> assertEquals(0.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(2000.0, bank.AL.get(1).getAmount())
        );
    }

    @Test
    void testWithdrawZeroAmount() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        // Edge case: withdrawing exactly 0 should leave the balance unchanged
        provideInput("11111111\n1111\n0\n");
        bank.withdraw();

        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testTransferZeroAmount() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Bob", 22222222, "2222", 0.0));    // balance = 1000

        // Edge case: transferring exactly 0 should leave both balances unchanged
        provideInput("11111111\n1111\n22222222\n0\n");
        bank.transfer();

        assertAll("Both balances unchanged after a zero-amount transfer",
                () -> assertEquals(1000.0, bank.AL.get(0).getAmount()),
                () -> assertEquals(1000.0, bank.AL.get(1).getAmount())
        );
    }

    @Test
    void testSaveAndLoadRoundTrip() {
        // Clean up any leftover file from previous runs before we start
        java.io.File file = new java.io.File("BankRecord.txt");
        file.delete();

        try {
            Bank bankToSave = new Bank();
            bankToSave.AL.add(new Account("Alice", 11111111, "1111", 500.0)); // balance = 1500
            bankToSave.AL.add(new Account("Bob", 22222222, "2222", 0.0));     // balance = 1000
            bankToSave.save();

            // Load into a completely fresh Bank object to prove the data
            // actually persisted to disk and back, not just stayed in memory
            Bank bankToLoad = new Bank();
            bankToLoad.load();

            assertAll("Loaded accounts match what was saved",
                    () -> assertEquals(2, bankToLoad.AL.size()),
                    () -> assertEquals("Alice", bankToLoad.AL.get(0).getName()),
                    () -> assertEquals(11111111, bankToLoad.AL.get(0).getAccountNumber()),
                    () -> assertEquals(1500.0, bankToLoad.AL.get(0).getAmount()),
                    () -> assertEquals("Bob", bankToLoad.AL.get(1).getName()),
                    () -> assertEquals(22222222, bankToLoad.AL.get(1).getAccountNumber()),
                    () -> assertEquals(1000.0, bankToLoad.AL.get(1).getAmount())
            );
        } finally {
            // Always clean up, even if the test fails, so we don't leave
            // test data behind for other tests or future runs
            file.delete();
        }
    }

    @Test
    void testLoadWhenFileDoesNotExist() {
        // Make sure the file genuinely does not exist before this test runs
        java.io.File file = new java.io.File("BankRecord.txt");
        file.delete();

        Bank bank = new Bank();
        bank.load(); // Should not throw, even though the file is missing

        // This documents the actual behaviour: load()'s empty catch block
        // silently swallows the FileNotFoundException, leaving AL empty
        // with no error message shown to the user (see Activity 2, Part B).
        assertEquals(0, bank.AL.size());
    }

    @Test
    void testTransferWrongPin() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        // Correct account number, but wrong PIN
        provideInput("11111111\nWRONGPIN\n");
        bank.transfer();

        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testWithdrawWrongPin() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        // Correct account number, but wrong PIN
        provideInput("11111111\nWRONGPIN\n");
        bank.withdraw();

        assertEquals(1000.0, bank.AL.get(0).getAmount());
    }

    @Test
    void testSaveFailure() {
        // Force save() to fail by making "BankRecord.txt" already exist as a
        // directory - FileOutputStream cannot write to a path that is a directory,
        // so this triggers the catch block in save().
        java.io.File asDirectory = new java.io.File("BankRecord.txt");
        asDirectory.mkdir();

        try {
            java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
            java.io.PrintStream originalOut = System.out;
            System.setOut(new java.io.PrintStream(outContent));

            Bank bank = new Bank();
            bank.AL.add(new Account("Alice", 11111111, "1111", 0.0));
            bank.save(); // should hit the catch block, not throw

            System.setOut(originalOut);

            String output = outContent.toString();
            assertTrue(output.contains("Error Saving Data to File"));
        } finally {
            asDirectory.delete(); // clean up the directory we created
        }
    }

    @Test
    void testWithdrawWithMultipleAccountsInList() {
        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0));  // balance = 1000
        bank.AL.add(new Account("Bob", 22222222, "2222", 0.0));    // balance = 1000

        // Withdraw from Bob (second account), forcing the loop to check
        // Alice first (no match), then Bob (match)
        provideInput("22222222\n2222\n200\n");
        bank.withdraw();

        assertAll(
                () -> assertEquals(1000.0, bank.AL.get(0).getAmount()), // Alice unchanged
                () -> assertEquals(800.0, bank.AL.get(1).getAmount())   // Bob withdrew 200
        );
    }

    @Test
    void testLoadStopsWhenNullObjectEncountered() throws Exception {
        java.io.File file = new java.io.File("BankRecord.txt");
        file.delete();

        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(fos);
            out.writeObject(new Account("Alice", 11111111, "1111", 0.0));
            out.writeObject(null);
            out.close();

            Bank bank = new Bank();
            bank.load();

            assertEquals(1, bank.AL.size());
            assertEquals("Alice", bank.AL.get(0).getName());
        } finally {
            file.delete();
        }
    }
}