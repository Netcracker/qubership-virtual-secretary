package com.netcracker.qubership.vsec.jobs.impl_act;

import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.utils.MiscUtils;

import java.sql.Connection;

/**
 * In case APP_AUTO_TERMINATION_DELAY_IN_SECONDS property has positive value - this action terminates application after time is passed.
 * Useful for local development as application requires manual stop.
 */
public class AutoTerminateApp extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MatterMostClientHelper mmHelper, Connection conn) {
        if (appProperties.getAutoTerminationDelayInSeconds() > 0) {
            long delaySecs = appProperties.getAutoTerminationDelayInSeconds();

            getLog().warn("Application will be terminated in {} seconds as specified in properties", delaySecs);
            MiscUtils.sleep(delaySecs * 1000);

            getLog().info("Terminating application as APP_AUTO_TERMINATION_DELAY_IN_SECONDS is set to {} seconds", delaySecs);
            System.exit(0);
        }
    }
}
