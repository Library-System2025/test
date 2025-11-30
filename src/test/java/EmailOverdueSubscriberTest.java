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

public class EmailOverdueSubscriberTest {

    @Test
    void testUpdate_sendsEmailWithCorrectArgs() {
        // Arrange
        EmailService mockEmailService = mock(EmailService.class);
        EmailOverdueSubscriber subscriber =
                new EmailOverdueSubscriber(mockEmailService, "library@najah.edu");

        // نعمل Media وهمي بالـ Mockito (بدون كتب حقيقية)
        Media m1 = mock(Media.class);
        when(m1.getTitle()).thenReturn("Clean Code");
        when(m1.getDueDate()).thenReturn("2025-12-01");
        when(m1.getFineAmount()).thenReturn(3.5);

        List<Media> overdueList = new ArrayList<>();
        overdueList.add(m1);

        // Act
        subscriber.update("u1", "u1@mail.com", overdueList);

        // Assert
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockEmailService, times(1))
                .sendEmail(eq("u1@mail.com"),
                           eq("Library Overdue Notice"),
                           bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Clean Code"));
        assertTrue(body.contains("2025-12-01"));
        assertTrue(body.contains("3.50")); // formatted fine
    }
}
