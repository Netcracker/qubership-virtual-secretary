package com.netcracker.qubership.vsec.jobs.impl_act;

import com.netcracker.qubership.vsec.jobs.AbstractActiveJob;
import com.netcracker.qubership.vsec.model.AppProperties;
import com.netcracker.qubership.vsec.utils.MiscUtils;
import net.bis5.mattermost.client4.MattermostClient;

public class CheckAllUsersInMattermostHaveCorrectEmails extends AbstractActiveJob {
    @Override
    protected void runAsync(AppProperties appProperties, MattermostClient client) {
        getLog().info("Start thread {}", appProperties.getMmHost());
        MiscUtils.sleep(500);
        System.out.println("End thread");
    }
}
