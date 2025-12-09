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
 * handles the authentication and message transmission logic.
 * </p>
 *
 * @author Zainab
 * @version 1.0
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
     * Configures SMTP properties for Gmail (TLS, port 587) and initiates a session.
     * Catches {@link MessagingException} and wraps it in a {@link RuntimeException}.
     * </p>
     *
     * @param to      The recipient's email address.
     * @param subject The subject line of the email.
     * @param body    The body text of the email.
     * @throws RuntimeException if the email fails to send due to authentication or connection issues.
     */

    public void sendEmail(String to, String subject, String body) {

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
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("📧 Email sent successfully to: " + to);

        } catch (MessagingException e) {
        	System.err.println("Error: " + e.getMessage());
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
