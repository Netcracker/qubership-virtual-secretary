package com.netcracker.qubership.vsec.jobs;

import com.netcracker.qubership.vsec.mattermost.MatterMostClientHelper;
import com.netcracker.qubership.vsec.model.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * This type of jobs contains logic to be executed/triggered on the application side.
 */
public abstract class AbstractActiveJob implements Runnable {
    private final Logger log;
    private AppProperties appProperties;
    private MatterMostClientHelper mmHelper;
    private Connection conn;

    protected abstract void runAsync(AppProperties appProperties, MatterMostClientHelper mmHelper, Connection conn);

    public AbstractActiveJob() {
        this.log = LoggerFactory.getLogger(getClass());
    }

    public Logger getLog() {
        return log;
    }

    @Override
    public final void run() {
        runAsync(appProperties, mmHelper, conn);
    }

    public AbstractActiveJob withContext(AppProperties appProperties) {
        if (this.appProperties != null) throw new IllegalStateException("Application properties already set");

        this.appProperties = appProperties;
        return this;
    }

    public AbstractActiveJob withMattermostClient(MatterMostClientHelper mmHelper) {
        if (this.mmHelper != null) throw new IllegalStateException("Mattermost helper is already set");

        this.mmHelper = mmHelper;
        return this;
    }

    public AbstractActiveJob withConnectionToDB(Connection connection) {
        if (this.conn != null) throw new IllegalStateException("Connection is already provided");

        this.conn = connection;
        return this;
    }
}
