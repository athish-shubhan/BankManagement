import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.inflectra.spiratest.addons.junitextension.SpiraTestCase;
import com.inflectra.spiratest.addons.junitextension.SpiraTestConfiguration;

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
 *
 * This class covers the "Account" Spira Test Set [TX:6482].
 */
@SpiraTestConfiguration(
        url = "https://rmit.spiraservice.net/",
        login = "s4139882",
        rssToken = "{25CBA1A6-10CC-41EC-99B0-7D4B1AD3AC10}",
        projectId = 1046,
        releaseId = 2718,
        testSetId = 6482
)
public class AccountTest {

    /**
     * This is the most fundamental test, as the entire suite depends on Account objects
     * being constructed correctly. It verifies all 4 fields at the same time using
     * assertAll(), and checks the "+1000" signup bonus rule stated in the console prompt
     * of addNewRecord() ("Default amount of 1000 is already added to the account..."),
     * confirming a $500 deposit correctly results in a $1500 balance as intended by the
     * application, not a hidden or accidental behaviour.
     */
    @Test
    @SpiraTestCase(testCaseId = 48676)
    public void
 constructor_withValidAndDefaultAmount_setsAllFieldsCorrectly() {
        // Arrange
        // (object creation below doubles as the Act, since construction is what's tested)

        // Act
        Account acc = new Account("Athish", 12345678, "1234", 500.0);

        // Assert
        assertAll("Parameterized constructor sets fields correctly",
                () -> assertEquals("Athish", acc.getName()),
                () -> assertEquals(12345678, acc.getAccountNumber()),
                () -> assertEquals("1234", acc.getPIN()),
                () -> assertEquals(1500.0, acc.getAmount()) // 1000 default + 500
        );
    }

    /**
     * This tests the default constructor, even though the code doesn't use it anywhere,
     * but it ought to be tested as another part of the program may use it, verified by
     * an assertAll().
     * Additionally, it checks if the four setters correctly update state afterward,
     * again by an assertAll().
     */
    @Test
    @SpiraTestCase(testCaseId = 50919)
    public void
 defaultConstructorAndSetters_whenCalled_setDefaultsThenUpdateFields() {
        // Arrange
        Account acc = new Account();

        // Act & Assert (default constructor's initial state)
        assertAll("Default constructor sets safe defaults",
                () -> assertNull(acc.getName()),
                () -> assertEquals(0, acc.getAccountNumber()),
                () -> assertNull(acc.getPIN()),
                () -> assertEquals(0.0, acc.getAmount())
        );

        // Act (setters)
        acc.setName("Athish");
        acc.setAccountNumber(87654321);
        acc.setPIN("4321");
        acc.setAmount(2000.0);

        // Assert (state after setters)
        assertAll("Setters correctly update state",
                () -> assertEquals("Athish", acc.getName()),
                () -> assertEquals(87654321, acc.getAccountNumber()),
                () -> assertEquals("4321", acc.getPIN()),
                () -> assertEquals(2000.0, acc.getAmount())
        );
    }

    /**
     * This test checks whether Account rejects a negative monetary amount, using two
     * entry points at once via assertAll(): the parameterized constructor, and
     * setAmount() called directly. This second check matters specifically because
     * Bank.transfer() and Bank.withdraw() call setAmount() directly when adjusting
     * balances, not the constructor, so this is the actual pathway through which a real
     * negative-amount defect would occur in practice.
     */
    @Test
    @SpiraTestCase(testCaseId = 50933)
    public void
 setAmount_withNegativeValue_shouldBeRejected() {
        // Arrange & Act
        Account viaConstructor = new Account("Athish1", 11112222, "0000", -500.0);
        Account viaSetter = new Account();
        viaSetter.setAmount(-750.0);

        // Assert
        assertAll("Negative amounts should be rejected (currently a known defect)",
                () -> assertTrue(viaConstructor.getAmount() >= 0,
                        "Constructor should not allow a negative resulting amount"),
                () -> assertTrue(viaSetter.getAmount() >= 0,
                        "setAmount() should not allow a negative amount")
        );
    }

    /**
     * This test checks the Serializable interface and verifies the serialization
     * behaviour by using an in-memory byte stream instead of a real file, to keep it a
     * unit test rather than an integration test.
     */
    @Test
    @SpiraTestCase(testCaseId = 50931)
    public void
 serialization_roundTrip_preservesAllFields() throws Exception {
        // Arrange
        Account original = new Account("Athish1", 33333333, "3333", 200.0);

        // Act (serialize, then deserialize)
        java.io.ByteArrayOutputStream byteOut = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(byteOut);
        out.writeObject(original);
        out.close();

        java.io.ByteArrayInputStream byteIn = new java.io.ByteArrayInputStream(byteOut.toByteArray());
        java.io.ObjectInputStream in = new java.io.ObjectInputStream(byteIn);
        Account restored = (Account) in.readObject();
        in.close();

        // Assert
        assertAll("Account survives serialization intact",
                () -> assertEquals(original.getName(), restored.getName()),
                () -> assertEquals(original.getAccountNumber(), restored.getAccountNumber()),
                () -> assertEquals(original.getPIN(), restored.getPIN()),
                () -> assertEquals(original.getAmount(), restored.getAmount())
        );
    }
}