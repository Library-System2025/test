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

/**
 * Controller class for the User Dashboard.
 * Handles borrowing, returning, paying fines, and viewing borrowed items.
 * 
 * @author Zainab
 * @version 1.0
 */

public class UserController {

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
    private static final String FILE_PATH        = "books.txt";
    private static final String STATUS_AVAILABLE = "Available";
    private static final String STATUS_BORROWED  = "Borrowed";
    private static final String STATUS_OVERDUE   = "Overdue";

    private String accountUsername;
    private String membershipType;
    private String accountEmail;

    private static final OverduePublisher overduePublisher = new OverduePublisher();
    private static EmailService emailService;

    /**
     * Static block to initialize the email service and subscribers.
     */
    static {
        try {
            Dotenv dotenv = Dotenv.load();
            String username = dotenv.get("EMAIL_USERNAME");
            String password = dotenv.get("EMAIL_PASSWORD");

            emailService = new EmailService(username, password);

            EmailOverdueSubscriber emailSubscriber =
                    new EmailOverdueSubscriber(emailService, username);

            overduePublisher.subscribe(emailSubscriber);
        } catch (Exception e) {
            System.err.println("Failed to initialize email service / subscribers: " + e.getMessage());
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
     * Sets the membership type.
     * @param membershipType The membership type.
     */
    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
        updateWelcomeLabel();
        tryLoadBooks();
    }

    /**
     * Sets the username.
     * @param username The username.
     */
    public void setCurrentUsername(String username) {
        this.accountUsername = username;
        updateWelcomeLabel();
        tryLoadBooks();
    }

    /**
     * Sets user credentials (legacy method).
     * @param username The username.
     * @param email    The email.
     */
    public void setCurrentUser(String username, String email) {
        this.accountUsername = username;
        this.accountEmail    = email;
        updateWelcomeLabel();
        tryLoadBooks();
    }

    private void tryLoadBooks() {
        if (accountUsername != null && membershipType != null) {
            reloadBooks();
        }
    }

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

    /**
     * Initializes the controller class.
     * Configures table columns, row coloring, and privacy logic (hiding other users' data).
     */
    @FXML
    public void initialize() {
        configureTableColumns();
        bookTable.setItems(mediaList);
        configureRowColors();
        configureDueDateColumn();
        configureFineColumn();
    }

    private void configureTableColumns() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("mediaType"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        fineColumn.setCellValueFactory(new PropertyValueFactory<>("fineAmount"));
    }

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

    private boolean isCurrentUserBorrower(String borrower) {
        return borrower != null && borrower.equals(accountUsername);
    }

    private boolean isBorrowedOrOverdue(String status) {
        return STATUS_BORROWED.equals(status) || STATUS_OVERDUE.equals(status);
    }

    /**
     * Handles logout action.
     */
    @FXML
    void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage  = (Stage) bookTable.getScene().getWindow();
            stage.setScene(new Scene(login));
        } catch (IOException e) {
        	System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Handles borrowing a book.
     * Checks for existing fines and item availability.
     */
    @FXML
    void handleBorrowBook() {
        // 1) أولاً: هل عنده غرامات غير مدفوعة؟
        for (Media m : mediaList) {
            if (m.getBorrowedBy() != null &&
                m.getBorrowedBy().equals(accountUsername) &&
                m.getFineAmount() > 0) {

                messageLabel.setText("❌ You have unpaid fines! Pay them first.");
                return;
            }
        }

        // 2) لازم يكون فيه عنصر مختار
        Media selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("⚠️ Please select an item to borrow.");
            return;
        }

        // منع استعار نسخة ثانية من نفس الكتاب
        for (Media m : mediaList) {
            if (m.getIsbn().equals(selected.getIsbn()) &&
                accountUsername.equals(m.getBorrowedBy())) {
                messageLabel.setText("❌ You already borrowed a copy of this item.");
                return;
            }
        }

        // 3) هل النسخة المختارة متاحة أصلاً؟
        if (!STATUS_AVAILABLE.equals(selected.getStatus())) {
            messageLabel.setText("❌ This item is not available.");
            return;
        }

        // 4) نكمّل الاستعارة
        selected.borrow(accountUsername);

        saveAllMediaToFile();
        reloadBooks();
        messageLabel.setText("✅ Borrowed successfully! Due date: " + selected.getDueDate());
    }

    /**
     * Handles fine payment.
     * Validates payment amount and updates the fine status.
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
     * Ensures no fines are pending before returning.
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
     * Reloads data from the file.
     */
    @FXML
    void handleReload() {
        reloadBooks();
        infoLabel.setText("🔄 Data reloaded.");
    }

    /**
     * Loads media data from 'books.txt'.
     * (Refactored to reduce cognitive complexity.)
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
        	System.err.println("Error: " + e.getMessage());
        }
        bookTable.refresh();
    }

    /**
     * Parses a single line for the current user context.
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

        Media item = createMediaItem(type, title, author, isbn,
                status, dueDate, fine, borrowedBy,
                amountPaid, copyId);

        applyFineForUserIfOverdue(item, borrowedBy);
        return item;
    }

    /**
     * Creates a Book or CD based on type.
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
     * Safely parses an int with a default value.
     */
    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses a double with a default value.
     */
    private double parseDoubleSafe(String value, double defaultValue) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Safely returns the element at index or empty string.
     */
    private String getPart(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    /**
     * Normalizes the borrowedBy field (ignores "0.0").
     */
    private String normalizeBorrowedBy(String rawBorrowedBy) {
        String trimmed = rawBorrowedBy == null ? "" : rawBorrowedBy.trim();
        return "0.0".equals(trimmed) ? "" : trimmed;
    }

    /**
     * Saves all media data to 'books.txt'.
     */
    void saveAllMediaToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Media m : mediaList) {
                writer.write(m.toFileFormat());
                writer.newLine();
            }
        } catch (IOException e) {
        	System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Reloads books and checks for overdue items.
     */
    void reloadBooks() {
        loadMediaFromFile();
        bookTable.refresh();
        checkOverdueAndNotify();
    }

    /**
     * Checks for overdue items belonging to the current user and sends notifications.
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
