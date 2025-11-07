package com.netcracker.qubership.vsec.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PUBLIC)
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

    @JsonProperty(value = "DB_USER_NAME",required = true)
    private String dbUserName;

    @JsonProperty(value = "DB_USER_PASSWORD", required = true)
    private String dbUserPassword;

    @JsonProperty(value = "DB_FILE_ENCRYPTION_PASSWORD", required = true)
    private String dbFileEncryptionPassword;

    /**
     * Name of the database file to persist data into.
     * Hardcoded value "./data/mapdb.db" is used as it is used in github actions.
     */
    public String getDbFileName() {
        return "./data/mapdb.db";
    }

    @JsonProperty(value = "version")
    private String version;

    /**
     * URL of OpenAI-compatible API to be used for scoring of weekly reports using GenAI.
     * DeepSeek is used currently.
     */
    @JsonProperty(value = "DEEP-SEEK-URL")
    private String deepSeekUrl;

    /**
     * Access token to call openAI-compatible API
     */
    @JsonProperty(value = "DEEP-SEEK-TOKEN")
    private String deepSeekToken;

    /**
     * Link to Google Forms Form. Currently, the URL is used in text messages to help team members remind it.
     */
    @JsonProperty(value = "WEEKLY_REPORT_GOOGLE_FORM_URL")
    private String weeklyReportFormUrl;
}
