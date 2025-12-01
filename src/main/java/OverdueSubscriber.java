
/**
 * Interface for the Observer pattern.
 * Subscribers implement this to receive updates about overdue items.
 * 
 * @author Zainab
 * @version 1.0
 */

public interface OverdueSubscriber {
	
	/**
     * Called when a notification needs to be sent.
     * @param username The user's name.
     * @param email The user's email.
     * @param overdueItems List of overdue media items.
     */
	
    void update(String username, String email, java.util.List<Media> overdueItems);
}
