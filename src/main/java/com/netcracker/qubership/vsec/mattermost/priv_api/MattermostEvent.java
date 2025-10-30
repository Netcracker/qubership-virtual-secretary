package com.netcracker.qubership.vsec.mattermost.priv_api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MattermostEvent implements IMattermostEvent {
    private String event;
    private Map<String, Object> data;
    private long seq;
    private volatile AtomicReference<Optional<MattermostPost>> cachedValueRef = null;
    private Object broadcast;

    public Optional<MattermostPost> getPost()
    {
        if (cachedValueRef == null) {
            calcValueSafe();
        }

        return cachedValueRef.get();
    }

    private synchronized void calcValueSafe() {
        if (cachedValueRef != null) return;

        if (null == data) {
            cachedValueRef = new AtomicReference<>(Optional.empty());
            return;
        }

        final Object post = data.get("post");
        if (null == post) {
            cachedValueRef = new AtomicReference<>(Optional.empty());
            return;
        }

        if (post instanceof String) {
            cachedValueRef = new AtomicReference<>(Optional.of(MattermostPost.fromString((String)post)));
            return;
        }

        cachedValueRef = new AtomicReference<>(Optional.empty());
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public long getSeq() {
        return seq;
    }

    public void setSeq(long seq) {
        this.seq = seq;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Object getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(Object broadcast) {
        this.broadcast = broadcast;
    }
}

