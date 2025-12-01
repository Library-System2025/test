import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Properties;

/**
 * Unit tests for the EmailService class.
 * Verifies interaction with JavaMail Transport.
 * 
 * @author Zainab
 * @version 1.0
 */

public class EmailServiceTest {

	/**
     * Tests if sendEmail actually calls Transport.send().
     * Uses MockedStatic for the static method Transport.send().
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

}
