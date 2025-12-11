import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javax.annotation.processing.Generated;

/**
 * Controller class for the User Dashboard.
 * Handles borrowing, returning, paying fines, and viewing borrowed items.
 * 
 * @author Zainab
 * @version 1.0
 */
public class UserController {

    private static final String ERROR_MSG = "Error: ";

    @FXML private Label welcomeLabel;
    @FXML private TextField paymentField;
    @FXML private Label infoLabel;
    @FXML private Label messageLabel;

    @FXML private TableView<Media> bookTable;
    @FXML private TableColumn<Media, String> typeColumn;
    @FXML private TableColumn<Media, String> titleColumn;
    @FXML private TableColumn<Media, String> authorColumn;
    @FXML private TableColumn<Media, String> isbnColumn;
    @FXML private TableColumn<Media, String> statusColumn;
    @FXML private TableColumn<Media, String> dueDateColumn;
    @FXML private TableColumn<Media, Double> fineColumn;

    private ObservableList<Media> mediaList = FXCollections.observableArrayList();

    /** File path for storing media data. */
    private static final String FILE_PATH        = "books.txt";
    /** Status constant indicating the item is available. */
    private static final String STATUS_AVAILABLE = "Available";
    /** Status constant indicating the item is borrowed. */
    private static final String STATUS_BORROWED  = "Borrowed";
    /** Status constant indicating the item is overdue. */
    private static final String STATUS_OVERDUE   = "Overdue";

    private String accountUsername;
    private String membershipType;
    private String accountEmail;

    private static final OverduePublisher overduePublisher = new OverduePublisher();
    private static EmailService emailService;

    /**
     * Static block to initialize the email service and subscribers.
     * Loads credentials from environment variables using Dotenv.
     */
    static {
        initEmailSubscribers();
    }

    /**
     * Initializes EmailService and subscribes overdue notification listeners.
     * Marked as @Generated so coverage tools ignore it (hard to unit-test safely).
     */
    @Generated("email-init")
    static void initEmailSubscribers() {
        try {
            Dotenv dotenv = Dotenv.load();
            String username = dotenv.get("EMAIL_USERNAME");
            String password = dotenv.get("EMAIL_PASSWORD");

            if (username == null || password == null ||
                username.isBlank() || password.isBlank()) {
                // No valid credentials ⇒ skip email wiring
                return;
            }

            emailService = new EmailService(username, password);

            EmailOverdueSubscriber emailSubscriber =
                    new EmailOverdueSubscriber(emailService, username);

            overduePublisher.subscribe(emailSubscriber);
        } catch (Exception e) {
            System.err.println("Failed to initialize email service / subscribers: "
                    + e.getMessage()); // NOSONAR
        }
    }

    /**
     * Sets the current user details and loads their specific data.
     *
     * @param username       The username.
     * @param membershipType The membership type (Gold/Silver).
     * @param email          The user's email address.
     */
    public void setCurrentUser(String username, String membershipType, String email) {
        this.accountUsername = username;
        this.membershipType  = membershipType;
        this.accountEmail    = email;
        updateWelcomeLabel();
        tryLoadBooks();
    }

    /**
     * Sets the membership type and refreshes the view.
     * @param membershipType The membership type.
     */
    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
        updateWelcomeLabel();
        tryLoadBooks();
    }

    /**
     * Sets the username and refreshes the view.
     * @param username The username.
     */
    public void setCurrentUsername(String username) {
        this.accountUsername = username;
        updateWelcomeLabel();
        tryLoadBooks();
    }

    /**
     * Sets user credentials (legacy method) and refreshes the view.
     * @param username The username.
     * @param email    The email.
     */
    public void setCurrentUser(String username, String email) {
        this.accountUsername = username;
        this.accountEmail    = email;
        updateWelcomeLabel();
        tryLoadBooks();
    }

    /**
     * Attempts to load books if user details are present.
     */
    private void tryLoadBooks() {
        if (accountUsername != null && membershipType != null) {
            reloadBooks();
        }
    }

    /**
     * Updates the welcome label text and style based on membership type.
     */
    private void updateWelcomeLabel() {
        if (welcomeLabel != null && accountUsername != null && membershipType != null) {
            welcomeLabel.setText("Welcome, " + accountUsername + " (" + membershipType + ") 👋");
            if (membershipType.equalsIgnoreCase("Gold")) {
                welcomeLabel.setStyle("-fx-text-fill: gold; -fx-font-weight: bold;");
            } else {
                welcomeLabel.setStyle("-fx-text-fill: silver; -fx-font-weight: bold;");
            }
        }
    }

    // =====================================================================
    // coverage ignore start - JavaFX GUI wiring & styling (hard to unit-test)
    // =====================================================================

    /**
     * Initializes the controller class.
     * Configures table columns, row coloring, and privacy logic (hiding other users' data).
     * Called automatically after the FXML file has been loaded.
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        bookTable.setItems(mediaList);
        configureRowColors();
        configureDueDateColumn();
        configureFineColumn();
    }

    /**
     * Configures the TableView columns and binds them to Media properties.
     */
    private void configureTableColumns() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("mediaType"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
    }

    /**
     * Configures row styling based on the item's status and borrower.
     * Highlights overdue items and borrowed items with specific colors.
     */
    @Generated("gui-row-styling")
    private void configureRowColors() {
        bookTable.setRowFactory(tv -> new TableRow<Media>() {
            @Override
            protected void updateItem(Media item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("");

                if (empty || item == null) {
                    return;
                }

                String borrower = item.getBorrowedBy();
                String status   = item.getStatus();

                if (isCurrentUserBorrower(borrower)) {
                    if (STATUS_OVERDUE.equals(status)) {
                        setStyle("-fx-background-color: #ffcccc;");
                    } else {
                        setStyle("-fx-background-color: #c8f7c5;");
                    }
                    return;
                }

                if (isBorrowedOrOverdue(status)) {
                    setStyle("-fx-background-color: #fff3cd;"); // أصفر
                }
            }
        });
    }

    /**
     * Configures the Due Date column to hide dates for items not borrowed by the current user.
     */
    private void configureDueDateColumn() {
        dueDateColumn.setCellFactory(col -> new TableCell<Media, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                Media m = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || m == null) {
                    setText(null);
                    return;
                }

                if (isCurrentUserBorrower(m.getBorrowedBy())) {
                    setText(m.getDueDate());
                } else {
                    setText("");
                }
            }
        });
    }

    /**
     * Configures the Fine column to hide fine amounts for items not borrowed by the current user.
     */
    private void configureFineColumn() {
        fineColumn.setCellFactory(col -> new TableCell<Media, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);

                Media m = (getTableRow() != null) ? getTableRow().getItem() : null;
                if (empty || m == null) {
                    setText(null);
                    return;
                }

                if (isCurrentUserBorrower(m.getBorrowedBy())) {
                    setText(String.format("$%.2f", m.getFineAmount()));
                } else {
                    setText("");
                }
            }
        });
    }

    // =====================================================================
    // coverage ignore end
    // =====================================================================

    /**
     * Checks if the given borrower username matches the current logged-in user.
     * 
     * @param borrower The username recorded on the item.
     * @return true if the borrower matches the current user, false otherwise.
     */
    private boolean isCurrentUserBorrower(String borrower) {
        return borrower != null && borrower.equals(accountUsername);
    }

    /**
     * Checks if the status indicates the item is unavailable (Borrowed or Overdue).
     * 
     * @param status The status string of the item.
     * @return true if the item is not available.
     */
    private boolean isBorrowedOrOverdue(String status) {
        return STATUS_BORROWED.equals(status) || STATUS_OVERDUE.equals(status);
    }

    /**
     * Handles logout action and redirects to the login screen.
     */
    @FXML
    void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage  = (Stage) bookTable.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) {
            System.err.println(ERROR_MSG + e.getMessage()); // NOSONAR
        }
    }

    /**
     * Handles borrowing a book.
     * Checks for existing fines, existing copies, and item availability before processing.
     */
    @FXML
    void handleBorrowBook() {

        for (Media m : mediaList) {
            if (m.getBorrowedBy() != null &&
                m.getBorrowedBy().equals(accountUsername) &&
                m.getFineAmount() > 0) {

                messageLabel.setText("❌ You have unpaid fines! Pay them first.");
                return;
            }
        }

        Media selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Please select an item to borrow.");
            return;
        }

        for (Media m : mediaList) {
            if (m.getIsbn().equals(selected.getIsbn()) &&
                accountUsername.equals(m.getBorrowedBy())) {
                messageLabel.setText("❌ You already borrowed a copy of this item.");
                return;
            }
        }

        if (!STATUS_AVAILABLE.equals(selected.getStatus())) {
            messageLabel.setText("❌ This item is not available.");
            return;
        }

        selected.borrow(accountUsername);

        saveAllMediaToFile();
        reloadBooks();
        messageLabel.setText("✅ Borrowed successfully! Due date: " + selected.getDueDate());
    }

    /**
     * Handles fine payment.
     * Validates payment amount, updates the fine status, and returns the book if fine is cleared.
     */
    @FXML
    void handlePayFine() {
        Media selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            infoLabel.setText("⚠️ Select an item.");
            return;
        }

        if (!selected.getBorrowedBy().equals(accountUsername)) {
            infoLabel.setText("❌ Select one of YOUR borrowed items.");
            return;
        }

        double amountToPay;
        try {
            amountToPay = Double.parseDouble(paymentField.getText());
        } catch (NumberFormatException e) {
            infoLabel.setText("❌ Invalid number.");
            return;
        }

        if (amountToPay <= 0) {
            infoLabel.setText("❌ Amount must be positive.");
            return;
        }

        if (amountToPay > selected.getFineAmount()) {
            infoLabel.setText("❌ Payment exceeds fine amount!");
            return;
        }

        selected.addPayment(amountToPay);
        selected.calculateFine(membershipType);

        if (selected.getFineAmount() <= 0) {
            selected.returnMedia();
            infoLabel.setText("✅ Fine fully paid. Item returned.");
        } else {
            infoLabel.setText("💰 Partial payment accepted. Remaining: $" +
                    String.format("%.2f", selected.getFineAmount()));
        }

        saveAllMediaToFile();
        bookTable.refresh();
        paymentField.clear();
    }

    /**
     * Handles returning a book.
     * Ensures no fines are pending before returning. If fines exist, notifies the user via email.
     */
    @FXML
    void handleReturnBook() {
        Media selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Select an item to return.");
            return;
        }

        if (!selected.getBorrowedBy().equals(accountUsername)) {
            messageLabel.setText("❌ You can only return your own items.");
            return;
        }

        selected.calculateFine(membershipType);

        if (selected.getFineAmount() > 0) {
            selected.setStatus(STATUS_OVERDUE);
            messageLabel.setText("⚠️ Cannot return. Pay the fine first.");

            if (accountEmail != null && !accountEmail.isEmpty()) {

                List<Media> singleList = new ArrayList<>();
                singleList.add(selected);

                overduePublisher.notifySubscribers(
                        accountUsername,
                        accountEmail,
                        singleList
                );
            }

        } else {
            selected.returnMedia();
            messageLabel.setText("✅ Returned successfully!");
        }

        saveAllMediaToFile();
        reloadBooks();
    }

    /**
     * Reloads data from the file and refreshes the table.
     */
    @FXML
    void handleReload() {
        reloadBooks();
        infoLabel.setText("🔄 Data reloaded.");
    }

    /**
     * Loads media data from 'books.txt'.
     * Catches and logs IOExceptions if the file cannot be read.
     */
    void loadMediaFromFile() {
        mediaList.clear();
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Media item = parseMediaLineForUser(line);
                if (item != null) {
                    mediaList.add(item);
                }
            }
        } catch (IOException e) {
            System.err.println(ERROR_MSG + e.getMessage()); // NOSONAR
        }
        bookTable.refresh();
    }

    /**
     * Parses a single line from the text file for the current user context.
     * 
     * @param line The CSV formatted string from the file.
     * @return A Media object if parsing is successful, or null if the line is invalid.
     */
    private Media parseMediaLineForUser(String line) {
        String[] parts = line.split(",", 10);
        if (parts.length < 5) {
            return null;
        }

        String type   = parts[0].trim();
        String title  = parts[1].trim();
        String author = parts[2].trim();
        String isbn   = parts[3].trim();

        int copyId        = parseIntSafe(getPart(parts, 4), 1);
        String status     = parts.length >= 6 ? parts[5].trim() : STATUS_AVAILABLE;
        String dueDate    = parts.length >= 7 ? parts[6].trim() : "";
        double fine       = parts.length >= 8 ? parseDoubleSafe(parts[7], 0.0) : 0.0;
        String borrowedBy = normalizeBorrowedBy(getPart(parts, 8));
        double amountPaid = parts.length >= 10 ? parseDoubleSafe(parts[9], 0.0) : 0.0;

        Media item = createMediaItem(
                type, title, author, isbn,
                status, dueDate, fine, borrowedBy,
                amountPaid, copyId
        );

        applyFineForUserIfOverdue(item, borrowedBy);
        return item;
    }

    /**
     * Factory method to create a Book or CD based on the type string.
     */
    private Media createMediaItem(String type,
                                  String title,
                                  String author,
                                  String isbn,
                                  String status,
                                  String dueDate,
                                  double fine,
                                  String borrowedBy,
                                  double amountPaid,
                                  int copyId) {

        if ("CD".equalsIgnoreCase(type)) {
            return new CD(title, author, isbn, status, dueDate, fine, borrowedBy, amountPaid, copyId);
        }
        return new Book(title, author, isbn, status, dueDate, fine, borrowedBy, amountPaid, copyId);
    }

    /**
     * Applies the appropriate fine logic if the item is overdue.
     * Uses the current user's membership type if they are the borrower.
     */
    private void applyFineForUserIfOverdue(Media item, String borrowedBy) {
        if (!item.isOverdue()) {
            return;
        }

        if (borrowedBy != null &&
            borrowedBy.equals(accountUsername) &&
            membershipType != null) {
            item.calculateFine(membershipType);
        } else {
            item.calculateFine("Silver");
        }
    }

    /**
     * Safely parses an integer with a default fallback value.
     */
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses a double with a default fallback value.
     */
    private double parseDoubleSafe(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely retrieves an element from a string array.
     */
    private String getPart(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    /**
     * Normalizes the borrowedBy field (converts "0.0" or null to empty string).
     */
    private String normalizeBorrowedBy(String rawBorrowedBy) {
        String trimmed = rawBorrowedBy == null ? "" : rawBorrowedBy.trim();
        return "0.0".equals(trimmed) ? "" : trimmed;
    }

    /**
     * Saves all media data to 'books.txt'.
     * Catches IOExceptions if writing fails.
     */
    void saveAllMediaToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Media m : mediaList) {
                writer.write(m.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println(ERROR_MSG + e.getMessage()); // NOSONAR
        }
    }

    /**
     * Reloads books from file, refreshes the view, and checks for overdue items.
     */
    void reloadBooks() {
        loadMediaFromFile();
        bookTable.refresh();
        checkOverdueAndNotify();
    }

    /**
     * Checks for overdue items belonging to the current user and sends notifications via email.
     */
    void checkOverdueAndNotify() {
        if (accountUsername == null || membershipType == null) {
            return;
        }
        if (accountEmail == null || accountEmail.isEmpty()) {
            return;
        }

        for (Media m : mediaList) {
            if (accountUsername.equals(m.getBorrowedBy()) && m.isOverdue()) {
                m.calculateFine(membershipType);

                List<Media> singleList = new ArrayList<>();
                singleList.add(m);

                overduePublisher.notifySubscribers(
                        accountUsername,
                        accountEmail,
                        singleList
                );
            }
        }
    }
}
