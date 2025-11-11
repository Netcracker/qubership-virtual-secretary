package com.netcracker.qubership.vsec.jobs.ajobs;

import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;

import java.sql.Connection;

public class AJobToCheckAllUsersInMattermostHaveCorrectEmails extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MatterMostClientHelper mmHelper, Connection conn) {
        // todo: to be implemented later
    }
}
