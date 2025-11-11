package com.netcracker.qubership.vsec.mattermost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.qubership.vsec.ErrorCodes;
import com.netcracker.qubership.vsec.jobs.AbstractReflectiveJob;
import com.netcracker.qubership.vsec.mattermost.priv_api.MattermostEvent;
import net.bis5.mattermost.client4.MattermostClient;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Factory to create new Mattermost client instance and register listeners to receive events from mattermost instance.
 */
public class MattermostClientFactory {
    private static final Logger log = LoggerFactory.getLogger(MattermostClientFactory.class);

    public static MattermostClient openNewClient(String mmServerDomainName, String mmAccessToken, List<AbstractReflectiveJob> listeners) {
        // Connect to matter-most server
        String url = "https://" + mmServerDomainName;
        MattermostClient client = MattermostClient.builder()
                .url(url).ignoreUnknownProperties()
                .build();
        client.setAccessToken(mmAccessToken);

        // Register events-listener
        try {
            URI serverUri = new URI("wss://" + mmServerDomainName + "/api/v4/websocket");
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + mmAccessToken);

            WebSocketClient wsClient = new WebSocketClientImpl(serverUri, headers, listeners);
            wsClient.connect();

        } catch (URISyntaxException e) {
            log.error(ErrorCodes.ERR004 + ": Error while creating websocket", e);
            throw new IllegalArgumentException(e);
        }

        return client;
    }

    private static class WebSocketClientImpl extends WebSocketClient {
        final List<AbstractReflectiveJob> listeners;
        private final ObjectMapper mapper;
        private final ExecutorService executorService;

        public WebSocketClientImpl(URI serverUri, Map<String, String> httpHeaders, List<AbstractReflectiveJob> listeners) {
            super(serverUri, httpHeaders);
            this.listeners = listeners;
            this.executorService = Executors.newFixedThreadPool(10);
            this.mapper = new ObjectMapper();
        }

        @Override
        public void onOpen(ServerHandshake handShakeData) {
            log.info("Connection to Mattermost server is opened");
        }

        @Override
        public void onMessage(String messageStr) {
            try {
                MattermostEvent message = mapper.readValue(messageStr, MattermostEvent.class);
                for (MattermostEventListener next : listeners) {
                    executorService.submit(() -> next.onEvent(message));
                }
            } catch (IOException ex) {
                log.error(ErrorCodes.ERR005 + ": Error while processing message", ex);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            log.info("Connection to Mattermost server is closed: code = {}, reason = {}", code, reason);
        }

        @Override
        public void onError(Exception ex) {
            log.error(ErrorCodes.ERR006 + ": Error happened", ex);
        }
    }
}
