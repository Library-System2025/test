public class EmailOverdueSubscriber implements OverdueSubscriber {

    private final EmailService emailService;
    private final String fromEmail;

    public EmailOverdueSubscriber(EmailService emailService, String fromEmail) {
        this.emailService = emailService;
        this.fromEmail = fromEmail;
    }

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
