package com.netcracker.qubership.vsec.utils;

import com.netcracker.qubership.vsec.ErrorCodes;
import com.netcracker.qubership.vsec.model.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtils {
    private static final Logger log = LoggerFactory.getLogger(DBUtils.class);
    private static final String DB_DRIVER = "org.h2.Driver";

    public static void checkDBDriverOrFail() {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException ex) {
            log.error(ErrorCodes.ERR003 + ": No '{}' DB driver class is found. Terminating app.", DB_DRIVER, ex);
            System.exit(1);
        }
    }

    public static String buildConnectionUrl(AppProperties appProperties) {
        return "jdbc:h2:" + appProperties.getDbFileName() + ";CIPHER=AES;";
    }

    public static String buildPasswordString(AppProperties appProperties) {
        return appProperties.getDbFileEncryptionPassword() + " " + appProperties.getDbUserPassword();
    }
}
