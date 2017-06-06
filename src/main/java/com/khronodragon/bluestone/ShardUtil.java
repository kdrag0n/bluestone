package com.khronodragon.bluestone;

import net.dv8tion.jda.core.JDA;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ShardUtil {
    private Map<Integer, Bot> shards = new HashMap<>();
    private int shardCount;

    ShardUtil(int shardCount) {
        this.shardCount = shardCount;
    }

    public int getShardCount() {
        return shardCount;
    }

    public Bot getShard(int shardId) {
        return shards.get(shardId);
    }

    public void setShard(int shardId, Bot bot) {
        shards.put(shardId, bot);
    }

    public Set<Bot> getShards() {
        return new HashSet<>(shards.values());
    }

    public int getGuildCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getGuilds().size()).sum();
    }

    public int getChannelCount() {
        return shards.values().stream().mapToInt(b -> {
            JDA jda = b.getJda();
            return jda.getTextChannels().size() + jda.getVoiceChannels().size();
        }).sum();
    }

    public int getUserCount() {
        return shards.values().stream().mapToInt(b -> b.getJda().getUsers().size()).sum();
    }

    public int getRequestCount() {
        return shards.values().stream().mapToInt(b -> b.commandCalls.values().stream().mapToInt(AtomicInteger::get).sum()).sum();
    }
}
