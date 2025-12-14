import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the {@link EmailOverdueSubscriber} class.
 * <p>
 * This test suite uses Mockito to verify that the subscriber correctly formats the email content
 * and invokes the underlying {@link EmailService} with the expected parameters.
 * It ensures full coverage of both successful sending and error handling scenarios.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */
public class EmailOverdueSubscriberTest {

    /**
     * Default constructor for the test class.
     */
    public EmailOverdueSubscriberTest() {
    }

    /**
     * Verifies that the {@code update} method constructs a properly formatted email body
     * containing details of the overdue items and calls the email service when a valid email is provided.
     */
    @Test
    void testUpdate_sendsEmailWithCorrectArgs() {
        EmailService mockEmailService = mock(EmailService.class);
        EmailOverdueSubscriber subscriber =
                new EmailOverdueSubscriber(mockEmailService, "library@najah.edu");

        Media m1 = mock(Media.class);
        when(m1.getTitle()).thenReturn("Clean Code");
        when(m1.getDueDate()).thenReturn("2025-12-01");
        when(m1.getFineAmount()).thenReturn(3.5);

        List<Media> overdueList = new ArrayList<>();
        overdueList.add(m1);

        subscriber.update("u1", "u1@mail.com", overdueList);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockEmailService, times(1))
                .sendEmail(eq("u1@mail.com"),
                           eq("Library Overdue Notice"),                          
                           bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Clean Code"));
        assertTrue(body.contains("2025-12-01"));
        assertTrue(body.contains("3.50")); 
    }

    /**
     * Verifies that the {@code update} method does not attempt to send an email
     * if the provided email address is null or empty.
     */
    @Test
    void testUpdate_skipsIfEmailIsNull() {
        EmailService mockEmailService = mock(EmailService.class);
        EmailOverdueSubscriber subscriber =
                new EmailOverdueSubscriber(mockEmailService, "library@najah.edu");

        List<Media> overdueList = new ArrayList<>();
        
        subscriber.update("userWithoutEmail", null, overdueList);

        verify(mockEmailService, never()).sendEmail(any(), any(), any());
    }

    /**
     * Verifies that the {@code getFromEmail} method returns the correctly configured sender address.
     */
    @Test
    void testGetFromEmail() {
        EmailService mockEmailService = mock(EmailService.class);
        String expectedEmail = "sender@test.com";
        EmailOverdueSubscriber subscriber =
                new EmailOverdueSubscriber(mockEmailService, expectedEmail);

        assertEquals(expectedEmail, subscriber.getFromEmail());
    }
}