package com.netcracker.qubership.vsec.mattermost.priv_api;

import java.util.Map;
import java.util.Optional;

public interface IMattermostEvent {
    Optional<MattermostPost> getPost();

    Map<String, Object> getData();
}