package com.netcracker.qubership.vsec.genai;

import com.netcracker.qubership.vsec.utils.MiscUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class GenAICaller {
    private static final Logger log = LoggerFactory.getLogger(GenAICaller.class);

    private final String openAIURL;
    private final String openAIToken;
    private final String modelName;

    public GenAICaller(String openAIURL, String openAIToken, String modelName) {
        this.openAIURL = openAIURL;
        this.openAIToken = openAIToken;
        this.modelName = modelName;

        if (MiscUtils.isEmpty(this.openAIURL))
            throw new IllegalStateException("Empty OpenAI URL is provided");
        if (MiscUtils.isEmpty(this.openAIToken))
            throw new IllegalStateException("Empty OpenAI access token is provided");
        if (MiscUtils.isEmpty(this.modelName))
            throw new IllegalStateException("Empty OpenAI model name is provided");
    }

    public String getResponseAsSingleString(String systemRole, String prompt) {
        GenAIResponseModel dsResponse = ask(systemRole, prompt);
        return getBasicText(dsResponse);
    }

    public GenAIResponseModel ask(String systemRole, String prompt) {
        log.debug("Asking GenAI: role = {}, prompt = {}", systemRole, prompt);

        String genAIResponse = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(openAIURL);

            String json = buildRequest(systemRole,prompt);
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + openAIToken);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int httpCode = response.getCode();
                if (httpCode != 200) {
                    log.debug("Status code: " + response.getCode());
                    log.debug("Request json = {}", json);
                }

                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    genAIResponse = EntityUtils.toString(
                            responseEntity,
                            StandardCharsets.UTF_8
                    );

                    EntityUtils.consume(responseEntity);
                }

                if (httpCode != 200) {
                    log.debug("Response = '{}'", genAIResponse);
                }
            }
        } catch (Exception ex) {
            log.error("Error while working with GenAI", ex);
            throw new IllegalStateException(ex);
        }

        return jsonStrToModel(genAIResponse);
    }

    private String buildRequest(String systemRole, String message) {
        // message = message.replace("\"", "\\\"");
        message = StringEscapeUtils.escapeJson(message);

        return "{\n" +
                "        \"model\": \"" + modelName + "\",\n" +
                "        \"messages\": [\n" +
                "          {\"role\": \"system\", \"content\": \"" + systemRole + "\"},\n" +
                "          {\"role\": \"user\", \"content\": \"" + message + "\"}\n" +
                "        ],\n" +
                "        \"stream\": false\n" +
                "      }";
    }

    private static GenAIResponseModel jsonStrToModel(String jsonStr) {
        log.debug("GenAI raw json = {}", jsonStr);
        return MiscUtils.readObjectFromJsonStr(jsonStr, GenAIResponseModel.class);
    }

    public String getBasicText(GenAIResponseModel dsResponse) {
        if (dsResponse == null) return null;
        if (dsResponse.getChoices() == null) return null;
        if (dsResponse.getChoices().isEmpty()) return null;
        if (dsResponse.getChoices().getFirst() == null) return null;
        if (dsResponse.getChoices().getFirst().getMessage() == null) return null;
        return dsResponse.getChoices().getFirst().getMessage().getContent();
    }

    public boolean doSmokeTest() {
        GenAIResponseModel dsResponse = ask("You are a helpful AI assistant", "Just reply with 'Hello World' phrase.");
        String responseText = getBasicText(dsResponse);
        return responseText.toLowerCase().contains("hello world");
    }
}
