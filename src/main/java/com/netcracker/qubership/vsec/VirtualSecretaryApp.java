package com.netcracker.qubership.vsec;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.netcracker.qubership.vsec.jobs.AllJobsRegistry;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.mattermost.MattermostClientFactory;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.utils.DBUtils;
import net.bis5.mattermost.client4.MattermostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
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

        // Adding ability to do test runs of the application in the remove environment (Github VMs)
        if ("--fake-run".equals(args[0])) {
            fakeRun(args);
            System.exit(0);
        }

        // Check if argument goes to properties files
        final String propsFileName = args[0];
        AppProperties appProps = null;
        try {
            JavaPropsMapper mapper = new JavaPropsMapper();
            appProps = mapper.readValue(new File(propsFileName), AppProperties.class);
        } catch (IOException ex) {
            log.error("Error while loading application properties from {}", propsFileName, ex);
            System.exit(1);
        }

        if (appProps == null) {
            System.out.printf("Can't load properties from the file %s\n", propsFileName);
            System.out.println("Terminating application");
            System.exit(1);
        }

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
            log.error("Error while working with DB. Terminating application.", sqlEx);
            System.exit(1);
        }

        System.exit(0);
    }

    private static void fakeRun(String[] args) {
        System.out.println("Hello world!");
    }

}
