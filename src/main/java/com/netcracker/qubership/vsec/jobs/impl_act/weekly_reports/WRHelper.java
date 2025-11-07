package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.qubership.vsec.db.MyDBMap;
import com.netcracker.qubership.vsec.db.MyDBSheet;
import com.netcracker.qubership.vsec.deepseek.DeepSeekCaller;
import com.netcracker.qubership.vsec.deepseek.ReportAnalysis;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper.SheetData;
import com.netcracker.qubership.vsec.db.SheetRow;
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
                   - Connection to goals (explicit mentions of OKRs, business objectives)
                
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
                
                In case report text contains only references to external resources, is empty or there are other issues with report analysis - reply with requested format anyway.
                
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
