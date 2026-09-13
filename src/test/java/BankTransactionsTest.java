import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

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
 * This class covers the "Bank - Transactions" Spira Test Set: all transfer() and
 * withdraw() scenarios.
 */
class BankTransactionsTest {

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
}