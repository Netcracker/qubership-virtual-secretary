package com.netcracker.qubership.vsec.mattermost;

import com.netcracker.qubership.vsec.mattermost.priv_api.MattermostPost;
import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.client4.MattermostClient;
import net.bis5.mattermost.client4.Pager;
import net.bis5.mattermost.model.Channel;
import net.bis5.mattermost.model.Post;
import net.bis5.mattermost.model.User;
import net.bis5.mattermost.model.UserList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides auxiliary methods with caching mechanisms to simplify and optimize calls to mattermost
 */
public class MatterMostClientHelper {
    /* caches can be unified - in case all IDs are unique - but currently not sure about that */
    private final Map<String, String> usersCache = new ConcurrentHashMap<>(); // cache for userId -> userEmail
    private final Map<String, String> channelsCache = new ConcurrentHashMap<>(); // cache for channelId -> channel name
    private final Map<String, String> channelsWithBotCache = new ConcurrentHashMap<>(); // cache for bot_id + user_id -> channelId

    private final MattermostClient mmClient;
    private final ReentrantLock lock = new ReentrantLock();
    private User botProfile;

    public MatterMostClientHelper(MattermostClient mmClient) {
        if (mmClient == null) throw new NullPointerException("Instance of MatterMost client is null");

        this.mmClient = mmClient;
    }

    public MattermostClient getClient() {
        return mmClient;
    }

    private void ensureBotProfile() {
        if (botProfile == null) {
            lock.lock();
            try {
                if (botProfile == null) {
                    ApiResponse<User> meApiResponse = mmClient.getMe();
                    botProfile = meApiResponse.readEntity();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public User getBotProfile() {
        ensureBotProfile();
        return botProfile;
    }

    public User getUserByEmail(String email) {
        ApiResponse<User> response = mmClient.getUserByEmail(email);
        return response.readEntity();
    }

    public String defineChannelId(User user1, User user2) {
        final String cacheKey = user1.getId() + "_" + user2.getId();

        // check in the cache
        String cachedChannelId = channelsWithBotCache.get(cacheKey);
        if (cachedChannelId != null) return cachedChannelId;

        // request from MM
        ApiResponse<Channel> channelApiResponse = mmClient.createDirectChannel(user1.getId(), user2.getId());
        Channel channel = channelApiResponse.readEntity();

        channelsWithBotCache.put(cacheKey, channel.getId());
        return channel.getId();
    }

    public void sendMessage(String msg, String channelId) {
        Post post = new MattermostPost();
        post.setMessage(msg);
        post.setChannelId(channelId);
        mmClient.createPost(post);
    }

    public void sendMessage(String msg, User toUser) {
        String channelId = defineChannelId(getBotProfile(), toUser);
        sendMessage(msg, channelId);
    }

    public List<User> getAllUsers() {
        List<User> allUsers = new ArrayList<>();

        int count = 1024; // number of requests to be sent to the MM-server
        int page = 0;

        while (count-- > 0) {
            Pager pager = Pager.of(page++, 50);
            ApiResponse<UserList> response = mmClient.getUsers(pager);

            UserList tmpList = response.readEntity();
            if (tmpList.isEmpty()) break;

            allUsers.addAll(tmpList);
        }

        return allUsers;
    }
}
