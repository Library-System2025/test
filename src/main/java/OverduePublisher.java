
/**
 * Publisher class for the Observer pattern.
 * Manages subscribers and notifies them of overdue events.
 * 
 * @author Zainab
 * @version 1.0
 */

public class OverduePublisher {

    private final java.util.List<OverdueSubscriber> subscribers = new java.util.ArrayList<>();

    /**
     * Adds a subscriber to the list.
     * @param s The subscriber to add.
     */
    
    public void subscribe(OverdueSubscriber s) {
        subscribers.add(s);
    }
    
    /**
     * Notifies all subscribers about overdue items.
     * @param username The username.
     * @param email The user's email.
     * @param overdueItems The list of overdue items.
     */

    public void notifySubscribers(String username, String email, java.util.List<Media> overdueItems) {
        for (OverdueSubscriber s : subscribers) {
            s.update(username, email, overdueItems);
        }
    }
}
