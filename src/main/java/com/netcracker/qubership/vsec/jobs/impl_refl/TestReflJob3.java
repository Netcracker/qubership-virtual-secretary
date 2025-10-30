package com.netcracker.qubership.vsec.jobs.impl_refl;

import com.netcracker.qubership.vsec.jobs.AbstractReflectiveJob;
import com.netcracker.qubership.vsec.mattermost.priv_api.IMattermostEvent;
import net.bis5.mattermost.model.Post;

public class TestReflJob3 extends AbstractReflectiveJob {
    @Override
    public void onMessage(Post post) {
        System.out.println("post3");
    }

    @Override
    public void onOther(IMattermostEvent post) {
        System.out.println("onOther3");
    }
}
