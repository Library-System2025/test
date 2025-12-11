import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Service class responsible for sending emails using the Jakarta Mail API.
 * <p>
 * This class encapsulates the SMTP configuration (specifically for Gmail) and
 * handles the authentication, validation, and message transmission logic.
 * </p>
 *
 * The class validates both the sender address (username) and the recipient
 * address before attempting to send an email.
 *
 * @author Zainab
 * @version 1.2
 */
public class EmailService {

    private final String username;
    private final String password;

    /**
     * Constructs an EmailService with specific credentials.
     *
     * @param username The SMTP username (sender's email address).
     * @param password The SMTP password or App Password.
     */
    public EmailService(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Creates a PasswordAuthentication object.
     * <p>
     * This method is package-private to facilitate unit testing by allowing
     * this logic to be overridden or mocked if necessary.
     * </p>
     *
     * @return A new {@link PasswordAuthentication} instance with the stored credentials.
     */
    PasswordAuthentication createPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
    }

    /**
     * Sends an email to a specified recipient.
     * <p>
     * This method:
     * <ul>
     *   <li>Validates that the sender (username) is non-null, non-empty, and a valid email.</li>
     *   <li>Validates that the recipient address is non-null and non-empty.</li>
     *   <li>Configures SMTP properties for Gmail (TLS, port 587) and initiates a session.</li>
     *   <li>Builds a {@link MimeMessage} and attempts to send it via {@link Transport}.</li>
     *   <li>Logs basic success/failure information to {@code System.out} / {@code System.err}.</li>
     * </ul>
     * If the recipient address is empty, the method returns without sending.
     * If Jakarta Mail throws a {@link MessagingException}, it is wrapped in a {@link RuntimeException}.
     * </p>
     *
     * @param to      The recipient's email address. Must not be null or blank.
     * @param subject The subject line of the email.
     * @param body    The body text of the email.
     * @throws RuntimeException if the email fails to send due to authentication, connection,
     *                          or invalid address issues.
     */
    public void sendEmail(String to, String subject, String body) {

        // Validate sender (username)
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalStateException(
                "Sender email (username) is empty or not configured. " +
                "Please initialize EmailService with a valid sender address."
            );
        }

        // Validate recipient
        if (to == null || to.trim().isEmpty()) {
            System.err.println("Cannot send email: recipient address is empty.");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return createPasswordAuthentication();
            }
        });

        try {
            // Validate and build sender address
            InternetAddress fromAddress = new InternetAddress(username);
            fromAddress.validate();

            // Validate and parse recipient address
            InternetAddress[] recipients = InternetAddress.parse(to, true);

            Message message = new MimeMessage(session);
            message.setFrom(fromAddress);
            message.setRecipients(Message.RecipientType.TO, recipients);
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);

        } catch (MessagingException e) {
            System.err.println(
                "Error while sending email. From='" + username + "', To='" + to + "'. Reason: " + e.getMessage()
            );
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends a reminder email.
     * <p>
     * This is a convenience wrapper around {@link #sendEmail(String, String, String)}.
     * </p>
     *
     * @param to      The recipient's email address.
     * @param subject The subject of the reminder.
     * @param body    The content of the reminder.
     */
    public void sendReminder(String to, String subject, String body) {
        sendEmail(to, subject, body);
    }
}
