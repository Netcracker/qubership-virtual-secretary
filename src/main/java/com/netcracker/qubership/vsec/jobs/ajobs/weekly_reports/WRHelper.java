package com.netcracker.qubership.vsec.jobs.ajobs.weekly_reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.qubership.vsec.db.MyDBMap;
import com.netcracker.qubership.vsec.db.MyDBSheet;
import com.netcracker.qubership.vsec.deepseek.DeepSeekCaller;
import com.netcracker.qubership.vsec.deepseek.ReportAnalysis;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.model.googlesheets.SheetData;
import com.netcracker.qubership.vsec.model.googlesheets.SheetRow;
import com.netcracker.qubership.vsec.model.team.QSMember;
import com.netcracker.qubership.vsec.model.team.QSTeam;
import com.netcracker.qubership.vsec.model.team.QSTeamLoader;
import com.netcracker.qubership.vsec.utils.MiscUtils;
import net.bis5.mattermost.model.User;
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
     * Download new rows from Google Sheet starting from the row which was remembered last time.
     * After data is downloaded - it will be stored into database.
     * If new records for the same date will appear - then obsolete records will be removed.
     * In case some mistakes will be found (human factor) - they will be modified
     */
    void loadAndStoreLatestDataFromGoogleSheet() {
        String lastLoadedRow = myDBMap.getValue(KEY_WR_LAST_LOADED_ROW, "2");
        if (Integer.parseInt(lastLoadedRow) < 2) lastLoadedRow = "2"; // small correcness of minimal value - useful during local development

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

            QSTeam qsTeam = QSTeamLoader.loadTeam(appProperties);
            myDBSheet.correctMistakesInDB(qsTeam);

            myDBMap.setValue(KEY_WR_LAST_LOADED_ROW, "" + (Integer.parseInt(lastLoadedRow) + rowsAdded));
            myDBMap.saveAllToDB();

            log.info("Data loaded from Google sheets = {}", sheetData.getValues().size());
        } catch (Exception ex) {
            log.error("Error while downloading weekly report", ex);
            throw new IllegalStateException("No weekly report is downloaded");
        }


    }

    /**
     * Notifies user to provide a report for the week, which is identified by a nearest happened Friday.
     * A special mark in database will be stored - not to sent message once again for the selected week.
     */
    void friendlyNotifyAllToSendWeeklyReports() {
        // Find nearest happened Friday we have to notify at
        final LocalDate today = LocalDate.now();
        LocalDate selectedFriday = today;
        while (!selectedFriday.getDayOfWeek().equals(DayOfWeek.FRIDAY)) selectedFriday = selectedFriday.minusDays(1);

        final String notificationMsg = """
                Good day  :raised_hand_with_fingers_splayed:\s
                Don't forget to fill a weekly report today using %s form.
                Thank you!
                """.formatted(appProperties.getWeeklyReportFormUrl());

        // Iterate over the team
        QSTeam qsTeam = QSTeamLoader.loadTeam(appProperties);
        for (QSMember member : qsTeam.getMembers()) {

            // Check if notification for calculated friday was done already
            String key = "FRIENDLY NOTIFICATION at Friday " + selectedFriday + " for " + member.getEmail();
            String value = myDBMap.getValue(key);
            if (!MiscUtils.isEmpty(value)) {
                log.info("User {} is already friendly notified to fill the weekly report today, selected Friday = {}", member.getEmail(), selectedFriday);
                continue;
            }

            User user = mmHelper.getUserByEmail(member.getEmail());
            mmHelper.sendMessage(notificationMsg, user);
            log.info("User {} is friendly notified to fill the weekly report today", member.getEmail());

            myDBMap.setValue(key, selectedFriday.toString());
            myDBMap.saveAllToDB();
        }
    }

    private static final LocalDate FROM_DATE = LocalDate.of(2025, 10, 20);

    /**
     * Notifies all members of the team to send reports in case there are still no reports from them.
     * Only reports for the passed weeks are taken into account.
     * We based on the fact that a week begins from MONDAY day of week.
     * The notification will be sent once per day, i.e. next day the same notification may happen again
     *
     */
    void angryNotifyToSendMissedReports() {
        final LocalDate today = LocalDate.now();
        final String todayStr = today.toString();

        // we need to process all reports up to the Monday (including) of week ago
        LocalDate dateToProceedTill = LocalDate.now().minusWeeks(1);
        while (!dateToProceedTill.getDayOfWeek().equals(DayOfWeek.MONDAY)) dateToProceedTill = dateToProceedTill.minusDays(1);

        final String ANGRY_NOTIFICATION_MSG = """
                :warning: You still have not sent report for %s week.\s
                Please do it ASAP using %s form.
                """;

        QSTeam qsTeam = QSTeamLoader.loadTeam(appProperties);
        List<String> emails = qsTeam.getAllEmails();
        Map<String, List<LocalDate>> missedReports = myDBSheet.findMissedReportRecords(emails, FROM_DATE, dateToProceedTill);

        log.info("Number of non-received reports = {}", missedReports.size());

        for (var me : missedReports.entrySet()) {
            String email  = me.getKey();
            List<LocalDate> dates = me.getValue();

            for (LocalDate date : dates) {
                final String key = "ANGRY NOTE FOR REPORT " + date + " FOR " + email; // email + " for " + date;
                String value = myDBMap.getValue(key);
                if (value != null) {
                    // check at least one day is passed since the notification
                    LocalDate lastSentDate = LocalDate.parse(value);
                    if (today.equals(lastSentDate)) {
                        log.info("User {} was already notified for missed report date = {}: {}", email, date, value);
                        continue;
                    }
                }


                value = todayStr;
                log.info("Sending notification to a user for missed report: email = {} for date = {}: {}", me.getKey(), me.getValue(), value);

                User user = mmHelper.getUserByEmail(email);
                String msg = ANGRY_NOTIFICATION_MSG.formatted(me.getValue(), appProperties.getWeeklyReportFormUrl());
                mmHelper.sendMessage(msg, user);

                myDBMap.setValue(key, value);
            }

            myDBMap.saveAllToDB();
        }
    }

    /**
     *
     */
    void calculateExistedReportsQuality() {
        DeepSeekCaller deepSeekCaller = new DeepSeekCaller(appProperties.getDeepSeekUrl(), appProperties.getDeepSeekToken());
        if (!deepSeekCaller.doSmokeTest()) {
            log.error("Deepseek smoke test is not passed. Please check settings & token");
            System.exit(1);
        }

        final String roleStr = "";
        final String promptTemplate = """
                You are an AI assistant that objectively evaluates weekly reports from developers and managers against strict criteria. Your task is to analyze the report text and assign scores across 4 key categories, then calculate a final score.
                
                EVALUATION CRITERIA (Scale 1-10):
                
                1. CONTENT & COMPLETENESS
                   - Information density (specifics vs. filler content)
                   - Coverage of work categories (development, research, collaboration, operations, improvements)
                   - Structure (logical sections, lists, paragraphs)
                
                2. IMPACT & RESULTS
                   - Use of action verbs (completed, fixed, improved vs. started, worked on)
                   - Presence of measurable results (numbers, metrics, quantitative indicators)
                   - Connection to goals (explicit mentions of Objectives and Key Results)
                
                3. PROACTIVITY & PROBLEM-SOLVING
                   - Description of problems and proposed/implemented solutions
                   - Mention of plans, risks, future improvements
                   - Constructive analysis of difficulties
                
                4. PROFESSIONAL CONTEXT
                   - Use of technical/professional terminology
                   - Mention of code review processes, collaboration, code quality
                   - Specificity and depth of technical details
                
                FINAL SCORE FORMULA:
                Final_Score = 0.3×Content_Score + 0.4×Impact_Score + 0.2×Proactivity_Score + 0.1×Context_Score
                
                ANALYSIS INSTRUCTIONS:
                1. Read the report carefully
                2. Evaluate each criterion on a 1-10 scale with justification
                3. Calculate the final score using the formula
                4. Provide detailed commentary with strengths and improvement recommendations
                5. Be strict but fair - low scores must be justified
                
                RESPONSE FORMAT:
                {
                  "scores": {
                    "content_score": X,
                    "impact_score": X,
                    "proactivity_score": X,
                    "context_score": X,
                    "final_score": X.X
                  },
                  "analysis": {
                    "content_justification": "text...",
                    "impact_justification": "text...",
                    "proactivity_justification": "text...",
                    "context_justification": "text..."
                  },
                  "recommendations": {
                    "strengths": ["strength 1", "strength 2"],
                    "improvements": ["recommendation 1", "recommendation 2"]
                  }
                }
                
                In case report text contains only references to external resources or is empty or there are other issues with report analysis - reply with requested format anyway.
                In case vacation is reported - then return -1 for all scores including final_score, all other details/recommendations must be empty strings.
                
                The report text to analyze goes next:
                """;

        // select one next report to analyze
        List<SheetRow> sheetRows = myDBSheet.getReportsWithNoQualityScore();
        for (SheetRow row : sheetRows) {
            String finalPrompt = promptTemplate + row.getCompletedWork();
            String strResponse = deepSeekCaller.getResponseAsSingleString(roleStr, finalPrompt);
            String jsonStr = MiscUtils.getJsonFromMDQuotedString(strResponse);
            try {
                ReportAnalysis reportAnalysis = ReportAnalysis.createFromString(jsonStr);
                myDBSheet.saveAnalysisIntoDB(row, reportAnalysis);

                log.info("DeepSeek answer = " + reportAnalysis);
                // break;
            } catch (Exception ex) {
                log.error("Error while parsing string as json into ReportAnalysis class [{}]", jsonStr, ex);
                throw new IllegalStateException(ex);
            }
        }

        log.info("All reports are analyzed");
    }

    /**
     * Collects overall status, prints table in MD format and sents it to management channel
     * Table format is follow:
     * Reporter-Email | Date1| Date2| ...
     * ...............| Status or Score value | ...
     *
     */
    void sendReportToManagementChannel() {
        List<SheetRow> allRows = myDBSheet.loadByQuery("select reporter_email, report_date, genai_final_score from my_db_sheet order by reporter_email");

        // calculate number of dates - we've fetch - to understand length of the final report
        List<String> uniqueDates = allRows.stream().map(SheetRow::getWeekStartDate).distinct().toList();

        QSTeam team = QSTeamLoader.loadTeam(appProperties);
        List<String> uniqueEmails = team.getMembers().stream().map(QSMember::getEmail).distinct().sorted().toList();

        // build report caption
        StringBuilder sb = new StringBuilder("|Reporter");
        for (var date : uniqueDates) {
            sb.append("|").append(date);
        }
        sb.append("|\n");

        // build sub-caption
        sb.append("|:---");
        for (var date : uniqueDates) {
            sb.append("|:---:");
        }
        sb.append("|\n");

        // build body

        for (String email : uniqueEmails) {
            sb.append("|").append(email);
            for (String date : uniqueDates) {
                String value = ":warning:";

                for (var row : allRows) {
                    if (email.equals(row.getEmail()) && date.equals(row.getWeekStartDate())) {
                        value = "" + row.getGenAIFinalScore();
                    }
                }

                if (value.equals("-1")) value = ":desert_island:"; // vacation

                sb.append("|").append(value);
            }
            sb.append("|\n");
        }

        log.info("Markdown report result:\n{}", sb);
    }

    static SheetData downloadWeeklyReportsData(String urlStr) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        URL url = new URI(urlStr).toURL();

        try (InputStream inputStream = url.openStream()) {
            return mapper.readValue(inputStream, SheetData.class);
        }
    }

    /**
     * If there are reports with quality score ready - then we need notify reported about calculated score
     *
     */
    void sendFeedbacksToReporters() {
        final String todayStr = LocalDate.now().toString();

        // step1: select rows where final_score > 0 order by report_date
        // step2: check notification was not sent before
        // step3: send message
        // step4: store flag - notification was sent

        List<SheetRow> rows = myDBSheet.getReportsWithQualityScore();
        for (SheetRow row : rows) {
            String key = "SCORES FOR REPORT " + row.getWeekStartDate() + " FOR " + row.getEmail();
            String actValue = myDBMap.getValue(key);
            if (!MiscUtils.isEmpty(actValue)) continue;

            String reportDate = row.getWeekStartDate();
            String finalScores= row.getGenAIFinalScore().toString();
            String strengths  = row.getAnalysisStrengths();
            String recommendations = row.getAnalysisImprovements();
            String reportText = row.getCompletedWork();

            String msg = """
                    :star2: Your report for the week **%s** was rated **%s out of 10**.
                    Your good strengths in the report are:
                    > %s
                    
                    Here are recommendations to be taken into account for future reports:
                    > %s
                    
                    Original report text:
                    ```text
                    %s
                    ```
                    Thank you!
                    """.formatted(reportDate, finalScores, strengths, recommendations, reportText);

            User user = mmHelper.getUserByEmail(row.getEmail());
            if (user == null || user.getEmail() == null) {
                log.error("Can't find user in Mattermost by email {}", row.getEmail());
                continue;
            }

            mmHelper.sendMessage(msg, user);

            myDBMap.setValue(key, todayStr);
            myDBMap.saveAllToDB();

            log.info("Notification about report scores for week {} was sent to user {}", reportDate, row.getEmail());
        }
    }


}
