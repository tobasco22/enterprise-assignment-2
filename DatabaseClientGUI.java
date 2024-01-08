 package databaseclientgui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;
import java.sql.PreparedStatement;



public class DatabaseClientGUI extends JFrame {
    private Connection connection;
    private Connection operationsLogConnection; // Connection to the operationslog database
    private JComboBox<String> databaseComboBox;
    private JComboBox<String> userPropertiesComboBox;
    private JTextField usernameTextField;
    private JPasswordField passwordField; // Use JPasswordField to hide the password
    private JTextArea commandArea;
    private JTextArea resultArea;
    private JTextField connectionStatusTextField; // Add connection status text field
    private static final String operationslogJdbcUrl = "jdbc:mysql://localhost:3306/operationslog"; 
    private static final String operationslogUsername = "project3app"; // Replace with the actual username
    private static final String operationslogPassword = "project3app"; // Replace with the actual password



    public DatabaseClientGUI() {
        super("Database Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create components
        databaseComboBox = new JComboBox<>(new String[]{"bikedb.properties", "project3.properties", "database3.properties"});
        userPropertiesComboBox = new JComboBox<>(new String[]{"root.properties", "client1.properties", "client2.properties"});

        
        // Set text fields for username and password to the same size as the drop-down menus
        Dimension textFieldSize = databaseComboBox.getPreferredSize();
        usernameTextField = new JTextField(10);
        usernameTextField.setPreferredSize(textFieldSize);
        passwordField = new JPasswordField(10);
        passwordField.setPreferredSize(textFieldSize);

        commandArea = new JTextArea(5, 40);
        resultArea = new JTextArea(15, 40);

        connectionStatusTextField = new JTextField("NO CONNECTION NOW"); // Initialize with the default message
        connectionStatusTextField.setEditable(false); // Make it read-only

        // Create buttons
        JButton connectButton = new JButton("Connect");
        JButton clearCommandButton = new JButton("Clear Command");
        JButton executeCommandButton = new JButton("Execute Command");
        JButton clearResultButton = new JButton("Clear Result");

        // Make the "User Properties" ComboBox the same size as the "Select Database" ComboBox
        userPropertiesComboBox.setPreferredSize(databaseComboBox.getPreferredSize());

        // Create left panel with GridBagLayout
        JPanel leftPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // First row (Select Database)
        gbc.gridx = 0;
        gbc.gridy = 0;
        leftPanel.add(new JLabel("Select Database:"), gbc);

        gbc.gridx = 1;
        leftPanel.add(databaseComboBox, gbc);

        // Second row (User Properties)
        gbc.gridx = 0;
        gbc.gridy = 1;
        leftPanel.add(new JLabel("User Properties:"), gbc);

        gbc.gridx = 1;
        leftPanel.add(userPropertiesComboBox, gbc);

        // Third row (Username)
        gbc.gridx = 0;
        gbc.gridy = 2;
        leftPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        leftPanel.add(usernameTextField, gbc);

        // Fourth row (Password)
        gbc.gridx = 0;
        gbc.gridy = 3;
        leftPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        leftPanel.add(passwordField, gbc);

        // Fifth row (Connect Button)
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        leftPanel.add(connectButton, gbc);

        // Create right panel for SQL command with GridBagLayout
        JPanel rightPanel = new JPanel(new GridBagLayout());

        // Create a separate GridBagConstraints for the right panel
        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.anchor = GridBagConstraints.WEST;
        rightGbc.insets = new Insets(5, 5, 5, 5);

        // First row (Enter SQL Command)
        rightGbc.gridx = 0;
        rightGbc.gridy = 0;
        rightPanel.add(new JLabel("Enter SQL Command:"), rightGbc);

        // Second row (SQL Command Text Area)
        rightGbc.gridx = 0;
        rightGbc.gridy = 1;
        rightGbc.gridwidth = 2;
        rightPanel.add(new JScrollPane(commandArea), rightGbc);

        // Third row (Clear Command and Execute Command buttons)
        rightGbc.gridx = 0;
        rightGbc.gridy = 2;
        rightGbc.gridwidth = 1;
        rightPanel.add(clearCommandButton, rightGbc);

        // Increase the gap between Clear Command and Execute Command buttons
        rightGbc.gridx = 1;
        rightPanel.add(executeCommandButton, rightGbc);

        // Make the "Connect" button slightly bigger
        connectButton.setPreferredSize(new Dimension(120, connectButton.getPreferredSize().height));

        // Create bottom panel for SQL result and connection status
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.add(new JLabel("SQL Execution Result:"));
        resultPanel.add(new JScrollPane(resultArea));
        resultPanel.add(clearResultButton);

        // Add the connection status text field
        bottomPanel.add(resultPanel, BorderLayout.NORTH);
        bottomPanel.add(connectionStatusTextField, BorderLayout.CENTER);

        // Add left, right, and bottom panels to the frame
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Add ActionListener for "Execute Command" button
        executeCommandButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String sqlCommand = commandArea.getText();

                if (connection != null) {
                    executeSQLCommand(sqlCommand);
                } else {
                    // Handle the case where there's no valid database connection.
                    connectionStatusTextField.setText("No valid database connection.");
                }
            }
        });

        // Create the "Connect" button ActionListener
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Close the existing connection (if any)
                if (connection != null) {
                    try {
                        connection.close();
                        connection = null; // Set the connection to null
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                String databasePropertiesFile = (String) databaseComboBox.getSelectedItem();
                String userPropertiesFile = (String) userPropertiesComboBox.getSelectedItem();
                String enteredUsername = usernameTextField.getText();
                char[] enteredPassword = passwordField.getPassword(); // Get the password as a char array

                try {
                    String jdbcUrl;
                    String expectedUsername = "";
                    String expectedPassword = "";

                    // Check if "bikedb.properties" or "project3.properties" is selected
                    if (databasePropertiesFile.equals("bikedb.properties") || databasePropertiesFile.equals("project3.properties")) {
                        jdbcUrl = "jdbc:mysql://localhost:3306/" + databasePropertiesFile.substring(0, databasePropertiesFile.indexOf('.'));
                        // Set the expected username and password based on the selected user
                        if (userPropertiesFile.equals("root.properties")) {
                            expectedUsername = "root";
                            expectedPassword = "pass";
                        } else if (userPropertiesFile.equals("client1.properties")) {
                            expectedUsername = "client1";
                            expectedPassword = "client1";
                        } else if (userPropertiesFile.equals("client2.properties")) {
                            expectedUsername = "client2";
                            expectedPassword = "client2";
                        }
                        // Add more conditions for other users if needed
                    } else {
                        Properties databaseProps = readPropertiesFromFile(databasePropertiesFile);

                        String hostname = databaseProps.getProperty("hostname");
                        int port = Integer.parseInt(databaseProps.getProperty("port"));
                        String databaseName = databaseProps.getProperty("databaseName");

                        jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + databaseName;
                    }

                    // Check if the entered username and password match the expected values
                    if (enteredUsername.equals(expectedUsername) && String.valueOf(enteredPassword).equals(expectedPassword)) {
                        // Set the class-level 'connection' variable
                        connection = DriverManager.getConnection(jdbcUrl, enteredUsername, String.valueOf(enteredPassword));
                        if (connection != null) {
                            connectionStatusTextField.setText("Connected to " + databasePropertiesFile + " as " + enteredUsername);
                            // You can store the 'connection' object for later use in SQL command execution.
                        } else {
                            connectionStatusTextField.setText("Connection failed");
                        }
                    } else {
                        connectionStatusTextField.setText("Invalid username or password. Connection failed.");
                    }
                } catch (IOException | SQLException ex) {
                    ex.printStackTrace();
                    connectionStatusTextField.setText("Connection failed: " + ex.getMessage());
                } finally {
                    // Clear the entered password for security
                    Arrays.fill(enteredPassword, '0');
                }
            }
        });

        // Add ActionListener for "Clear Command" button
        clearCommandButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commandArea.setText(""); // Clear the SQL command area by setting its text to an empty string
            }
        });

        // Add ActionListener for "Clear Result" button
        clearResultButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resultArea.setText(""); // Clear the text
                resultArea.removeAll(); // Remove all components, including the JTable
                resultArea.revalidate(); // Revalidate the container to reflect the changes
                resultArea.repaint(); // Repaint the container
            }
        });

        // Create a thread to establish the connection to operationslog in the background
        Thread operationsLogThread = new Thread(() -> {
            try (Connection connection = DriverManager.getConnection(operationslogJdbcUrl, operationslogUsername, operationslogPassword)) {
    // Use the connection here
} catch (SQLException ex) {
    ex.printStackTrace();
    connectionStatusTextField.setText("Connection to operationslog failed.");
}
        });

        operationsLogThread.start(); // Start the thread to establish the connection

        

    }

    // Define a method to read properties from a file
    private Properties readPropertiesFromFile(String fileName) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(fileName)) {
            properties.load(fis);
        }
        return properties;
    }

    private void executeSQLCommand(String sqlCommand) {
    try {
        Statement statement = connection.createStatement();

        if (statement.execute(sqlCommand)) {
            ResultSet resultSet = statement.getResultSet();
            displayResultSetAsTable(resultSet);

            // Check if the SQL command is a query
            boolean isQuery = sqlCommand.trim().toLowerCase().startsWith("select");

            // Call the method with the username from the text field
            String enteredUsername = usernameTextField.getText();
            updateOperationsLog(enteredUsername,  isQuery);
        } else {
            int updateCount = statement.getUpdateCount();
            resultArea.setText(updateCount + " rows affected");
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
        resultArea.setText("Error: " + ex.getMessage());
    }
}



    
    private void logSuccessfulOperation() {
        try {
            Statement statement = operationsLogConnection.createStatement();

            // You should determine the current user based on the userPropertiesComboBox selection
            String currentUser = (String) userPropertiesComboBox.getSelectedItem();

            String checkQuery = "SELECT * FROM operationscount WHERE login_username = ?";
            PreparedStatement checkStatement = operationsLogConnection.prepareStatement(checkQuery);
            checkStatement.setString(1, currentUser);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                String updateQuery = "UPDATE operationscount SET num_queries = num_queries + 1, num_updates = num_updates + 1 WHERE login_username = ?";
                PreparedStatement updateStatement = operationsLogConnection.prepareStatement(updateQuery);
                updateStatement.setString(1, currentUser);
                updateStatement.executeUpdate();
            } else {
                String insertQuery = "INSERT INTO operationscount (login_username, num_queries, num_updates) VALUES (?, 1, 1)";
                PreparedStatement insertStatement = operationsLogConnection.prepareStatement(insertQuery);
                insertStatement.setString(1, currentUser);
                insertStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Handle any errors in the logSuccessfulOperation method
        }
    }
    
    private void updateOperationsLog(String username, boolean isQuery) {
    try (Connection connection = DriverManager.getConnection(operationslogJdbcUrl, operationslogUsername, operationslogPassword)) {
        // Check if an entry exists for the current username
        String checkQuery = "SELECT * FROM operationscount WHERE login_username = ?";
        PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
        checkStatement.setString(1, username);
        ResultSet resultSet = checkStatement.executeQuery();

        if (resultSet.next()) {
            // An entry exists, update the counts
            String updateQuery = isQuery
                    ? "UPDATE operationscount SET num_queries = num_queries + 1 WHERE login_username = ?"
                    : "UPDATE operationscount SET num_updates = num_updates + 1 WHERE login_username = ?";
            PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
            updateStatement.setString(1, username);
            updateStatement.executeUpdate();
        } else {
            // No entry found, insert a new one
            String insertQuery = "INSERT INTO operationscount (login_username, num_queries, num_updates) VALUES (?, ?, ?)";
            PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
            insertStatement.setString(1, username);
            if (isQuery) {
                insertStatement.setInt(2, 1);
                insertStatement.setInt(3, 0);
            } else {
                insertStatement.setInt(2, 0);
                insertStatement.setInt(3, 1);
            }
            insertStatement.executeUpdate();
        }
    } catch (SQLException ex) {
        ex.printStackTrace();
        // Handle any errors when updating the operationslog
    }
}










    private void displayResultSetAsTable(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        DefaultTableModel tableModel = new DefaultTableModel();

        // Add column names to the table model
        for (int i = 1; i <= columnCount; i++) {
            tableModel.addColumn(metaData.getColumnName(i));
        }

        // Add data rows to the table model
        while (resultSet.next()) {
            Object[] rowData = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                rowData[i - 1] = resultSet.getString(i);
            }
            tableModel.addRow(rowData);
        }

        JTable table = new JTable(tableModel);

        // Replace the resultArea with a JScrollPane that contains the JTable
        JScrollPane scrollPane = new JScrollPane(table);
        resultArea.setText(""); // Clear the result text area
        resultArea.setLayout(new BorderLayout());
        resultArea.add(scrollPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseClientGUI gui = new DatabaseClientGUI();
            gui.setVisible(true);
        });
    }
}