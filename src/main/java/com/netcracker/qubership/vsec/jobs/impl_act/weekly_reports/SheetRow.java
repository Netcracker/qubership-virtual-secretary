package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SheetRow {
    private static final DateTimeFormatter DATE_FORMATTER_CREATED_WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_REPORT_FOR_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");


    @JsonProperty("timestamp")
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("blueStreamLead")
    private String blueStreamLead;

    @JsonProperty("weekStartDate")
    @JsonFormat(pattern = "dd.MM.yyyy")
    private String weekStartDate;

    @JsonProperty("completedWork")
    private String completedWork;

    @JsonProperty("nextWeekPlans")
    private String nextWeekPlans;

    @JsonProperty("email")
    private String email;

    @JsonIgnore
    private LocalDate weekStartDateAsLocalDate;

    // Default constructor
    public SheetRow() {
    }

    // Constructor for creating from string array (useful for parsing)
    @JsonCreator
    public SheetRow(List<String> rowData) {
        if (rowData != null && rowData.size() >= 7) {
            this.timestamp = LocalDateTime.parse(rowData.get(0), DATE_FORMATTER_CREATED_WHEN);
            this.fullName = rowData.get(1);
            this.blueStreamLead = rowData.get(2);
            this.weekStartDate = rowData.get(3);
            this.completedWork = rowData.get(4);
            this.nextWeekPlans = rowData.get(5);
            this.email = rowData.get(6);
        }
    }

    // Getters and setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBlueStreamLead() {
        return blueStreamLead;
    }

    public void setBlueStreamLead(String blueStreamLead) {
        this.blueStreamLead = blueStreamLead;
    }

    public String getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(String weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public String getCompletedWork() {
        return completedWork;
    }

    public void setCompletedWork(String completedWork) {
        this.completedWork = completedWork;
    }

    public String getNextWeekPlans() {
        return nextWeekPlans;
    }

    public void setNextWeekPlans(String nextWeekPlans) {
        this.nextWeekPlans = nextWeekPlans;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getWeekStartDateAsLocalDate() {
        if (weekStartDateAsLocalDate == null) {
            weekStartDateAsLocalDate = LocalDate.parse(weekStartDate, DATE_FORMATTER_REPORT_FOR_DATE);
        }
        return weekStartDateAsLocalDate;
    }
}