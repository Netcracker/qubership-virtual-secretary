package com.netcracker.qubership.vsec.deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;

@Data
public class ReportAnalysis {

    @JsonProperty("scores")
    private Scores scores;

    @JsonProperty("analysis")
    private Analysis analysis;

    @JsonProperty("recommendations")
    private Recommendations recommendations;

    @Data
    public static class Scores {
        @JsonProperty("content_score")
        private int contentScore;

        @JsonProperty("impact_score")
        private int impactScore;

        @JsonProperty("proactivity_score")
        private int proactivityScore;

        @JsonProperty("context_score")
        private int contextScore;

        @JsonProperty("final_score")
        private double finalScore;
    }

    @Data
    public static class Analysis {
        @JsonProperty("content_justification")
        private String contentJustification;

        @JsonProperty("impact_justification")
        private String impactJustification;

        @JsonProperty("proactivity_justification")
        private String proactivityJustification;

        @JsonProperty("context_justification")
        private String contextJustification;
    }

    @Data
    public static class Recommendations {
        @JsonProperty("strengths")
        private List<String> strengths;

        @JsonProperty("improvements")
        private List<String> improvements;

        public String getStrengthOneString() {
            StringBuilder sb = new StringBuilder();
            int counter = 1;
            for (String line : getStrengths()) {
                sb.append(counter).append(". ");
                sb.append(line).append("\n");
            }

            return sb.toString();
        }

        public String getImprovementsOneString() {
            StringBuilder sb = new StringBuilder();
            int counter = 1;
            for (String line : getImprovements()) {
                sb.append(counter).append(". ");
                sb.append(line).append("\n");
            }

            return sb.toString();
        }
    }

    public static ReportAnalysis createFromString(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonString, ReportAnalysis.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
