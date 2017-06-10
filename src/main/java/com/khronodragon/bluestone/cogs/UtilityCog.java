package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.ShardUtil;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.commons.lang3.text.WordUtils;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;
import static com.khronodragon.bluestone.util.Strings.str;

import java.util.HashSet;

public class UtilityCog extends Cog {
    public UtilityCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Utility";
    }
    public String getDescription() {
        return "Essential utility commands, as well as playful ones.";
    }

    @Command(name = "icon", desc = "Get the current guild's icon.", guildOnly = true)
    public void cmdIcon(Context ctx) {
        ctx.send(val(ctx.guild.getIconUrl()).or("There's no icon here!")).queue();
    }

    @Command(name = "user", desc = "Get some info about a user.", aliases = {"userinfo", "whois"})
    public void cmdUser(Context ctx) { // TODO: parseUser
        ctx.send("java is terrible so this command is impossible to implement").queue();
    }

    @Command(name = "guildinfo", desc = "Get loads of info about this guild.", guildOnly = true, aliases = {"ginfo", "guild", "server", "serverinfo", "sinfo"})
    public void cmdGuildInfo(Context ctx) {
        EmbedBuilder emb = new EmbedBuilder()
                .setColor(randomColor())
                .setAuthor(ctx.guild.getName(), null,
                        val(ctx.guild.getIconUrl()).or(ctx.jda.getSelfUser().getEffectiveAvatarUrl()))
                .setFooter(ctx.guild.getSelfMember().getEffectiveName(), ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .addField("ID", ctx.guild.getId(), true)
                .addField("Members", str(ctx.guild.getMembers().size()), true)
                .addField("Channels", str(ctx.guild.getTextChannels().size() + ctx.guild.getVoiceChannels().size()), true)
                .addField("Roles", str(ctx.guild.getRoles().size()) ,true)
                .addField("Emotes", str(ctx.guild.getEmotes().size()), true)
                .addField("Region", ctx.guild.getRegion().getName(), true)
                .addField("Owner", ctx.guild.getOwner().getAsMention(), true)
                .addField("Default Channel", ctx.guild.getPublicChannel().getAsMention(), true)
                .addField("Admins Need 2FA?", ctx.guild.getRequiredMFALevel().getKey() == 1 ? "Yes" : "No", true)
                .addField("Content Scan Level", ctx.guild.getExplicitContentLevel().getDescription(), true)
                .addField("Verification Level", WordUtils.capitalize(ctx.guild.getVerificationLevel().name().toLowerCase()
                        .replace('_', ' ')), true)
                .setThumbnail(ctx.guild.getIconUrl());

        ctx.send(emb.build()).queue();
    }

    @Command(name = "info", desc = "Get some info about me.", aliases = {"about", "stats", "statistics"})
    public void cmdInfo(Context ctx) {
        Runtime runtime = Runtime.getRuntime();
        ShardUtil shardUtil = bot.getShardUtil();
        EmbedBuilder emb = newEmbedWithAuthor(ctx, "https://khronodragon.com/")
                .setColor(randomColor())
                .setDescription("Made by **Dragon5232#1841**")
                .addField("Guilds", str(shardUtil.getGuildCount()), true)
                .addField("Uptime", bot.formatUptime(), true)
                .addField("Requests", str(shardUtil.getRequestCount()), true)
                .addField("Threads", str(Thread.activeCount()), true)
                .addField("Memory Used", bot.formatMemory(), true)
                .addField("Users", str(shardUtil.getUserCount()), true)
                .addField("Channels", str(shardUtil.getChannelCount()), true)
                .addField("Commands", str(new HashSet<>(bot.commands.values()).size()), true);

        if (ctx.jda.getSelfUser().getIdLong() == 239775420470394897L) {
            emb.addField("Invite Link", "https://tiny.cc/goldbot", true);
        }

        ctx.send(emb.build()).queue();
    }
}
