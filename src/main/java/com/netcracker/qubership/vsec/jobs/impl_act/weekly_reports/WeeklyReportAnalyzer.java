package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.model.team.QSMember;
import com.netcracker.qubership.vsec.model.team.QSTeam;
import com.netcracker.qubership.vsec.model.team.QSTeamLoader;
import net.bis5.mattermost.client4.MattermostClient;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WeeklyReportAnalyzer extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MattermostClient client) {
        // prepare data
        QSTeam qsTeam = QSTeamLoader.loadTeam(appProperties.getQubershipTeamConfigFile());
        SheetData sheetData;

        try {
            String urlStr = "https://sheets.googleapis.com/v4/spreadsheets/" + appProperties.getWeeklyReportSheetId() + "/values/" + appProperties.getWeeklyReportSheetName()+ "?key=" + appProperties.getWeeklyReportApiKey();
            sheetData = downloadWeeklyReportsData(urlStr);
        } catch (Exception ex) {
            getLog().error("Error while downloading weekly report", ex);
            throw new IllegalStateException("No weekly report is downloaded");
        }

        // do analyze
        List<ReportWarning> warnings = new ArrayList<>();

        // Algo: starting from the BEGINING date we ensure that each member has created a repo
        LocalDate startDate = getDateByStringPointsToMonday(appProperties.getWeeklyReportsStartDateMonday());
        LocalDate endDate   = LocalDate.now();

        LocalDate curDate   = startDate;
        while (curDate.isBefore(endDate) || curDate.isEqual(endDate)) {
            // Check that reports exists for the date curDate
            List<ReportWarning> foundWarns = checkReportsForTheDate(curDate, sheetData, qsTeam.getMembers());
            warnings.addAll(foundWarns);

            curDate = curDate.plusWeeks(1);
        }

        // report about found warnings
        for (ReportWarning warning : warnings) {
            getLog().warn("Report warning: date = {}, person email = {}", warning.getDate(), warning.getPerson().getEmail());
        }

    }

    public static SheetData downloadWeeklyReportsData(String urlStr) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        URL url = new URI(urlStr).toURL();

        try (InputStream inputStream = url.openStream()) {
            return mapper.readValue(inputStream, SheetData.class);
        }
    }

    /**
     * Returns date of the Monday to which startDateStr argument is pointed too.
     * In case mismatch then next nearest Monday's date is returned
     * @param startDateStrYYYYMMDD String in format yyyy-MM-dd
     * @return LocalDate which exactly matches to Monday
     */
    private static LocalDate getDateByStringPointsToMonday(String startDateStrYYYYMMDD) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.parse(startDateStrYYYYMMDD, formatter);

        // ensure start date points to Monday
        while (startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            startDate = startDate.plusDays(1);
        }

        return startDate;
    }

    private static List<ReportWarning> checkReportsForTheDate(LocalDate mondayDate, SheetData sheetData, List<QSMember> qsMembers) {
        List<ReportWarning> result = new ArrayList<>();

        for (QSMember member : qsMembers) {
            List<SheetRow> personReports = findAllRecordsForAPerson(sheetData, mondayDate, member.getEmail());

            if (personReports.isEmpty()) {
                ReportWarning warning = new ReportWarning(mondayDate, member);
                result.add(warning);
            }
        }

        return result;
    }

    private static List<SheetRow> findAllRecordsForAPerson(SheetData sheetData, LocalDate reportDate, String email) {
        List<SheetRow> result = new ArrayList<>();

        for (SheetRow row : sheetData.getValues()) {
            if (row.getEmail().equals(email)) {
                if (row.getWeekStartDateAsLocalDate().equals(reportDate)) {
                    result.add(row);
                }
            }
        }

        return result;
    }
}
