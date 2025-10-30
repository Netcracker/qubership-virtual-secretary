package com.netcracker.qubership.vsec.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiscUtils {
    private static final Logger log = LoggerFactory.getLogger(MiscUtils.class);

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            log.warn("", ex);
        }
    }
}
