

/**
 * Implementation of the {@link OverdueSubscriber} interface that handles email notifications.
 * <p>
 * This class follows the Observer design pattern. When notified about overdue items,
 * it constructs a formatted email containing details of the books and fines, 
 * then delegates the sending process to the {@link EmailService}.
 * </p>
 * 
 * @author Zainab
 * @version 1.0
 */

public class EmailOverdueSubscriber implements OverdueSubscriber {

    private final EmailService emailService;
    private final String fromEmail;
    
    /**
     * Constructs a new EmailOverdueSubscriber.
     *
     * @param emailService The service instance used to send the actual emails.
     * @param fromEmail    The email address from which notifications will be sent.
     */

    public EmailOverdueSubscriber(EmailService emailService, String fromEmail) {
        this.emailService = emailService;
        this.fromEmail = fromEmail;
    }
    
    /**
     * Receives an update about a user with overdue items and triggers an email notification.
     * <p>
     * Constructs a detailed message body listing the title, due date, and fine for each overdue item,
     * along with the total fine amount.
     * </p>
     *
     * @param username     The name of the user with overdue items.
     * @param email        The recipient email address of the user.
     * @param overdueItems The list of {@link Media} items that are overdue.
     */

    @Override
    public void update(String username, String email, java.util.List<Media> overdueItems) {

        String subject = "Library Overdue Notice";

        StringBuilder body = new StringBuilder();
        body.append(String.format("Dear %s,\n\n", username));
        body.append(String.format("You have %d overdue book(s):\n\n", overdueItems.size()));

        double totalFine = 0;

        for (Media m : overdueItems) {
            body.append("📘 Title: ").append(m.getTitle()).append("\n");
            body.append("📅 Due date: ").append(m.getDueDate()).append("\n");
            body.append("⏳ Fine: $").append(String.format("%.2f", m.getFineAmount())).append("\n\n");

            totalFine += m.getFineAmount();
        }

        body.append("-------------------------\n");
        body.append(String.format("Total fine: $%.2f\n\n", totalFine));
        body.append("Best regards,\nAn-Najah Library System");

        emailService.sendEmail(email, subject, body.toString());
    }
}
