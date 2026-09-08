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

}