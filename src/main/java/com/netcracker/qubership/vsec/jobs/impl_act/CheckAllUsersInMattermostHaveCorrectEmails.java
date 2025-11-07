package com.netcracker.qubership.vsec.jobs.impl_act;

import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.utils.MiscUtils;

import java.sql.Connection;

public class CheckAllUsersInMattermostHaveCorrectEmails extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MatterMostClientHelper mmHelper, Connection conn) {
        // todo: to be implemented later
    }
}
