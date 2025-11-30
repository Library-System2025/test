import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Properties;

public class EmailServiceTest {

    @Test
    void testSendEmail_invokesTransportSend() throws Exception {

        // Arrange
        EmailService emailService = new EmailService("sender@gmail.com", "pass123");

        // Mock Transport.send()
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {

            // Act
            emailService.sendEmail(
                    "target@mail.com",
                    "Test Subject",
                    "Hello world!"
            );

            // Assert -> verify static Transport.send was called exactly once
            mockedTransport.verify(
                    () -> Transport.send(any(Message.class)),
                    times(1)
            );
        }
    }
    
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
