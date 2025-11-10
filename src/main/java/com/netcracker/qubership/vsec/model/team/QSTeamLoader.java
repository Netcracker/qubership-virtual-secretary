package com.netcracker.qubership.vsec.model.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.qubership.vsec.model.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

public class QSTeamLoader {
    private static final Logger log = LoggerFactory.getLogger(QSTeamLoader.class);

    private static QSTeam cachedValue = null;
    private static ReentrantLock lock = new ReentrantLock();

    public static QSTeam loadTeam(AppProperties appProperties) {
        if (cachedValue != null) return cachedValue;

        lock.lock();
        try {
            if (cachedValue == null) {
                String fileName = appProperties.getQubershipTeamConfigFile();
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    cachedValue = objectMapper.readValue(new File(fileName), QSTeam.class);
                } catch (Exception ex) {
                    log.error("Error while loading Qubership team from configuration file {}", fileName, ex);
                    throw new RuntimeException("Error while loading Qubership team");
                }
            }

            return cachedValue;
        } finally {
            lock.unlock();
        }
    }
}
