package com.netcracker.qubership.vsec.jobs.ajobs.weekly_reports;

import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;

import java.sql.Connection;

public class AJobToAnalyzeWeeklyReports extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MatterMostClientHelper mmHelper, Connection conn) {
        WRHelper wrHelper = new WRHelper(appProperties, mmHelper, conn);

        wrHelper.loadAndStoreLatestDataFromGoogleSheet();
        wrHelper.friendlyNotifyAllToSendWeeklyReports();
        wrHelper.angryNotifyToSendMissedReports();
        wrHelper.calculateExistedReportsQuality();
        wrHelper.sendFeedbacksToReporters();
        wrHelper.sendReportToManagementChannel();
    }
}
