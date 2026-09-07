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
}