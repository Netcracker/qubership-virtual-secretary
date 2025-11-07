package com.netcracker.qubership.vsec.jobs.impl_refl;

import com.netcracker.qubership.vsec.jobs.AbstractReflectiveJob;
import com.netcracker.qubership.vsec.mattermost.priv_api.IMattermostEvent;
import net.bis5.mattermost.model.Post;

public class TestReflJob1 extends AbstractReflectiveJob {
    @Override
    public void onMessage(Post post) {
        // implement your logic here to process messages sent to Bot
    }

    @Override
    public void onOther(IMattermostEvent post) {
        // implement your logic here to process events sent to Bot
    }
}
