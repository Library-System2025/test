import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import java.util.*;

/**
 * Unit tests for the OverduePublisher class.
 * Verifies the Observer Pattern implementation.
 * 
 * @author Zainab
 * @version 1.0
 */

public class OverduePublisherTest {

	/**
     * Verifies that notifySubscribers triggers the update method on all subscribers.
     */
	
    @Test
    void testNotifySubscribers_callsUpdateOnAllSubscribers() {

        OverduePublisher publisher = new OverduePublisher();

        // Mock subscribers
        OverdueSubscriber sub1 = mock(OverdueSubscriber.class);
        OverdueSubscriber sub2 = mock(OverdueSubscriber.class);

        Media item = mock(Media.class);
        List<Media> list = Collections.singletonList(item);

        publisher.subscribe(sub1);
        publisher.subscribe(sub2);

        publisher.notifySubscribers("u1", "u1@mail.com", list);

        verify(sub1, times(1))
                .update("u1", "u1@mail.com", list);

        verify(sub2, times(1))
                .update("u1", "u1@mail.com", list);
    }
}
