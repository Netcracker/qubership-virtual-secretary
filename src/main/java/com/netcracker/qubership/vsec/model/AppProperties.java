package com.netcracker.qubership.vsec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppProperties {
    /**
     * Mattermost server instance domain name, i.e. mattermost.mydomain.com
     */
    @JsonProperty(value = "MATTERMOST_HOSTNAME", required = true)
    private String mmHost;

    /**
     * Token value of the Mattermost bot to access mattermost instance
     * and to communicate with the team on behalf of
     */
    @JsonProperty(value = "MATTERMOST_TOKEN", required = true)
    private String mmToken;

    /**
     * In case has any positive value - the application will be terminated automatically
     * on time passed. Useful for local development.
     */
    @JsonProperty(value = "APP_AUTO_TERMINATION_DELAY_IN_SECONDS", defaultValue = "0")
    private Long autoTerminationDelayInSeconds;

    /**
     * All replies of the team are collected into some google sheet.
     * So here we specify the ID of Google sheet to download data from
     */
    @JsonProperty(value = "WEEKLY_REPORT_SHEET_ID", required = true)
    private String weeklyReportSheetId;

    /**
     * All replies of the team are collected into some google sheet.
     * So here we specify the NAME of Google sheet to download data from
     */
    @JsonProperty(value = "WEEKLY_REPORT_SHEET_NAME", required = true)
    private String weeklyReportSheetName;

    /**
     * All replies of the team are collected into some google sheet.
     * This property contains API KEY which must be setup.
     * See <a href="https://docs.cloud.google.com/docs/authentication/api-keys">official documentaiton</a>
     */
    @JsonProperty(value = "WEEKLY_REPORT_API_KEY", required = true)
    private String weeklyReportApiKey;

    /**
     * A path to JSON file with team members to analyze reports for
     */
    @JsonProperty(value = "QUBERSHIP_TEAM_CONFIG_FILE", required = true)
    private String qubershipTeamConfigFile;

    /**
     * The date in format of YYYY-MM-DD which points to Monday.
     * The Analyzer will check accuracy of weekly reports starting from this date.
     */
    @JsonProperty(value = "WEEKLY_REPORT_START_DATE_MONDAY", required = true)
    private String weeklyReportsStartDateMonday;

    public String getMmHost() {
        return mmHost;
    }

    public String getMmToken() {
        return mmToken;
    }

    public Long getAutoTerminationDelayInSeconds() {
        return autoTerminationDelayInSeconds;
    }

    public String getWeeklyReportSheetId() {
        return weeklyReportSheetId;
    }

    public String getWeeklyReportSheetName() {
        return weeklyReportSheetName;
    }

    public String getWeeklyReportApiKey() {
        return weeklyReportApiKey;
    }

    public String getQubershipTeamConfigFile() {
        return qubershipTeamConfigFile;
    }

    public String getWeeklyReportsStartDateMonday() {
        return weeklyReportsStartDateMonday;
    }
}
