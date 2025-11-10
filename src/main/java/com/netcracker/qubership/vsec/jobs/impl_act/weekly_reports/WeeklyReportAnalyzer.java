package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;

import java.sql.Connection;

public class WeeklyReportAnalyzer extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MatterMostClientHelper mmHelper, Connection conn) {
        WRHelper wrHelper = new WRHelper(appProperties, mmHelper, conn);
        wrHelper.loadLatestDataFromGoogleSheet();
        wrHelper.friendlyNotifyAllToSendWeeklyReports();
        wrHelper.angryNotifyToSendMissedReports();
        wrHelper.calculateExistedReportsQuality();
        wrHelper.sendFeedbacksToReporters();
        wrHelper.sendReportToManagementChannel();
    }
}
