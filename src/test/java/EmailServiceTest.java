import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Transport;
import jakarta.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the {@link EmailService} class.
 * <p>
 * This class verifies the functionality of the EmailService, including:
 * <ul>
 *   <li>Successful email transmission.</li>
 *   <li>Error handling when transmission fails.</li>
 *   <li>Input validation for senders and recipients.</li>
 *   <li>Authentication object creation.</li>
 *   <li>Delegation of reminder emails.</li>
 * </ul>
 * Uses Mockito to mock static {@link Transport} calls to avoid sending real emails during testing.
 *
 * @author Zainab
 * @version 1.0
 */
class EmailServiceTest {

    /**
     * Verifies that {@link EmailService#sendEmail} successfully invokes the
     * underlying Transport.send method when inputs are valid.
     */
    @Test
    void testSendEmail_Success() {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            emailService.sendEmail("target@mail.com", "Test Subject", "Test Body");

            mockedTransport.verify(
                    () -> Transport.send(any(Message.class)),
                    times(1)
            );
        }
    }

    /**
     * Verifies that {@link EmailService#sendEmail} wraps a {@link MessagingException}
     * into a {@link RuntimeException} when the transport layer fails.
     */
    @Test
    void testSendEmail_TransportFailure_ThrowsRuntimeException() {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            
            mockedTransport.when(() -> Transport.send(any(Message.class)))
                    .thenThrow(new MessagingException("Connection failed"));

            
            assertThrows(RuntimeException.class, () -> 
                emailService.sendEmail("target@mail.com", "Subject", "Body")
            );
        }
    }

    /**
     * Verifies that {@link EmailService#sendEmail} throws an {@link IllegalStateException}
     * if the service is initialized with an empty or null username (sender).
     */
    @Test
    void testSendEmail_InvalidSender_ThrowsException() {
        EmailService emailService = new EmailService("", "pass123");

        assertThrows(IllegalStateException.class, () -> 
            emailService.sendEmail("target@mail.com", "Subject", "Body")
        );
    }

    /**
     * Verifies that {@link EmailService#sendEmail} aborts the operation safely
     * (does not attempt to send) if the recipient address is null or empty.
     */
    @Test
    void testSendEmail_InvalidRecipient_DoesNotSend() {
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            
            emailService.sendEmail("", "Subject", "Body");

            
            mockedTransport.verify(
                    () -> Transport.send(any(Message.class)),
                    times(0)
            );
        }
    }

    /**
     * Verifies that {@link EmailService#createPasswordAuthentication()} correctly
     * returns an object containing the credentials provided in the constructor.
     */
    @Test
    void testCreatePasswordAuthentication() {
        String user = "user@test.com";
        String pass = "secret";
        EmailService service = new EmailService(user, pass);

        PasswordAuthentication auth = service.createPasswordAuthentication();

        assertEquals(user, auth.getUserName(), "Username should match the constructor input.");
        assertEquals(pass, auth.getPassword(), "Password should match the constructor input.");
    }
    
    /**
     * Verifies that {@link EmailService#sendReminder} correctly delegates
     * the call to {@link EmailService#sendEmail}.
     */
    @Test
    void testSendReminder_DelegatesToSendEmail() {
        
        EmailService service = spy(new EmailService("user@test.com", "pass"));

        
        doNothing().when(service).sendEmail(anyString(), anyString(), anyString());

        service.sendReminder("recipient@test.com", "Reminder", "Don't forget!");

        
        verify(service, times(1)).sendEmail("recipient@test.com", "Reminder", "Don't forget!");
    }
}