package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper_models.ReportWarning;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper_models.SheetData;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper_models.SheetRow;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.model.team.QSMember;
import com.netcracker.qubership.vsec.model.team.QSTeam;
import com.netcracker.qubership.vsec.model.team.QSTeamLoader;
import net.bis5.mattermost.client4.MattermostClient;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.time.DayOfWeek.*;

public class WeeklyReportAnalyzer extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MattermostClient client, Connection conn) {
        WRHelper wrHelper = new WRHelper(appProperties, client, conn);
        wrHelper.loadLatestDataFromGoogleSheet();
        wrHelper.friendlyNotifyAllToSendReportIfTodayIs(FRIDAY);
        wrHelper.angryNotifyToSendMissedReportsIfTodayIs(MONDAY);
        wrHelper.calculateReportQualityPerPersonAndProvideFeedback();
        wrHelper.sendReportToManagementIfTodayIs(TUESDAY);

        if (true) return;

        // prepare data
        QSTeam qsTeam = QSTeamLoader.loadTeam(appProperties.getQubershipTeamConfigFile());

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
        while (startDate.getDayOfWeek() != MONDAY) {
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
