package com.netcracker.qubership.vsec.utils;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.netcracker.qubership.vsec.model.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static com.netcracker.qubership.vsec.ErrorCodes.ERR001;
import static com.netcracker.qubership.vsec.ErrorCodes.ERR002;

public class PropsUtils {
    private static final Logger log = LoggerFactory.getLogger(PropsUtils.class);

    public static AppProperties loadFromFile(String propsFileName) {
        try {
            JavaPropsMapper mapper = new JavaPropsMapper();
            return mapper.readValue(new File(propsFileName), AppProperties.class);
        } catch (IOException ex) {
            log.error(ERR002 + ": Error while loading application properties from {}", propsFileName, ex);
            System.exit(1);
        }

        throw new IllegalStateException(ERR001 + ": Unexpected application state");
    }
}
