import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the EmailOverdueSubscriber class.
 * Verifies that emails are sent with correct content when updates occur.
 * 
 * @author Zainab
 * @version 1.0
 */

public class EmailOverdueSubscriberTest {

	 /**
     * Tests if the subscriber correctly formats and sends an email.
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
}
