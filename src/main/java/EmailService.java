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
 * Service class responsible for sending emails.
 * <p>
 * This class configures the SMTP server (Gmail) and handles the actual transmission of emails.
 * It is used by the system to send overdue notifications and reminders.
 * </p>
 *
 * @author Zainab
 * @version 1.0
 */

public class EmailService {

    private final String username;   
    private final String password;   

    /**
     * Constructs a new EmailService with the given credentials.
     * 
     * @param username The email account username (sender).
     * @param password The email account password (app password).
     */
    
    public EmailService(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Sends an email with the specified subject and body to the recipient.
     *
     * @param to      The recipient's email address.
     * @param subject The subject line of the email.
     * @param body    The body content of the email.
     * @throws RuntimeException If the email sending fails.
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
                return new PasswordAuthentication(username, password);
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
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Convenience method to send a reminder email.
     * Delegates to the {@link #sendEmail(String, String, String)} method.
     * 
     * @param to      The recipient's email address.
     * @param subject The subject line.
     * @param body    The body content.
     */
    
    public void sendReminder(String to, String subject, String body) {
       
        sendEmail(to, subject, body);
    }
}
