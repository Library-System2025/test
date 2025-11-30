import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class UserTest {

    @Test
    void testConstructorAndGetters() {
        User user = new User("u1", "123", "User", "Gold");

        assertEquals("u1", user.getUsername());
        assertEquals("123", user.getPassword());
        assertEquals("User", user.getRole());
        assertEquals("Gold", user.getMembership());
    }

    @Test
    void testToFileFormat() {
        User user = new User("admin", "999", "Admin", "Silver");

        String expected = "admin,999,Admin,Silver";
        assertEquals(expected, user.toFileFormat());
    }
}
