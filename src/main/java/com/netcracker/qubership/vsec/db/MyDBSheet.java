package com.netcracker.qubership.vsec.db;

import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper_models.SheetData;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper_models.SheetRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDBSheet {
    private static final Logger log = LoggerFactory.getLogger(MyDBSheet.class);

    private static final String CREATE_TABLE_SQL = """
             CREATE TABLE IF NOT EXISTS my_db_sheet (
                   id INT AUTO_INCREMENT PRIMARY KEY,
                   created_when VARCHAR(50) NOT NULL,
                   reporter_email VARCHAR(255) NOT NULL,
                   reporter_name VARCHAR(100),
                   report_date VARCHAR(50) NOT NULL,
                   msg_done VARCHAR,
                   msg_plans VARCHAR
               )
            """;
    private static final String INSERT_SQL = """
            INSERT INTO my_db_sheet (
                created_when,\s
                reporter_email,\s
                reporter_name,\s
                report_date,\s
                msg_done,\s
                msg_plans
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String CLEAN_UP_OBSOLETE_SQL = """
            DELETE FROM my_db_sheet\s
            WHERE id NOT IN (
                SELECT id FROM (
                    SELECT id,
                           reporter_email,
                           report_date,
                           created_when,
                           ROW_NUMBER() OVER (
                               PARTITION BY reporter_email, report_date\s
                               ORDER BY created_when DESC
                           ) as rn
                    FROM my_db_sheet
                ) ranked
                WHERE rn = 1
            )
            """;


    private final Connection conn;

    public MyDBSheet(Connection conn) {
        this.conn = conn;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException sqlEx) {
            log.error("Error while executing DDL to create table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }
    }

    public int appendData(SheetData sheetData) {
        int[] updates;
        try (PreparedStatement pstm = conn.prepareStatement(INSERT_SQL)) {
            for (SheetRow row : sheetData.getValues()) {
                pstm.setString(1, row.getTimestamp().toString());
                pstm.setString(2, row.getEmail());
                pstm.setString(3, ""); // reporter_name
                pstm.setString(4, row.getWeekStartDateAsLocalDate().toString()); // report_date
                pstm.setString(5, row.getCompletedWork()); // msg_done
                pstm.setString(6, row.getNextWeekPlans()); // msg_plans
                pstm.addBatch();
            }

            updates = pstm.executeBatch();
        } catch (SQLException sqlEx) {
            log.error("Error while executing DML to save data into table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }

        // now do clean up of the obsolete records (i.e. which were overriden by reporter with new version of the report)
        try (PreparedStatement pstm = conn.prepareStatement(CLEAN_UP_OBSOLETE_SQL)) {
            int numOfCleanedUpRows = pstm.executeUpdate();
            log.info("{} of obsolete records where cleaned up", numOfCleanedUpRows);
        } catch (SQLException sqlEx) {
            log.error("Error while executing clean-up procedure", sqlEx);
        }

        return updates.length;
    }
}
