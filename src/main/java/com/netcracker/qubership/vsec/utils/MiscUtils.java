package com.netcracker.qubership.vsec.utils;

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
}
