package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.netcracker.qubership.vsec.db.MyDBMap;
import com.netcracker.qubership.vsec.model.AppProperties;
import net.bis5.mattermost.client4.MattermostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.DayOfWeek;

class WRHelper {
    private static final Logger log = LoggerFactory.getLogger(WRHelper.class);
    private final AppProperties appProperties;
    private final MattermostClient client;
    private final Connection conn;

    public WRHelper(AppProperties appProperties, MattermostClient client, Connection conn) {
        this.appProperties = appProperties;
        this.client = client;
        this.conn = conn;
    }

    void loadLatestDataFromGoogleSheet() {
        MyDBMap myDBMap = new MyDBMap(conn);
        String oldValue = myDBMap.getValue("test");
        String newValue = "" + System.currentTimeMillis();
        myDBMap.setValue("test", newValue);
        myDBMap.saveAllToDB();

        log.info("Current value in myDBmap = {}, setting new value = {}", oldValue, newValue);
    }

    void friendlyNotifyAllToSendReportIfTodayIs(DayOfWeek dayOfWeek) {

    }

    void angryNotifyToSendMissedReportsIfTodayIs(DayOfWeek dayOfWeek) {

    }

    void calculateReportQualityPerPersonAndProvideFeedback() {

    }

    void sendReportToManagementIfTodayIs(DayOfWeek dayOfWeek) {

    }


}
