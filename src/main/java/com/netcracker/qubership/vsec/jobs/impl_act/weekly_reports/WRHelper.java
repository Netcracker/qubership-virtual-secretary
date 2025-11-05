package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.qubership.vsec.db.MyDBMap;
import com.netcracker.qubership.vsec.db.MyDBSheet;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper_models.SheetData;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.model.team.QSTeam;
import com.netcracker.qubership.vsec.model.team.QSTeamLoader;
import com.netcracker.qubership.vsec.utils.MiscUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

class WRHelper {
    private static final String KEY_WR_LAST_LOADED_ROW = "WR-LAST-LOADED-ROW";

    private static final Logger log = LoggerFactory.getLogger(WRHelper.class);
    private final AppProperties appProperties;
    private final MatterMostClientHelper mmHelper;
    private final Connection conn;
    private final MyDBMap myDBMap;
    private final MyDBSheet myDBSheet;

    public WRHelper(AppProperties appProperties, MatterMostClientHelper mmHelper, Connection conn) {
        this.appProperties = appProperties;
        this.mmHelper = mmHelper;
        this.conn = conn;
        this.myDBMap = new MyDBMap(conn);
        this.myDBSheet = new MyDBSheet(conn);
    }

    /**
     *
     */
    void loadLatestDataFromGoogleSheet() {
        String lastLoadedRow = myDBMap.getValue(KEY_WR_LAST_LOADED_ROW, "2");

        SheetData sheetData;

        try {
            String sheetNameEncoded = "'" + MiscUtils.encodeURL(appProperties.getWeeklyReportSheetName()) + "'";
            String range = "A" + lastLoadedRow + ":G";
            String sheetId = appProperties.getWeeklyReportSheetId();
            String apiKey = appProperties.getWeeklyReportApiKey();

            // See documentation at https://developers.google.com/workspace/sheets/api/samples/reading
            String urlStr = "https://sheets.googleapis.com/v4/spreadsheets/" + sheetId + "/values/" + sheetNameEncoded + "!" + range + "?" + "&key=" + apiKey;

            // Download data from Google Sheets
            sheetData = downloadWeeklyReportsData(urlStr);

            // Persist data into database
            int rowsAdded = myDBSheet.appendData(sheetData);
            if (rowsAdded != sheetData.getValues().size()) {
                throw new IllegalStateException("Unexpected state: number of added rows into DB differs to downloaded from Google Sheet: " +
                        "added = " + rowsAdded + ", downloaded = " + sheetData.getValues().size());
            }

            myDBMap.setValue(KEY_WR_LAST_LOADED_ROW, "" + (Integer.parseInt(lastLoadedRow) + rowsAdded));
            myDBMap.saveAllToDB();

            log.info("Data loaded from Google sheets = {}", sheetData.getValues().size());
        } catch (Exception ex) {
            log.error("Error while downloading weekly report", ex);
            throw new IllegalStateException("No weekly report is downloaded");
        }


    }

    /**
     *
     * @param dayOfWeek
     */
    void friendlyNotifyAllToSendReportIfTodayIsEndOf(DayOfWeek dayOfWeek) {

    }

    private static final LocalDate FROM_DATE = LocalDate.of(2025, 10, 20);
    private static final String WARN_VALUE = "Notified at ";

    /**
     * Notifies all members of the team to send reports in case there are still no reports from them.
     * Only reports for the passed week are taken into account.
     * We based on the fact that a week begins from MONDAY day of week.
     *
     * The notification will be done once per missed date per email.
     *
     */
    void angryNotifyToSendMissedReports() {
        // we need to process all reports up to the Monday (including) of week ago
        LocalDate dateToProceedTill = LocalDate.now().minusWeeks(1);
        while (!dateToProceedTill.getDayOfWeek().equals(DayOfWeek.MONDAY)) dateToProceedTill = dateToProceedTill.minusDays(1);

        QSTeam qsTeam = QSTeamLoader.loadTeam(appProperties.getQubershipTeamConfigFile());
        List<String> emails = qsTeam.getAllEmails();
        Map<String, List<LocalDate>> missedReports = myDBSheet.findMissedReportRecords(emails, FROM_DATE, dateToProceedTill);

        for (var me : missedReports.entrySet()) {
            String email  = me.getKey();
            List<LocalDate> dates = me.getValue();

            for (LocalDate date : dates) {
                String key = email + " for " + date;
                String value = myDBMap.getValue(key);
                if (value != null) {
                    log.debug("User {} was already notified for missed report date = {}: {}", email, date, value);
                    continue;
                }


                value = WARN_VALUE + LocalDate.now();
                log.info("Sending notification to a user for missed report: email = {} for date = {}: {}", me.getKey(), me.getValue(), value);
                myDBMap.setValue(key, value);
            }

            myDBMap.saveAllToDB();
        }
    }

    /**
     *
     */
    void calculateReportQualityPerPersonAndProvideFeedbackIfTodayIfNoonOf(DayOfWeek dayOfWeek) {

    }

    /**
     *
     * @param dayOfWeek
     */
    void sendReportToManagementIfTodayIsNoonOf(DayOfWeek dayOfWeek) {

    }

    public static SheetData downloadWeeklyReportsData(String urlStr) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        URL url = new URI(urlStr).toURL();

        try (InputStream inputStream = url.openStream()) {
            return mapper.readValue(inputStream, SheetData.class);
        }
    }


}
