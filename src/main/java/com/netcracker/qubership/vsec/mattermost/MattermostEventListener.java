package com.netcracker.qubership.vsec.mattermost;

import com.netcracker.qubership.vsec.mattermost.priv_api.IMattermostEvent;
import com.netcracker.qubership.vsec.mattermost.priv_api.MattermostEvent;
import com.netcracker.qubership.vsec.mattermost.priv_api.MattermostPost;
import net.bis5.mattermost.model.Post;

import java.util.Optional;

import static com.netcracker.qubership.vsec.mattermost.priv_api.EventType.POSTED;

public abstract class MattermostEventListener {
    public abstract void onMessage(Post post);
    public abstract void onOther(IMattermostEvent post);

    public final void onEvent(MattermostEvent event) {
        String eventType = event.getEvent();
        Optional<MattermostPost> postOpt = event.getPost();

        if (postOpt.isPresent()) {
            MattermostPost post = postOpt.get();

            if (POSTED.toString().equals(eventType)) {
                onMessage(post);
                return;
            }
        }

        onOther(event);
    }
}