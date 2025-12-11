import java.util.List;

/**
 * Implementation of the {@link OverdueSubscriber} interface that handles email notifications.
 * <p>
 * This class follows the Observer design pattern. When notified about overdue items,
 * it constructs a formatted email containing details of the books and fines,
 * then delegates the sending process to the {@link EmailService}.
 * <br>
 * If the provided email address is null or blank, the notification is skipped and
 * an informational message is logged instead of attempting to send.
 * </p>
 * 
 * @author Zainab
 * @version 1.1
 */
public class EmailOverdueSubscriber implements OverdueSubscriber {

    private final EmailService emailService;
    private final String fromEmail;

    /**
     * Constructs a new EmailOverdueSubscriber.
     *
     * @param emailService The service instance used to send the actual emails.
     * @param fromEmail    The email address from which notifications will be sent.
     *                     This value can be used for metadata or future extensions.
     */
    public EmailOverdueSubscriber(EmailService emailService, String fromEmail) {
        this.emailService = emailService;
        this.fromEmail = fromEmail;
    }

    /**
     * Receives an update about a user with overdue items and triggers an email notification.
     * <p>
     * This method:
     * <ul>
     *   <li>Validates that the recipient email is not null or blank.</li>
     *   <li>Builds a detailed message body listing the title, due date, and fine for each overdue item.</li>
     *   <li>Calculates and displays the total fine at the end of the message.</li>
     *   <li>Delegates the actual sending to {@link EmailService#sendEmail(String, String, String)}.</li>
     * </ul>
     * If the email parameter is null or blank, the method logs a message and exits without sending.
     * </p>
     *
     * @param username     The name of the user with overdue items.
     * @param email        The recipient email address of the user.
     * @param overdueItems The list of {@link Media} items that are overdue.
     */
    @Override
    public void update(String username, String email, List<Media> overdueItems) {

        if (email == null || email.trim().isEmpty()) {
            System.out.printf("Skipping reminder for user '%s': empty email address.%n", username);
            return;
        }

        String subject = "Library Overdue Notice";

        StringBuilder body = new StringBuilder();
        body.append(String.format("Dear %s,%n%n", username));
        body.append(String.format("You have %d overdue book(s):%n%n", overdueItems.size()));

        double totalFine = 0.0;

        for (Media m : overdueItems) {
            body.append("Title: ").append(m.getTitle()).append(System.lineSeparator());
            body.append("Due date: ").append(m.getDueDate()).append(System.lineSeparator());
            body.append("Fine: $")
                .append(String.format("%.2f", m.getFineAmount()))
                .append(System.lineSeparator())
                .append(System.lineSeparator());

            totalFine += m.getFineAmount();
        }

        body.append("-------------------------").append(System.lineSeparator());
        body.append(String.format("Total fine: $%.2f%n%n", totalFine));
        body.append("Best regards,").append(System.lineSeparator());
        body.append("An-Najah Library System");

        System.out.println("Sending overdue reminder to: " + email);
        emailService.sendEmail(email, subject, body.toString());
    }

    /**
     * Returns the configured "from" email address used by this subscriber.
     *
     * @return The configured sender email address.
     */
    public String getFromEmail() {
        return fromEmail;
    }
}
