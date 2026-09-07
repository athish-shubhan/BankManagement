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
}