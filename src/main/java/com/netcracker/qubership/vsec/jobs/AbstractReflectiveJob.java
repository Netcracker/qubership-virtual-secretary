package com.netcracker.qubership.vsec.jobs;

import com.netcracker.qubership.vsec.mattermost.MattermostEventListener;
import com.netcracker.qubership.vsec.mattermost.priv_api.IMattermostEvent;
import net.bis5.mattermost.model.Post;

/**
 * This type of job contains logic to be executed only when corresponding event will be received.
 * The same copy of event will be shared among all registered jobs.
 */
public abstract class AbstractReflectiveJob extends MattermostEventListener {
    public abstract void onMessage(Post post);
    public abstract void onOther(IMattermostEvent post);
}
