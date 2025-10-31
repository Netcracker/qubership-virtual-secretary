package com.netcracker.qubership.vsec;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.netcracker.qubership.vsec.jobs.AllJobsRegistry;
import com.netcracker.qubership.vsec.mattermost.MattermostClientFactory;
import com.netcracker.qubership.vsec.model.AppProperties;
import net.bis5.mattermost.client4.MattermostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VirtualSecretaryApp {
    private static final Logger log = LoggerFactory.getLogger(VirtualSecretaryApp.class);

    public static void main(String[] args) {
        // Check if program arguments are specified
        if (args.length < 1) {
            System.out.println("Incorrect program arguments");
            System.out.println("Usage example: VirtualSecretaryApp $PATH_TO_PROPERTIES_FILE$");
            System.exit(1);
        }

        // Adding ability to do test runs of the application in the remove environment (Github VMs)
        if ("--fake-run".equals(args[0])) {
            fakeRun(args);
            System.exit(0);
        }

        // Check if argument goes to properties files
        final String propsFileName = args[0];
        AppProperties appProps = null;
        try {
            JavaPropsMapper mapper = new JavaPropsMapper();
            appProps = mapper.readValue(new File(propsFileName), AppProperties.class);
        } catch (IOException ex) {
            log.error("Error while loading application properties from {}", propsFileName, ex);
            System.exit(1);
        }

        if (appProps == null) {
            System.out.printf("Can't load properties from the file %s\n", propsFileName);
            System.out.println("Terminating application");
            System.exit(1);
        }

        AllJobsRegistry allJobsRegistry = new AllJobsRegistry();


        // Open connection to Mattermost server
        MattermostClient client = MattermostClientFactory.openNewClient(appProps.getMmHost(), appProps.getMmToken(), allJobsRegistry.getRegisteredReflectiveJobs());

        // Run all jobs
        allJobsRegistry.runAllActiveJobs(appProps, client);
    }


    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "FileEncryptionKey456 MyStrongPassword123"; // requires exact in this format
    // file_password user_password

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS timestamp_data (
                id INT AUTO_INCREMENT PRIMARY KEY,
                event_name VARCHAR(100) NOT NULL,
                timestamp_value VARCHAR(19) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String INSERT_SQL = """
        INSERT INTO timestamp_data (event_name, timestamp_value) 
        VALUES (?, ?)
        """;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void fakeRun(String[] args) {
        log.info("This is a fake run. Timestamp = " + System.currentTimeMillis());

        final String propsFileName = args[1];
        AppProperties appProps = null;
        try {
            JavaPropsMapper mapper = new JavaPropsMapper();
            appProps = mapper.readValue(new File(propsFileName), AppProperties.class);
        } catch (IOException ex) {
            log.error("Error while loading application properties from {}", propsFileName, ex);
            System.exit(1);
        }

        try {
            // Load H2 JDBC driver
            Class.forName(DB_DRIVER);

            // Create connection with file password for encryption
            String connectionUrl = "jdbc:h2:" + appProps.getDbFileName() + ";CIPHER=AES;";

            String passString = appProps.getDbFileEncryptionPassword() + " " + appProps.getDbUserPassword();

            try (Connection conn = DriverManager.getConnection(connectionUrl, appProps.getDbUserName(), passString);
                 Statement stmt = conn.createStatement()) {

                // Create table
                stmt.execute(CREATE_TABLE_SQL);
                System.out.println("Database initialized successfully!");

                // Get current timestamp in required format
                String currentTimestamp = LocalDateTime.now().format(FORMATTER);

                try (PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
                    // Set parameters
                    pstmt.setString(1, "test-event");
                    pstmt.setString(2, currentTimestamp);

                    // Execute insert
                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected > 0) {
                        System.out.println("Stored timestamp for event: " + "test-event" + " - " + currentTimestamp);
                    }
                }

                retrieveTimestamps(conn);
            }

        } catch (ClassNotFoundException e) {
            System.err.println("H2 JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    private static final String SELECT_SQL = """
        SELECT id, event_name, timestamp_value, created_at 
        FROM timestamp_data 
        ORDER BY created_at DESC
        """;

    public static void retrieveTimestamps(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_SQL)) {

            System.out.println("\n=== Stored Timestamps ===");
            System.out.printf("%-5s %-15s %-20s %-20s%n",
                    "ID", "Event", "Timestamp", "Created At");
            System.out.println("------------------------------------------------------------");

            while (rs.next()) {
                int id = rs.getInt("id");
                String eventName = rs.getString("event_name");
                String timestampValue = rs.getString("timestamp_value");
                Timestamp createdAt = rs.getTimestamp("created_at");

                System.out.printf("%-5d %-15s %-20s %-20s%n",
                        id, eventName, timestampValue, createdAt);
            }

        } catch (SQLException e) {
            System.err.println("Failed to retrieve timestamps: " + e.getMessage());
        }
    }

}
