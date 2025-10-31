package com.netcracker.qubership.vsec.jobs;

import com.netcracker.qubership.vsec.jobs.impl_act.AutoTerminateApp;
import com.netcracker.qubership.vsec.jobs.impl_act.CheckAllUsersInMattermostHaveCorrectEmails;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.WeeklyReportAnalyzer;
import com.netcracker.qubership.vsec.jobs.impl_refl.TestReflJob1;
import com.netcracker.qubership.vsec.jobs.impl_refl.TestReflJob2;
import com.netcracker.qubership.vsec.jobs.impl_refl.TestReflJob3;
import com.netcracker.qubership.vsec.model.AppProperties;
import net.bis5.mattermost.client4.MattermostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * All jobs (active and reflective) must be registered in this registry.
 */
public class AllJobsRegistry {
    private static final long TIMEOUT_VALUE = 1;
    private static final TimeUnit TIMEOUT_UNIT  = TimeUnit.HOURS;

    private static final Logger log = LoggerFactory.getLogger(AllJobsRegistry.class);
    private final List<AbstractActiveJob> registeredActiveJobs = new ArrayList<>();
    private final List<AbstractReflectiveJob> registeredReflectiveJobs = new ArrayList<>();

    public AllJobsRegistry() {
        registeredActiveJobs.add(new CheckAllUsersInMattermostHaveCorrectEmails());
        registeredActiveJobs.add(new WeeklyReportAnalyzer());
        registeredActiveJobs.add(new AutoTerminateApp());

        registeredReflectiveJobs.add(new TestReflJob1());
        registeredReflectiveJobs.add(new TestReflJob2());
        registeredReflectiveJobs.add(new TestReflJob3());
    }

    public List<AbstractReflectiveJob> getRegisteredReflectiveJobs() {
        return registeredReflectiveJobs;
    }

    public void runAllActiveJobs(AppProperties appProperties, MattermostClient client, Connection connection) {
        try (ExecutorService executor = Executors.newFixedThreadPool(registeredActiveJobs.size())) {

            for (AbstractActiveJob aJob : registeredActiveJobs) {
                executor.execute(
                        aJob.withContext(appProperties).withMattermostClient(client).withConnectionToDB(connection)
                );
            }
            executor.shutdown();

            boolean isGraceShutdown = executor.awaitTermination(TIMEOUT_VALUE, TIMEOUT_UNIT);
            log.info("All jobs were {}", isGraceShutdown ? "completed normally" : "terminated by timeout");
        } catch (InterruptedException ex) {
            log.error("Error while jobs executing", ex);
        }
    }
}
