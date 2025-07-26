import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Database Connection Class
class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/lab_booking_system"; // Your DB URL
    private static final String USER = "root"; // Your MySQL username
    private static final String PASSWORD = "Sid@2004"; // Your MySQL password

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

// Booking Model Class
class Booking {
    String staffId, staffName, startTime, endTime, labName, branch, date;
    int numStudents;

    public Booking(String staffId, String staffName, String date, String startTime, String endTime, String labName, int numStudents, String branch) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.labName = labName;
        this.numStudents = numStudents;
        this.branch = branch;
    }

    public Object[] toObjectArray() {
        return new Object[]{staffId, staffName, date, startTime, endTime, labName, numStudents, branch};
    }
}

// DAO Class for Booking Operations
class BookingDAO {
    // Add a new booking
    public boolean addBooking(Booking booking) throws SQLException {
        String query = "INSERT INTO bookings (staff_id, staff_name, date, start_time, end_time, lab_name, num_students, branch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, booking.staffId);
            stmt.setString(2, booking.staffName);
            stmt.setString(3, booking.date);
            stmt.setTime(4, Time.valueOf(booking.startTime + ":00"));
            stmt.setTime(5, Time.valueOf(booking.endTime + ":00"));
            stmt.setString(6, booking.labName);
            stmt.setInt(7, booking.numStudents);
            stmt.setString(8, booking.branch);
            return stmt.executeUpdate() > 0;
        }
    }

    // Delete a booking by staff ID
    public boolean deleteBookingByStaffId(String staffId) throws SQLException {
        String query = "DELETE FROM bookings WHERE staff_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, staffId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Retrieve bookings by time interval and date
    public List<Booking> getBookingsByTimeIntervalAndDate(String date, String startTime, String endTime) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        String query = "SELECT * FROM bookings WHERE date = ? AND start_time >= ? AND end_time <= ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, date);
            stmt.setTime(2, Time.valueOf(startTime + ":00"));
            stmt.setTime(3, Time.valueOf(endTime + ":00"));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking booking = new Booking(
                            rs.getString("staff_id"),
                            rs.getString("staff_name"),
                            rs.getString("date"),
                            rs.getString("start_time").substring(0, 5),
                            rs.getString("end_time").substring(0, 5),
                            rs.getString("lab_name"),
                            rs.getInt("num_students"),
                            rs.getString("branch")
                    );
                    bookings.add(booking);
                }
            }
        }
        return bookings;
    }
}

// Main Class for Lab Booking System
public class LabBookingSystem extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private BookingDAO bookingDAO;

    public LabBookingSystem() {
        bookingDAO = new BookingDAO();
        setTitle("Lab Booking System");
        setSize(800, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Table for displaying bookings
        String[] columnNames = {"Staff ID", "Staff Name", "Date", "Start Time", "End Time", "Lab Name", "No. of Students", "Branch"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Form input fields
        JPanel formPanel = new JPanel(new GridLayout(8, 2, 10, 10));
        JTextField staffIdField = new JTextField();
        JTextField staffNameField = new JTextField();
        JTextField dateField = new JTextField();  // New date field
        JTextField startTimeField = new JTextField();
        JTextField endTimeField = new JTextField();
        JTextField labNameField = new JTextField();
        JTextField numStudentsField = new JTextField();
        JTextField branchField = new JTextField();

        formPanel.add(new JLabel("Staff ID:"));
        formPanel.add(staffIdField);
        formPanel.add(new JLabel("Staff Name:"));
        formPanel.add(staffNameField);
        formPanel.add(new JLabel("Date (YYYY-MM-DD):"));  // New date label
        formPanel.add(dateField);  // New date field in form
        formPanel.add(new JLabel("Start Time (HH:MM):"));
        formPanel.add(startTimeField);
        formPanel.add(new JLabel("End Time (HH:MM):"));
        formPanel.add(endTimeField);
        formPanel.add(new JLabel("Lab Name:"));
        formPanel.add(labNameField);
        formPanel.add(new JLabel("Number of Students:"));
        formPanel.add(numStudentsField);
        formPanel.add(new JLabel("Branch:"));
        formPanel.add(branchField);

        // Book Lab button
        JButton bookButton = new JButton("Book Lab");
        bookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String staffId = staffIdField.getText();
                String staffName = staffNameField.getText();
                String date = dateField.getText();  // Get date input
                String startTime = startTimeField.getText();
                String endTime = endTimeField.getText();
                String labName = labNameField.getText();
                String branch = branchField.getText();
                int numStudents;

                // Input validation
                if (staffId.isEmpty() || staffName.isEmpty() || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || labName.isEmpty() || branch.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "All fields must be filled!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    numStudents = Integer.parseInt(numStudentsField.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Invalid number of students!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Add booking to database
                Booking booking = new Booking(staffId, staffName, date, startTime, endTime, labName, numStudents, branch);
                try {
                    boolean success = bookingDAO.addBooking(booking);
                    if (success) {
                        tableModel.addRow(booking.toObjectArray());
                        refreshTable(); // Refresh the table
                        // Clear fields
                        staffIdField.setText("");
                        staffNameField.setText("");
                        dateField.setText("");  // Clear date field
                        startTimeField.setText("");
                        endTimeField.setText("");
                        labNameField.setText("");
                        numStudentsField.setText("");
                        branchField.setText("");
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to book lab!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Delete Lab booking button
        JButton deleteButton = new JButton("Delete Booking by Staff ID");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String staffId = JOptionPane.showInputDialog("Enter staff ID of booking to delete:");

                if (staffId == null || staffId.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Staff ID must be provided", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    boolean success = bookingDAO.deleteBookingByStaffId(staffId);
                    if (success) {
                        JOptionPane.showMessageDialog(null, "Booking deleted successfully!");
                        refreshTable(); // Refresh the table to show updated bookings
                    } else {
                        JOptionPane.showMessageDialog(null, "Booking not found!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Retrieve Lab bookings by time interval and date button
        JButton retrieveButton = new JButton("Retrieve Bookings by Time Interval");
        retrieveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String date = JOptionPane.showInputDialog("Enter date (YYYY-MM-DD):");
                String startTime = JOptionPane.showInputDialog("Enter start time (HH:MM):");
                String endTime = JOptionPane.showInputDialog("Enter end time (HH:MM):");

                if (date == null || startTime == null || endTime == null || date.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Date, start time, and end time must be provided", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    List<Booking> bookings = bookingDAO.getBookingsByTimeIntervalAndDate(date, startTime, endTime);
                    tableModel.setRowCount(0); // Clear existing table data
                    for (Booking booking : bookings) {
                        tableModel.addRow(booking.toObjectArray());
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(bookButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(retrieveButton);

        // Add components to frame
        add(scrollPane, BorderLayout.CENTER);
        add(formPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Refresh the table to show the latest bookings
    private void refreshTable() {
        tableModel.setRowCount(0);
        // Fetch bookings from the database and add them to the tableModel
        // Not shown here for brevity, but you'd fetch all bookings and call tableModel.addRow(...)
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LabBookingSystem().setVisible(true);
            }
        });
    }
}
