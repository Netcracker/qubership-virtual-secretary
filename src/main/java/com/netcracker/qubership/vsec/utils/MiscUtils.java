package com.netcracker.qubership.vsec.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MiscUtils {
    private static final Logger log = LoggerFactory.getLogger(MiscUtils.class);

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            log.warn("", ex);
        }
    }

    public static String encodeURL(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8)
                .replace("+", "%20")  // Replace + with %20 for spaces
                .replace("%2B", "+"); // Keep actual plus signs
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static <T> T readObjectFromJsonStr(String jsonAsStr, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonAsStr, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error while parsing string as json. Json = '{}'", jsonAsStr, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * In case source string contains JSON but quoted with ```json ... ```
     * the original JSON as string will be returned
     * @return String without quotes
     */
    public static String getJsonFromMDQuotedString(String str) {
        int from = str.indexOf("{");
        int to = str.lastIndexOf("}");

        return str.substring(from, to + 1);
    }
}
