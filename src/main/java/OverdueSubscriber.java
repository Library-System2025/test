

public interface OverdueSubscriber {
    void update(String username, String email, java.util.List<Media> overdueItems);
}
