package com.khronodragon.bluestone.voice;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.HashSet;

public class ExtraTrackInfo {
    public final MessageChannel textChannel;
    public final Member requester;
    public boolean sendNowPlaying = true;
    private HashSet<Member> skipVotes = new HashSet<>();

    ExtraTrackInfo(MessageChannel channel, Member req) {
        textChannel = channel;
        requester = req;
    }

    public int getSkipVotes() {
        return skipVotes.size();
    }

    public void addSkipVote(Member member) {
        skipVotes.add(member);
    }

    public boolean hasVotedToSkip(Member member) {
        return skipVotes.contains(member);
    }
}
