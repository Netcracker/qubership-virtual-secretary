package com.netcracker.qubership.vsec.db;

import com.netcracker.qubership.vsec.ErrorCodes;
import com.netcracker.qubership.vsec.genai.ReportAnalysis;
import com.netcracker.qubership.vsec.model.googlesheets.SheetData;
import com.netcracker.qubership.vsec.model.googlesheets.SheetRow;
import com.netcracker.qubership.vsec.model.team.QSMember;
import com.netcracker.qubership.vsec.model.team.QSTeam;
import com.netcracker.qubership.vsec.utils.MiscUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                msg_plans VARCHAR,
                genai_content_score INT DEFAULT 0,
                genai_impact_score INT DEFAULT 0,
                genai_proactivity_score INT DEFAULT 0,
                genai_context_score INT DEFAULT 0,
                genai_final_score INT DEFAULT 0,
                genai_analysis_content VARCHAR,
                genai_analysis_impact VARCHAR,
                genai_analysis_proactivity VARCHAR,
                genai_analysis_context VARCHAR,
                genai_analysis_strength VARCHAR,
                genai_analysis_improvements VARCHAR
            )
            """;

    private final Connection conn;

    public MyDBSheet(Connection conn) {
        this.conn = conn;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException sqlEx) {
            log.error(ErrorCodes.ERR009 + ": Error while executing DDL to create table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }
    }

    /**
     * Stores data from the model into database.
     * @param sheetData
     * @return
     */
    public int appendData(SheetData sheetData) {
        final String INSERT_SQL = """
                INSERT INTO my_db_sheet (
                    created_when,\s
                    reporter_email,\s
                    reporter_name,\s
                    report_date,\s
                    msg_done,\s
                    msg_plans
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

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
            log.error(ErrorCodes.ERR010 + ": Error while executing DML to save data into table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }

        return updates.length;
    }


    public void correctMistakesInDB(QSTeam qsTeam) {
        // corrects dates to nearest Mondays:
        // if Sunday is selected then next nearest Monday will be selected
        // if other day of week is selected then previous nearest Monday will be selected

        List<SheetRow> allRows = loadByQuery("SELECT id, report_date FROM my_db_sheet");
        Map<String, String> mistakesMap = new HashMap<>(); // ID -> correct date

        for (var row : allRows) {
            LocalDate date = LocalDate.parse(row.getWeekStartDate());
            if (!date.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
                if (date.getDayOfWeek().equals(DayOfWeek.SUNDAY)) date = date.plusDays(1);
                while (!date.getDayOfWeek().equals(DayOfWeek.MONDAY)) date = date.minusDays(1);
                mistakesMap.put(row.getId(), date.toString());
            }
        }

        if (!mistakesMap.isEmpty()) {
            final String updateQuery = "UPDATE my_db_sheet SET report_date = ? WHERE id = ?";

            try (PreparedStatement pstm = conn.prepareStatement(updateQuery)) {

                for (var me : mistakesMap.entrySet()) {
                    pstm.setString(1, me.getValue());
                    pstm.setInt(2, Integer.parseInt(me.getKey()));
                    pstm.addBatch();

                    log.info("Correcting date in database to new value '{}' for ID = {}", me.getValue(), me.getKey());
                }

                pstm.executeBatch();
            } catch (SQLException sqlEx) {
                log.error("Error while executing DML to save data into table", sqlEx);
                throw new IllegalStateException(sqlEx);
            }

            log.info("{} number of incorrect dates were corrected", mistakesMap.size());
        }

        // if alternative emails were used - then set main ones into DB
        final String updateEmailQuery = "UPDATE my_db_sheet SET reporter_email = ? WHERE reporter_email = ?";
        try (PreparedStatement pstm = conn.prepareStatement(updateEmailQuery)) {
            for (QSMember member : qsTeam.getMembers()) {
                String altEmail = member.getAltEmail();
                if (MiscUtils.isEmpty(altEmail)) continue;

                pstm.setString(1, member.getEmail());
                pstm.setString(2, altEmail);
                pstm.addBatch();

                log.info("Changing submitted alternative email from {} to base {} if will be found in database", altEmail, member.getEmail());
            }

            pstm.executeUpdate(); // todo: fix case - when no alt emails was set - not to execute stmt
        } catch (SQLException sqlEx) {
            log.error("Error while executing DML to save data into table", sqlEx);
            throw new IllegalStateException(sqlEx);
        }

        // now do clean up of the obsolete records (i.e. which were overridden by reporter with new version of the report)
        final String CLEAN_UP_OBSOLETE_SQL = """
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

        try (PreparedStatement pstm = conn.prepareStatement(CLEAN_UP_OBSOLETE_SQL)) {
            int numOfCleanedUpRows = pstm.executeUpdate();
            log.info("{} of obsolete records where cleaned up", numOfCleanedUpRows);
        } catch (SQLException sqlEx) {
            log.error(ErrorCodes.ERR011 + ": Error while executing clean-up procedure", sqlEx);
        }
    }

    public List<SheetRow> loadByReportDate(LocalDate dateOfWeekBegining) {
        final String LOAD_REPORTS_BY_DATE = "SELECT * FROM my_db_sheet WHERE report_date = ?";
        return loadByQuery(LOAD_REPORTS_BY_DATE, dateOfWeekBegining.toString());
    }

    /**
     * Collects pairs of email -> date which are missed in the database, which in turns means that the report was not submitted by a person
     *
     * @param teamEmails  List of email of the team to collect data for
     * @param dateFromInc date to proceed database from (including)
     * @param dateTillInc date to proceed database to (including)
     */
    public Map<String, List<LocalDate>> findMissedReportRecords(List<String> teamEmails, LocalDate dateFromInc, LocalDate dateTillInc) {
        Map<String, List<LocalDate>> result = new HashMap<>();

        LocalDate curDate = dateFromInc;
        while (curDate.isBefore(dateTillInc) || curDate.equals(dateTillInc)) {
            List<String> emailPerDate = new ArrayList<>(teamEmails);
            List<SheetRow> reportsPerDate = loadByReportDate(curDate);

            for (SheetRow row : reportsPerDate) {
                emailPerDate.remove(row.getEmail());
            }

            // here we have unreported emails per curDate date
            for (String email : emailPerDate) {
                List<LocalDate> dates = result.computeIfAbsent(email, k -> new ArrayList<>());
                dates.add(curDate);
            }

            curDate = curDate.plusWeeks(1);
        }

        return result;
    }

    public List<SheetRow> getReportsWithNoQualityScore() {
        final String SELECT_NON_SCORED_REPORTS_SQL = "SELECT * FROM my_db_sheet WHERE genai_final_score = 0 ORDER BY id";
        return loadByQuery(SELECT_NON_SCORED_REPORTS_SQL);
    }


    private static final String SAVE_ANALYSIS_SQL = """
            UPDATE my_db_sheet\s
              SET\s
                  genai_content_score = ?,
                  genai_impact_score = ?,
                  genai_proactivity_score = ?,
                  genai_context_score = ?,
                  genai_final_score = ?,
                  genai_analysis_content = ?,
                  genai_analysis_impact = ?,
                  genai_analysis_proactivity = ?,
                  genai_analysis_context = ?,
                  genai_analysis_strength = ?,
                  genai_analysis_improvements = ?
              WHERE reporter_email = ? AND report_date = ?
            """;

    public void saveAnalysisIntoDB(SheetRow forRow, ReportAnalysis analysisResult) {
        try (PreparedStatement pstm = conn.prepareStatement(SAVE_ANALYSIS_SQL)) {
            pstm.setInt(1, analysisResult.getScores().getContentScore());
            pstm.setInt(2, analysisResult.getScores().getImpactScore());
            pstm.setInt(3, analysisResult.getScores().getProactivityScore());
            pstm.setInt(4, analysisResult.getScores().getContextScore());
            pstm.setDouble(5, analysisResult.getScores().getFinalScore());

            pstm.setString(6, analysisResult.getAnalysis().getContentJustification());
            pstm.setString(7, analysisResult.getAnalysis().getImpactJustification());
            pstm.setString(8, analysisResult.getAnalysis().getProactivityJustification());
            pstm.setString(9, analysisResult.getAnalysis().getContextJustification());

            pstm.setString(10, analysisResult.getRecommendations().getStrengthOneString());
            pstm.setString(11, analysisResult.getRecommendations().getImprovementsOneString());

            pstm.setString(12, forRow.getEmail());
            pstm.setString(13, forRow.getWeekStartDate());

            int rowsUpdated = pstm.executeUpdate();
            if (rowsUpdated != 1)
                throw new SQLException("Unexpected number of updated row. Exp value = 1, act value = " + rowsUpdated + ", email = " + forRow.getEmail() + ", date = " + forRow.getWeekStartDate());
        } catch (SQLException sqlEx) {
            log.error("Error while loading data by report date", sqlEx);
            throw new IllegalStateException(sqlEx);
        }
    }

    public List<SheetRow> getReportsWithQualityScore() {
        final String SELECT_REPORTS_WITH_FINAL_SCORE_SQL = "SELECT * FROM my_db_sheet WHERE genai_final_score > 0 ORDER BY report_date";
        return loadByQuery(SELECT_REPORTS_WITH_FINAL_SCORE_SQL);
    }

    public List<SheetRow> loadByQuery(String selectQuery, Object... bindParameters) {
        List<SheetRow> result = new ArrayList<>();

        try (PreparedStatement pstm = conn.prepareStatement(selectQuery)) {
            if (bindParameters != null) {
                int index = 1;
                for (var bind : bindParameters) {
                    DBHelper.setBind(index, bind, pstm);
                    index++;
                }
            }

            try (ResultSet rs = pstm.executeQuery()) {
                while (rs.next()) {
                    SheetRow row = DBHelper.parse(rs, SheetRow.class);
                    result.add(row);
                }
            }
        } catch (SQLException sqlEx) {
            log.error("Error while loading data using sql query = {} with bind parameters = {}", selectQuery, bindParameters, sqlEx);
            throw new IllegalStateException(sqlEx);
        }

        return result;
    }
}
