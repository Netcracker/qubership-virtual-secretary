package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;

class SheetDataDeserializer extends JsonDeserializer<SheetData> {
    private static final String KEY_FIELD_MONDAY = "Start date of the reported week (Monday)";
    private static final String KEY_FIELD_WHAT_HAVE_BEEN_DONE = "What have been done";

    @Override
    public SheetData deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = p.getCodec().readTree(p);

        SheetData sheetData = new SheetData();
        sheetData.setRange(node.get("range").asText());
        sheetData.setMajorDimension(node.get("majorDimension").asText());

        JsonNode valuesNode = node.get("values");
        List<SheetRow> rows = new ArrayList<>();

        boolean skipCaptionRow = true;
        if (valuesNode.isArray()) {
            for (JsonNode rowNode : valuesNode) {
                if (rowNode.isArray()) {
                    List<String> rowData = new ArrayList<>();
                    for (JsonNode cellNode : rowNode) {
                        rowData.add(cellNode.asText());
                    }

                    if (skipCaptionRow) {
                        if (rowData.contains(KEY_FIELD_MONDAY) && rowData.contains(KEY_FIELD_WHAT_HAVE_BEEN_DONE)) {
                            skipCaptionRow = false;
                            continue;
                        } else throw new UnexpectedException("No expected fields in the caption row");
                    }

                    rows.add(new SheetRow(rowData));
                }
            }
        }

        sheetData.setValues(rows);
        return sheetData;
    }
}