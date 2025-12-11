import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Transport;
import jakarta.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Comprehensive Unit Test suite for the {@link EmailService} class.
 * <p>
 * This class validates the interaction with the Jakarta Mail API. It uses {@link MockedStatic} 
 * to mock the static {@link Transport#send(Message)} method, ensuring that actual emails 
 * are not sent during testing, while verifying proper method delegation and error handling.
 * </p>
 *
 * @author Zainab
 * @version 1.0
 */
public class EmailServiceTest {

    /**
     * Default constructor for EmailServiceTest.
     */
    public EmailServiceTest() {
    }

    /**
     * Verifies that {@link EmailService#sendEmail} successfully invokes the static 
     * {@link Transport#send} method when valid parameters are provided.
     *
     * @throws Exception if mocking fails.
     */
    @Test
    void testSendEmail_invokesTransportSend() throws Exception {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            emailService.sendEmail(
                    "target@mail.com",
                    "Test Subject",
                    "Hello world!"
            );

            mockedTransport.verify(
                    () -> Transport.send(any(Message.class)),
                    times(1)
            );
        }
    }

    /**
     * Verifies that {@link EmailService#sendEmail} wraps checked {@link MessagingException}
     * into a runtime exception, allowing the application to handle failures gracefully.
     *
     * @throws Exception if mocking fails.
     */
    @Test
    void testSendEmail_whenTransportThrows_wrapsInRuntimeException() throws Exception {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            mockedTransport.when(() -> Transport.send(any(Message.class)))
                    .thenThrow(new MessagingException("fail"));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> emailService.sendEmail("x@mail.com", "sub", "body")
            );

            assertTrue(ex.getMessage().contains("Failed to send email"));
        }
    }

    /**
     * Verifies that the {@code sendReminder} convenience method delegates execution 
     * to the {@code sendEmail} method with identical parameters.
     */
    @Test
    void testSendReminder_delegatesToSendEmail() {
        EmailService spyService = spy(new EmailService("sender@gmail.com", "pass123"));

        doNothing().when(spyService).sendEmail(anyString(), anyString(), anyString());

        spyService.sendReminder("user@mail.com", "Reminder", "Body here");

        verify(spyService, times(1))
                .sendEmail("user@mail.com", "Reminder", "Body here");
    }

    /**
     * Verifies that {@code createPasswordAuthentication} correctly encapsulates 
     * the username and password provided during initialization.
     */
    @Test
    void testCreatePasswordAuthentication_usesGivenUsernameAndPassword() {
        String user = "sender@gmail.com";
        String pass = "appPassword123";

        EmailService service = new EmailService(user, pass);

        PasswordAuthentication pa = service.createPasswordAuthentication();

        assertEquals(user, pa.getUserName());
        assertEquals(pass, pa.getPassword());
    }
}