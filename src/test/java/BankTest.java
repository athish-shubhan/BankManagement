import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
    void testWithdrawNegativeAmountShouldBeRejected() {
        // This test documents the negative-amount defect found in Activity 2, Part B.
        // EXPECTED (correct) behaviour: withdrawing a negative amount should be
        // rejected, and the balance should remain unchanged.
        // ACTUAL behaviour: the code only checks "balance >= amount", so a negative
        // amount passes that check, and subtracting a negative amount INCREASES
        // the balance instead of rejecting the withdrawal.
        // This test is expected to FAIL, proving the defect exists in Bank.withdraw().

        Bank bank = new Bank();
        bank.AL.add(new Account("Alice", 11111111, "1111", 0.0)); // balance = 1000

        provideInput("11111111\n1111\n-500\n");
        bank.withdraw();

        // Correct behaviour would keep the balance at 1000.0 (withdrawal rejected)
        assertEquals(1000.0, bank.AL.get(0).getAmount(),
                "Withdrawing a negative amount should be rejected, but the balance changed instead.");
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
}