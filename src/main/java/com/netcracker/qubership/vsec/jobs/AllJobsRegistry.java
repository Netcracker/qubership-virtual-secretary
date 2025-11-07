package com.netcracker.qubership.vsec.jobs;

import com.netcracker.qubership.vsec.jobs.impl_act.CheckAllUsersInMattermostHaveCorrectEmails;
import com.netcracker.qubership.vsec.jobs.impl_act.weekly_reports.WeeklyReportAnalyzer;
import com.netcracker.qubership.vsec.jobs.impl_refl.TestReflJob1;
import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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


        registeredReflectiveJobs.add(new TestReflJob1());
    }

    public List<AbstractReflectiveJob> getRegisteredReflectiveJobs() {
        return registeredReflectiveJobs;
    }

    public void runAllActiveJobs(AppProperties appProperties, MatterMostClientHelper client, Connection connection) {
        final CountDownLatch countDownLatch = new CountDownLatch(registeredActiveJobs.size());

        try (ExecutorService executor = Executors.newFixedThreadPool(registeredActiveJobs.size())) {

            for (AbstractActiveJob aJob : registeredActiveJobs) {
                executor.execute(aJob
                        .withContext(appProperties)
                        .withMattermostClient(client)
                        .withConnectionToDB(connection)
                        .notifyingCountDownLatchAfterCompletion(countDownLatch)
                );
            }

            countDownLatch.await();
            executor.shutdown();
        } catch (InterruptedException ex) {
            log.error("Error while jobs executing", ex);
        }
    }
}
