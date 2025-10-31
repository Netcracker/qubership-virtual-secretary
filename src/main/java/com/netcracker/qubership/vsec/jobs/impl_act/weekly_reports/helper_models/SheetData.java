package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper_models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(using = SheetDataDeserializer.class)
public class SheetData {

    @JsonProperty("range")
    private String range;

    @JsonProperty("majorDimension")
    private String majorDimension;

    @JsonProperty("values")
    private List<SheetRow> values;

    // Constructors, getters, and setters
    public SheetData() {
    }

    public SheetData(String range, String majorDimension, List<SheetRow> values) {
        this.range = range;
        this.majorDimension = majorDimension;
        this.values = values;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getMajorDimension() {
        return majorDimension;
    }

    public void setMajorDimension(String majorDimension) {
        this.majorDimension = majorDimension;
    }

    public List<SheetRow> getValues() {
        return values;
    }

    public void setValues(List<SheetRow> values) {
        this.values = values;
    }
}