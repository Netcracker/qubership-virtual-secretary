package com.netcracker.qubership.vsec;

import com.netcracker.qubership.vsec.jobs.AllJobsRegistry;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.mattermost.MattermostClientFactory;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.utils.DBUtils;
import com.netcracker.qubership.vsec.utils.PropsUtils;
import net.bis5.mattermost.client4.MattermostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class VirtualSecretaryApp {
    private static final Logger log = LoggerFactory.getLogger(VirtualSecretaryApp.class);

    public static void main(String[] args) {
        // Check if program arguments are specified
        if (args.length < 1) {
            System.out.println("Incorrect program arguments");
            System.out.println("Usage example: VirtualSecretaryApp $PATH_TO_PROPERTIES_FILE$");
            System.exit(1);
        }

        // Check if argument goes to properties files
        final AppProperties appProps = PropsUtils.loadFromFile(args[0]);
        final AllJobsRegistry allJobsRegistry = new AllJobsRegistry();

        DBUtils.checkDBDriverOrFail();

        String connectionUrl = DBUtils.buildConnectionUrl(appProps);
        String passString  = DBUtils.buildPasswordString(appProps);
        try (Connection conn = DriverManager.getConnection(connectionUrl, appProps.getDbUserName(), passString)) {
            // Open connection to Mattermost server
            MattermostClient mmClient = MattermostClientFactory.openNewClient(appProps.getMmHost(), appProps.getMmToken(), allJobsRegistry.getRegisteredReflectiveJobs());
            MatterMostClientHelper mmHelper = new MatterMostClientHelper(mmClient);
            mmHelper.setDebugEmailToSendMessagesOnlyTo(appProps.getOnlyAllowedEmailToSendMessagesViaMattermost());

            // Run all jobs
            allJobsRegistry.runAllActiveJobs(appProps, mmHelper, conn);
        } catch (SQLException sqlEx) {
            log.error(ErrorCodes.ERR007 + ": Error while working with DB. Terminating application.", sqlEx);
            System.exit(1);
        }

        // WA: to force closing async threads of Mattermost client
        System.exit(0);
    }

}
