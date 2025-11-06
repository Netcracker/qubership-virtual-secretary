package com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.netcracker.qubership.vsec.db.SheetRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class SheetDataDeserializer extends JsonDeserializer<SheetData> {

    @Override
    public SheetData deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = p.getCodec().readTree(p);

        SheetData sheetData = new SheetData();
        sheetData.setRange(node.get("range").asText());
        sheetData.setMajorDimension(node.get("majorDimension").asText());

        JsonNode valuesNode = node.get("values");
        List<SheetRow> rows = new ArrayList<>();

        if (valuesNode != null && valuesNode.isArray()) {
            for (JsonNode rowNode : valuesNode) {
                if (rowNode.isArray()) {
                    List<String> rowData = new ArrayList<>();
                    for (JsonNode cellNode : rowNode) {
                        rowData.add(cellNode.asText());
                    }

                    rows.add(new SheetRow(rowData));
                }
            }
        }

        sheetData.setValues(rows);
        return sheetData;
    }
}