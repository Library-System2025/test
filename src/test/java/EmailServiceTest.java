import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Transport;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the EmailService class.
 * Verifies interaction with JavaMail Transport and delegation behavior.
 */
public class EmailServiceTest {

    /**
     * Tests if sendEmail actually calls Transport.send().
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
     * Tests exception handling when email sending fails.
     */
    @Test
    void testSendEmail_whenTransportThrows_wrapsInRuntimeException() throws Exception {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {

            mockedTransport.when(() -> Transport.send(any(Message.class)))
                    .thenThrow(new jakarta.mail.MessagingException("fail"));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> emailService.sendEmail("x@mail.com", "sub", "body")
            );

            assertTrue(ex.getMessage().contains("Failed to send email"));
        }
    }

    /**
     * Tests that sendReminder delegates to sendEmail with the same parameters.
     */
    @Test
    void testSendReminder_delegatesToSendEmail() {

        EmailService spyService = spy(new EmailService("sender@gmail.com", "pass123"));

        // ما بدنا فعليًا نبعت إيميل بالتست
        doNothing().when(spyService).sendEmail(anyString(), anyString(), anyString());

        spyService.sendReminder("user@mail.com", "Reminder", "Body here");

        verify(spyService, times(1))
                .sendEmail("user@mail.com", "Reminder", "Body here");
    }

    /**
     * Tests that createPasswordAuthentication returns the expected username and password.
     * This indirectly verifies the logic used inside the Authenticator of EmailService.
     */
    @Test
    void testCreatePasswordAuthentication_usesGivenUsernameAndPassword() {

        String user = "sender@gmail.com";
        String pass = "appPassword123";

        EmailService service = new EmailService(user, pass);

        PasswordAuthentication pa = service.createPasswordAuthentication();

        assertEquals(user, pa.getUserName());
        assertEquals(pass, new String(pa.getPassword()));
    }
}
