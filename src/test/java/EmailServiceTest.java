import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Transport;
import jakarta.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit Test for EmailService.
 * 
 * @author Zainab
 * @version 1.0
 */
public class EmailServiceTest {

    /**
     * Default Constructor.
     */
    public EmailServiceTest() {}

    /**
     * Tests sending email successfully (Mocked).
     * @throws Exception If fails.
     */
    @Test
    void testSendEmail_invokesTransportSend() throws Exception {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            emailService.sendEmail("target@mail.com", "Test", "Body");

            mockedTransport.verify(
                    () -> Transport.send(any(Message.class)),
                    times(1)
            );
        }
    }

    /**
     * Tests handling of MessagingException.
     * @throws Exception If fails.
     */
    @Test
    void testSendEmail_Exception() throws Exception {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            mockedTransport.when(() -> Transport.send(any(Message.class)))
                    .thenThrow(new MessagingException("fail"));

            assertThrows(RuntimeException.class, () -> 
                emailService.sendEmail("x@mail.com", "sub", "body")
            );
        }
    }

    /**
     * Tests authentication object creation.
     */
    @Test
    void testAuth() {
        EmailService s = new EmailService("u", "p");
        PasswordAuthentication pa = s.createPasswordAuthentication();
        assertEquals("u", pa.getUserName());
        assertEquals("p", pa.getPassword());
    }
    
    /**
     * Tests reminder delegation.
     */
    @Test
    void testReminder() {
        EmailService s = spy(new EmailService("u", "p"));
        doNothing().when(s).sendEmail(any(), any(), any());
        s.sendReminder("to", "sub", "body");
        verify(s, times(1)).sendEmail("to", "sub", "body");
    }
}