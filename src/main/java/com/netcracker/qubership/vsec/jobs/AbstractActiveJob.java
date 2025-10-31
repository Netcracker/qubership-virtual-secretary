package com.netcracker.qubership.vsec.jobs;

import com.netcracker.qubership.vsec.model.AppProperties;
import net.bis5.mattermost.client4.MattermostClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * This type of jobs contains logic to be executed/triggered on the application side.
 */
public abstract class AbstractActiveJob implements Runnable {
    private final Logger log;
    private AppProperties appProperties;
    private MattermostClient client;
    private Connection conn;

    protected abstract void runAsync(AppProperties appProperties, MattermostClient client, Connection conn);

    public AbstractActiveJob() {
        this.log = LoggerFactory.getLogger(getClass());
    }

    public Logger getLog() {
        return log;
    }

    @Override
    public final void run() {
        runAsync(appProperties, client, conn);
    }

    public AbstractActiveJob withContext(AppProperties appProperties) {
        if (this.appProperties != null) throw new IllegalStateException("Application properties already set");

        this.appProperties = appProperties;
        return this;
    }

    public AbstractActiveJob withMattermostClient(MattermostClient client) {
        if (this.client != null) throw new IllegalStateException("Mattermost client is already set");

        this.client = client;
        return this;
    }

    public AbstractActiveJob withConnectionToDB(Connection connection) {
        if (this.conn != null) throw new IllegalStateException("Connection is already provided");

        this.conn = connection;
        return this;
    }
}
