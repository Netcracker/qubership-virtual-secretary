package com.netcracker.qubership.vsec.deepseek;

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

public class DeepSeekCaller {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekCaller.class);

    private final String deepSeekUrl;
    private final String deepSeekToken;

    public DeepSeekCaller(String deepSeekUrl, String deepSeekToken) {
        this.deepSeekUrl = deepSeekUrl;
        this.deepSeekToken = deepSeekToken;

        if (MiscUtils.isEmpty(this.deepSeekUrl))
            throw new IllegalStateException("Empty DeepSeek URL is provided");
        if (MiscUtils.isEmpty(this.deepSeekToken))
            throw new IllegalStateException("Empty DeepSeek access token is provided");
    }

    public String getResponseAsSingleString(String systemRole, String prompt) {
        DeepSeekChatResponse dsResponse = ask(systemRole, prompt);
        return getBasicText(dsResponse);
    }

    public DeepSeekChatResponse ask(String systemRole, String prompt) {
        log.debug("Asking DeepSeek: role = {}, prompt = {}", systemRole, prompt);

        String deepSeekAnswer = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(deepSeekUrl);

            String json = buildRequest(systemRole,prompt);
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + deepSeekToken);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int httpCode = response.getCode();
                if (httpCode != 200) {
                    log.debug("Status code: " + response.getCode());
                    log.debug("Request json = {}", json);
                }

                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    deepSeekAnswer = EntityUtils.toString(
                            responseEntity,
                            StandardCharsets.UTF_8
                    );

                    EntityUtils.consume(responseEntity);
                }

                if (httpCode != 200) {
                    log.debug("Response = '{}'", deepSeekAnswer);
                }
            }
        } catch (Exception ex) {
            log.error("Error while working with DeepSeek", ex);
            throw new IllegalStateException(ex);
        }

        return jsonStrToModel(deepSeekAnswer);
    }

    private String buildRequest(String systemRole, String message) {
        // message = message.replace("\"", "\\\"");
        message = StringEscapeUtils.escapeJson(message);

        return "{\n" +
                "        \"model\": \"deepseek-chat\",\n" +
                "        \"messages\": [\n" +
                "          {\"role\": \"system\", \"content\": \"" + systemRole + "\"},\n" +
                "          {\"role\": \"user\", \"content\": \"" + message + "\"}\n" +
                "        ],\n" +
                "        \"stream\": false\n" +
                "      }";
    }

    private static DeepSeekChatResponse jsonStrToModel(String jsonStr) {
        log.debug("DeepSeek raw json = {}", jsonStr);
        return MiscUtils.readObjectFromJsonStr(jsonStr, DeepSeekChatResponse.class);
    }

    public String getBasicText(DeepSeekChatResponse dsResponse) {
        if (dsResponse == null) return null;
        if (dsResponse.getChoices() == null) return null;
        if (dsResponse.getChoices().isEmpty()) return null;
        if (dsResponse.getChoices().getFirst() == null) return null;
        if (dsResponse.getChoices().getFirst().getMessage() == null) return null;
        return dsResponse.getChoices().getFirst().getMessage().getContent();
    }

    public boolean doSmokeTest() {
        DeepSeekChatResponse dsResponse = ask("You are a helpful AI assistant", "Just reply with 'Hello World' phrase.");
        String responseText = getBasicText(dsResponse);
        return responseText.toLowerCase().contains("hello world");
    }
}
