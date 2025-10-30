package com.netcracker.qubership.vsec.model.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class QSTeamLoader {
    private static final Logger log = LoggerFactory.getLogger(QSTeamLoader.class);

    public static QSTeam loadTeam(final String fileName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(new File(fileName), QSTeam.class);
        } catch (Exception ex) {
            log.error("Error while loading Qubership team from configuration file {}", fileName, ex);
            throw new RuntimeException("Error while loading Qubership team");
        }
    }
}
