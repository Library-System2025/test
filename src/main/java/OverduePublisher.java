public class OverduePublisher {

    private final java.util.List<OverdueSubscriber> subscribers = new java.util.ArrayList<>();

    public void subscribe(OverdueSubscriber s) {
        subscribers.add(s);
    }

    public void notifySubscribers(String username, String email, java.util.List<Media> overdueItems) {
        for (OverdueSubscriber s : subscribers) {
            s.update(username, email, overdueItems);
        }
    }
}
