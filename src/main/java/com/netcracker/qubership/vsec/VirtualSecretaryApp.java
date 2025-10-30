package com.netcracker.qubership.vsec;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.netcracker.qubership.vsec.jobs.AllJobsRegistry;
import com.netcracker.qubership.vsec.mattermost.MattermostClientFactory;
import com.netcracker.qubership.vsec.model.AppProperties;
import net.bis5.mattermost.client4.MattermostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class VirtualSecretaryApp {
    private static final Logger log = LoggerFactory.getLogger(VirtualSecretaryApp.class);

    public static void main(String[] args) {
        // Check if program arguments are specified
        if (args.length != 1) {
            System.out.println("Incorrect program arguments");
            System.out.println("Usage example: VirtualSecretaryApp $PATH_TO_PROPERTIES_FILE$");
            System.exit(1);
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

        AllJobsRegistry allJobsRegistry = new AllJobsRegistry();


        // Open connection to Mattermost server
        MattermostClient client = MattermostClientFactory.openNewClient(appProps.getMmHost(), appProps.getMmToken(), allJobsRegistry.getRegisteredReflectiveJobs());

        // Run all jobs
        allJobsRegistry.runAllActiveJobs(appProps, client);
    }
}
