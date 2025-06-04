import java.sql.*;
import java.util.Scanner;

public class LibraryManagementSystem{
    private static final String url = "jdbc:mysql://127.0.0.1:3306/library_db";
    private static final String username = "root";
    private static final String password = "Python@5060";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, username, password);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\n--- ADVANCED LIBRARY MANAGEMENT SYSTEM ---");
                System.out.println("1. Add Book");
                System.out.println("2. View All Books");
                System.out.println("3. Add Member");
                System.out.println("4. View All Members");
                System.out.println("5. Borrow Book");
                System.out.println("6. Return Book");
                System.out.println("7. View Loans");
                System.out.println("8. View Overdue Loans");
                System.out.println("0. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                switch (choice) {
                    case 1 -> addBook(connection, scanner);
                    case 2 -> viewBooks(connection);
                    case 3 -> addMember(connection, scanner);
                    case 4 -> viewMembers(connection);
                    case 5 -> borrowBook(connection, scanner);
                    case 6 -> returnBook(connection, scanner);
                    case 7 -> viewLoans(connection);
                    case 8 -> viewOverdueLoans(connection);
                    case 0 -> {
                        System.out.println("Exiting System...");
                        scanner.close();
                        connection.close();
                        return;
                    }
                    default -> System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addBook(Connection connection, Scanner scanner) throws SQLException {
        scanner.nextLine();  // Clear buffer
        System.out.print("Enter book title: ");
        String title = scanner.nextLine();
        System.out.print("Enter author name: ");
        String author = scanner.nextLine();
        System.out.print("Enter published year: ");
        int year = scanner.nextInt();
        scanner.nextLine(); // Clear buffer
        System.out.print("Enter category: ");
        String category = scanner.nextLine();
        System.out.print("Enter number of copies: ");
        int copies = scanner.nextInt();
        scanner.nextLine(); // Clear buffer
        System.out.print("Enter shelf location: ");
        String shelf = scanner.nextLine();

        String sql = "INSERT INTO books (title, author, year_published, category, copies_total, copies_available, shelf_location) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, year);
            ps.setString(4, category);
            ps.setInt(5, copies);
            ps.setInt(6, copies);  // Initially, copies_available = total copies
            ps.setString(7, shelf);
            ps.executeUpdate();
            System.out.println("Book added successfully!");
        }
    }

    private static void viewBooks(Connection connection) throws SQLException {
        String sql = "SELECT * FROM books";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nBook List:");
            System.out.printf("%-5s %-30s %-20s %-6s %-15s %-6s %-6s %-10s\n",
                    "ID", "Title", "Author", "Year", "Category", "Total", "Avail", "Shelf");
            System.out.println("---------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-6d %-15s %-6d %-6d %-10s\n",
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("year_published"),
                        rs.getString("category"),
                        rs.getInt("copies_total"),
                        rs.getInt("copies_available"),
                        rs.getString("shelf_location"));
            }
        }
    }

    private static void addMember(Connection connection, Scanner scanner) throws SQLException {
        scanner.nextLine(); // clear buffer
        System.out.print("Enter first name: ");
        String firstName = scanner.nextLine();
        System.out.print("Enter last name: ");
        String lastName = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter phone: ");
        String phone = scanner.nextLine();

        String sql = "INSERT INTO members (first_name, last_name, email, phone, membership_date) VALUES (?, ?, ?, ?, CURDATE())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.executeUpdate();
            System.out.println("Member added successfully!");
        }
    }

    private static void viewMembers(Connection connection) throws SQLException {
        String sql = "SELECT * FROM members";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nMembers List:");
            System.out.printf("%-5s %-15s %-15s %-25s %-15s %-12s\n", "ID", "First Name", "Last Name", "Email", "Phone", "Joined Date");
            System.out.println("----------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-5d %-15s %-15s %-25s %-15s %-12s\n",
                        rs.getInt("member_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getDate("membership_date").toString());
            }
        }
    }

    private static void borrowBook(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter member ID: ");
        int memberId = scanner.nextInt();
        System.out.print("Enter book ID: ");
        int bookId = scanner.nextInt();


        String checkSql = "SELECT copies_available FROM books WHERE book_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int available = rs.getInt("copies_available");
                if (available <= 0) {
                    System.out.println("No copies available for this book.");
                    return;
                }
            } else {
                System.out.println("Book ID not found.");
                return;
            }
        }


        String loanSql = "INSERT INTO loans (book_id, member_id, loan_date, due_date, status) VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'Borrowed')";
        try (PreparedStatement ps = connection.prepareStatement(loanSql)) {
            ps.setInt(1, bookId);
            ps.setInt(2, memberId);
            int inserted = ps.executeUpdate();
            if (inserted > 0) {
                // Reduce available copies by 1
                String updateCopies = "UPDATE books SET copies_available = copies_available - 1 WHERE book_id = ?";
                try (PreparedStatement ps2 = connection.prepareStatement(updateCopies)) {
                    ps2.setInt(1, bookId);
                    ps2.executeUpdate();
                }
                System.out.println("Book borrowed successfully! Due in 14 days.");
            }
        }
    }

    private static void returnBook(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter loan ID: ");
        int loanId = scanner.nextInt();


        String checkSql = "SELECT book_id, status FROM loans WHERE loan_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setInt(1, loanId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                int bookId = rs.getInt("book_id");
                if (!status.equalsIgnoreCase("Borrowed")) {
                    System.out.println("This loan is not currently active or already returned.");
                    return;
                }


                String updateLoan = "UPDATE loans SET return_date = CURDATE(), status = 'Returned' WHERE loan_id = ?";
                try (PreparedStatement ps2 = connection.prepareStatement(updateLoan)) {
                    ps2.setInt(1, loanId);
                    int updated = ps2.executeUpdate();
                    if (updated > 0) {
                        // Increase available copies by 1
                        String updateCopies = "UPDATE books SET copies_available = copies_available + 1 WHERE book_id = ?";
                        try (PreparedStatement ps3 = connection.prepareStatement(updateCopies)) {
                            ps3.setInt(1, bookId);
                            ps3.executeUpdate();
                        }
                        System.out.println("Book returned successfully!");
                    }
                }
            } else {
                System.out.println("Loan ID not found.");
            }
        }
    }

    private static void viewLoans(Connection connection) throws SQLException {
        String sql = "SELECT l.loan_id, m.first_name, m.last_name, b.title, l.loan_date, l.due_date, l.return_date, l.status " +
                "FROM loans l " +
                "JOIN members m ON l.member_id = m.member_id " +
                "JOIN books b ON l.book_id = b.book_id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nLoans:");
            System.out.printf("%-5s %-20s %-30s %-12s %-12s %-12s %-10s\n",
                    "ID", "Member Name", "Book Title", "Loan Date", "Due Date", "Return Date", "Status");
            System.out.println("----------------------------------------------------------------------------------------");
            while (rs.next()) {
                String memberName = rs.getString("first_name") + " " + rs.getString("last_name");
                String returnDate = rs.getDate("return_date") != null ? rs.getDate("return_date").toString() : "-";
                System.out.printf("%-5d %-20s %-30s %-12s %-12s %-12s %-10s\n",
                        rs.getInt("loan_id"),
                        memberName,
                        rs.getString("title"),
                        rs.getDate("loan_date").toString(),
                        rs.getDate("due_date").toString(),
                        returnDate,
                        rs.getString("status"));
            }
        }
    }

    private static void viewOverdueLoans(Connection connection) throws SQLException {
        String sql = "SELECT l.loan_id, m.first_name, m.last_name, b.title, l.loan_date, l.due_date " +
                "FROM loans l " +
                "JOIN members m ON l.member_id = m.member_id " +
                "JOIN books b ON l.book_id = b.book_id " +
                "WHERE l.status = 'Borrowed' AND l.due_date < CURDATE()";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\nOverdue Loans:");
            System.out.printf("%-5s %-20s %-30s %-12s %-12s\n", "ID", "Member Name", "Book Title", "Loan Date", "Due Date");
            System.out.println("--------------------------------------------------------------------------");
            while (rs.next()) {
                String memberName = rs.getString("first_name") + " " + rs.getString("last_name");
                System.out.printf("%-5d %-20s %-30s %-12s %-12s\n",
                        rs.getInt("loan_id"),
                        memberName,
                        rs.getString("title"),
                        rs.getDate("loan_date").toString(),
                        rs.getDate("due_date").toString());
            }
        }
    }
}
