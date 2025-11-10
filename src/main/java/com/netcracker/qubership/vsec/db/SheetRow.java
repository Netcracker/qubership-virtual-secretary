package com.netcracker.qubership.vsec.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter @Setter
public class SheetRow {
    private static final DateTimeFormatter DATE_FORMATTER_CREATED_WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy H:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_REPORT_FOR_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @JsonIgnore
    @MyDBColumn("id")
    private String id;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @MyDBColumn("created_when")
    private LocalDateTime timestamp;

    @JsonProperty("fullName")
    @MyDBColumn("reporter_name")
    private String fullName;

    @JsonProperty("blueStreamLead")
    private String blueStreamLead;

    @JsonProperty("weekStartDate")
    @JsonFormat(pattern = "dd.MM.yyyy")
    @MyDBColumn("report_date")
    private String weekStartDate;

    @JsonProperty("completedWork")
    @MyDBColumn("msg_done")
    private String completedWork;

    @JsonProperty("nextWeekPlans")
    @MyDBColumn("msg_plans")
    private String nextWeekPlans;

    @JsonProperty("email")
    @MyDBColumn("reporter_email")
    private String email;

    @JsonIgnore
    private LocalDate weekStartDateAsLocalDate;

    @JsonIgnore
    @MyDBColumn("genai_content_score")
    private Integer genAIContentScore;

    @JsonIgnore
    @MyDBColumn("genai_impact_score")
    private Integer genAIImpactScore;

    @JsonIgnore
    @MyDBColumn("genai_proactivity_score")
    private Integer genAIProactivityScore;

    @JsonIgnore
    @MyDBColumn("genai_context_score")
    private Integer genAIContextScore;

    @JsonIgnore
    @MyDBColumn("genai_final_score")
    private Integer genAIFinalScore;

    @JsonIgnore
    @MyDBColumn("genai_analysis_content")
    private String analysisContent;

    @JsonIgnore
    @MyDBColumn("genai_analysis_impact")
    private String analysisImpact;

    @JsonIgnore
    @MyDBColumn("genai_analysis_proactivity")
    private String analysisProactivity;

    @JsonIgnore
    @MyDBColumn("genai_analysis_context")
    private String analysisContext;

    @JsonIgnore
    @MyDBColumn("genai_analysis_strength")
    private String analysisStrengths;

    @JsonIgnore
    @MyDBColumn("genai_analysis_improvements")
    private String analysisImprovements;

    // Default constructor
    public SheetRow() {
    }

    // Constructor for creating from string array (useful for parsing) which comes from Google Sheets
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

    public LocalDate getWeekStartDateAsLocalDate() {
        if (weekStartDateAsLocalDate == null) {
            weekStartDateAsLocalDate = LocalDate.parse(weekStartDate, DATE_FORMATTER_REPORT_FOR_DATE);
        }
        return weekStartDateAsLocalDate;
    }
}