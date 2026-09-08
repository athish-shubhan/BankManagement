import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    void testParameterizedConstructor() {
        Account acc = new Account("Alice", 12345678, "1234", 500.0);

        assertEquals("Alice", acc.getName());
        assertEquals(12345678, acc.getAccountNumber());
        assertEquals("1234", acc.getPIN());
        assertEquals(1500.0, acc.getAmount()); // 1000 default + 500
    }

    @Test
    void testDefaultConstructorAndSetters() {
        Account acc = new Account();

        assertNull(acc.getName());
        assertEquals(0, acc.getAccountNumber());
        assertNull(acc.getPIN());
        assertEquals(0.0, acc.getAmount());

        acc.setName("Bob");
        acc.setAccountNumber(87654321);
        acc.setPIN("4321");
        acc.setAmount(2000.0);

        assertAll("Account setters",
                () -> assertEquals("Bob", acc.getName()),
                () -> assertEquals(87654321, acc.getAccountNumber()),
                () -> assertEquals("4321", acc.getPIN()),
                () -> assertEquals(2000.0, acc.getAmount())
        );
    }

    @Test
    void testConstructorWithNegativeAmount() {
        // Boundary/invalid input case: negative amount passed at account creation.
        // Related to the negative-amount defect identified in Activity 2 (Part B).
        Account acc = new Account("Charlie", 11112222, "0000", -500.0);

        // The constructor always does "1000 + amount", so passing -500 results in 500.
        // This documents the ACTUAL current behaviour, so we notice if it ever changes.
        assertEquals(500.0, acc.getAmount());
    }

    @Test
    void testSetAmountWithNegativeValue() {
        //???? IDK if i should include this as Bank.transfer()/withdraw() but is it necessary
        // Checks whether setAmount() validates its input the same way the
        // constructor does (or doesn't) - relevant since Bank.transfer()/withdraw()
        // call setAmount() directly, not the constructor.
        Account acc = new Account();
        acc.setAmount(-750.0);

        assertEquals(-750.0, acc.getAmount());
    }

}