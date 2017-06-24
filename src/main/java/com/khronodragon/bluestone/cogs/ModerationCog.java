package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.restaction.pagination.MessagePaginationAction;
import net.dv8tion.jda.core.utils.MiscUtil;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ModerationCog extends Cog {
    private static final String PURGE_NO_PARAMS = ":warning: **No valid parameters included!**\n" +
            "Valid parameters:\n" +
            "    \u2022 `<num 2-500>` - number of messages to include **(required)**\n" +
            "    \u2022 `links` - include messages with links\n" +
            "    \u2022 `attach` - include messages with an attachment\n" +
            "    \u2022 `embeds` - include messages with embeds\n" +
            "    \u2022 `@user` - include messages by `user`\n" +
            "    \u2022 `bots` - include messages by bots\n" +
            "    \u2022 `\"text\"` - include messages containing `text`\n" +
            "    \u2022 `[regex]` - include messages that match the regex";
    private static final Pattern PURGE_LINK_PATTERN = Pattern.compile("https?://.+");
    private static final Pattern PURGE_QUOTE_PATTERN = Pattern.compile("\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern PURGE_REGEX_PATTERN = Pattern.compile("\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern PURGE_MENTION_PATTERN = Pattern.compile("<@!?(\\d{17,20})>");
    private static final Pattern PURGE_NUM_PATTERN = Pattern.compile("(?:^|\\s)(\\d{1,3})(?:$|\\s)");
    private static final Collection<Permission> MUTED_PERMS = Collections.unmodifiableCollection(
            Arrays.asList(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION));
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@!?(\\d{17,20})>");

    public ModerationCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Moderation";
    }

    public String getDescription() {
        return "Some handy moderation tools.";
    }

    private String match(Pattern pattern, String input, Consumer<Matcher> func) {
        return match(pattern, input, func, true);
    }

    private String match(Pattern pattern, String input, Consumer<Matcher> func, boolean iterate) {
        Matcher matcher = pattern.matcher(input);

        if (iterate) {
            while (matcher.find())
                func.accept(matcher);
        } else {
            if (matcher.find())
                func.accept(matcher);
        }

        return input.replaceAll(pattern.pattern(), " ");
    }

    @Command(name = "purge", desc = "Purge messages from a channel.", guildOnly = true,
            aliases = {"clean", "nuke"}, perms = {"messageManage", "messageHistory"},
            usage = "[parameters]", thread = true)
    public void cmdPurge(Context ctx) {
        if (bot.isSelfbot()) {
            ctx.send(":x: Discord doesn't allow selfbots to purge.").queue();
            return;
        }
        if (ctx.rawArgs.length() < 1) {
            ctx.send(PURGE_NO_PARAMS).queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        Matcher matcher;
        String args = ctx.rawArgs;
        String regex = null;
        List<String> substrings = new LinkedList<>();
        List<Long> userIds = new LinkedList<>();
        int limit = 0;
        TextChannel channel = ctx.event.getTextChannel();

        // match all the params
        args = match(PURGE_QUOTE_PATTERN, args, m -> {
            substrings.add(m.group().toLowerCase().trim());
        });

        matcher = PURGE_REGEX_PATTERN.matcher(args);
        if (matcher.find())
            regex = matcher.group();

        args = match(PURGE_MENTION_PATTERN, args, m -> {
            userIds.add(MiscUtil.parseSnowflake(m.group()));
        });

        matcher = PURGE_NUM_PATTERN.matcher(args);
        if (matcher.find()) {
            try {
                limit = Integer.parseInt(matcher.group().trim());
            } catch (NumberFormatException e) {
                ctx.send(":x: Invalid number given for limit!").queue();
                return;
            }
        }
        args = args.replaceAll(PURGE_NUM_PATTERN.pattern(), " ");

        if (limit > 500 || limit < 2) {
            ctx.send(":x: Invalid message limit!").queue();
            return;
        }

        boolean bots = args.contains("bot");
        boolean embeds = args.contains("embed");
        boolean links = args.contains("link");
        boolean attachments = args.contains("attach");
        boolean none = substrings.isEmpty() && regex == null && userIds.isEmpty() && !bots && !embeds && !links && !attachments;

        String twoWeekWarn = "";
        OffsetDateTime maxAge = ctx.message.getCreationTime().minusWeeks(2).plusMinutes(1);
        List<Message> toDelete = new LinkedList<>();

        for (Message msg: channel.getIterableHistory()) {
            if (toDelete.size() >= limit)
                break;

            if (msg.getIdLong() == ctx.message.getIdLong())
                continue;

            if (msg.getCreationTime().isBefore(maxAge)) {
                twoWeekWarn = "\n:vertical_traffic_light: *Some messages weren't deleted, because they were more than 2 weeks old.*";
                break;
            }

            if (none || userIds.contains(msg.getAuthor().getIdLong()) || (bots && msg.getAuthor().isBot()) ||
                    (embeds && !msg.getEmbeds().isEmpty()) || (attachments && !msg.getAttachments().isEmpty()) ||
                    (links && PURGE_LINK_PATTERN.matcher(msg.getRawContent()).find())) {
                toDelete.add(msg);
                continue;
            }

            if (substrings.stream()
                    .anyMatch(ss -> msg.getRawContent().contains(ss))) {
                toDelete.add(msg);
                continue;
            }

            try {
                if (regex != null && msg.getRawContent().matches(regex))
                    toDelete.add(msg);
            } catch (PatternSyntaxException e) {
                ctx.send(":x: Invalid regex given!").queue();
                return;
            }
        }

        if (toDelete.isEmpty()) {
            ctx.send(":warning: No messages match your criteria!").queue();
            return;
        } else if (toDelete.size() < 2) {
            ctx.send(":warning: Not enough messages match your criteria!").queue();
            return;
        }

        if (toDelete.size() <= 100) {
            channel.deleteMessages(toDelete).complete();
        } else {
            for (int i = 0; i <= toDelete.size(); i += 100) {
                channel.deleteMessages(toDelete.subList(i, Math.min(i + 100, toDelete.size()))).complete();
            }
        }

        ctx.send(":white_check_mark: Deleted **" + toDelete.size() +
                "** messages!" + twoWeekWarn).queue(msg -> {
            msg.delete().queueAfter(2, TimeUnit.SECONDS);
            ctx.message.addReaction("\uD83D\uDC4D").queue();
        });
    }

    @Command(name = "mute", desc = "Mute someone in all text channels.", guildOnly = true,
            perms = {"manageRoles", "manageChannel"},
            thread = true, usage = "[@user] {reason}")
    public void cmdMute(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need someone to mute!").queue();
            return;
        } else if (!ctx.rawArgs.matches("^<@!?(\\d{17,20})>$")) {
            ctx.send(":warning: Invalid mention!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            ctx.send(":x: I don't have permission to **manage channels**!").queue();
            return;
        }

        Member user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        Message status = ctx.send(":hourglass: Muting...").complete();
        String reason;
        String userReason = ctx.rawArgs.replaceAll(MENTION_PATTERN.pattern(), "").trim();
        if (userReason.length() < 1 || userReason.length() > 450)
            reason = getTag(ctx.author) + " used the mute command (with sufficient permissions)";
        else
            reason = getTag(ctx.author) + ": " + userReason;

        for (TextChannel channel: ctx.guild.getTextChannels()) {
            if (!user.hasPermission(channel, Permission.MESSAGE_WRITE))
                continue;

            PermissionOverride override = channel.getPermissionOverride(user);
            if (override == null)
                channel.createPermissionOverride(user)
                        .setDeny(MUTED_PERMS)
                        .reason(reason).complete();
            else
                override.getManager().deny(MUTED_PERMS).reason(reason).complete();
        }

        status.editMessage(new StringBuilder(":thumbsup: Muted **")
                            .append(user.getUser().getName())
                            .append('#')
                            .append(user.getUser().getDiscriminator())
                            .append("**.")
                            .toString()).queue();
    }

    @Command(name = "unmute", desc = "Unmute someone in all text channels.", guildOnly = true,
            perms = {"manageRoles", "manageChannel"},
            thread = true, usage = "[@user] {reason}")
    public void cmdUnmute(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need someone to unmute!").queue();
            return;
        } else if (!ctx.rawArgs.matches("^<@!?(\\d{17,20})>$")) {
            ctx.send(":warning: Invalid mention!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            ctx.send(":x: I don't have permission to **manage channels**!").queue();
            return;
        }

        Member user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        Message status = ctx.send(":hourglass: Unmuting...").complete();
        String reason;
        String userReason = ctx.rawArgs.replaceAll(MENTION_PATTERN.pattern(), "").trim();
        if (userReason.length() < 1 || userReason.length() > 450)
            reason = getTag(ctx.author) + " used the unmute command (with sufficient permissions)";
        else
            reason = getTag(ctx.author) + ": " + userReason;

        for (TextChannel channel: ctx.guild.getTextChannels()) {
            if (user.hasPermission(channel, Permission.MESSAGE_WRITE))
                continue;

            PermissionOverride override = channel.getPermissionOverride(user);
            if (override == null)
                continue;
            else
                override.getManager().grant(MUTED_PERMS).reason(reason).complete();
        }

        status.editMessage(new StringBuilder(":thumbsup: Unmuted **")
                .append(user.getUser().getName())
                .append('#')
                .append(user.getUser().getDiscriminator())
                .append("**.")
                .toString()).queue();
    }

    @Command(name = "ban", desc = "Swing the ban hammer on someone.", guildOnly = true,
            perms = {"banMembers"}, usage = "[@user] {reason}")
    public void cmdBan(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need someone to ban!").queue();
            return;
        } else if (!MENTION_PATTERN.matcher(ctx.rawArgs).find()) {
            ctx.send(":warning: Invalid mention!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            ctx.send(":x: I don't have permission to **ban members**!").queue();
            return;
        }
        String reason;
        String userReason = ctx.rawArgs.replaceAll(MENTION_PATTERN.pattern(), "").trim();
        if (userReason.length() < 1 || userReason.length() > 450)
            reason = getTag(ctx.author) + " used the ban command (with sufficient permissions)";
        else
            reason = getTag(ctx.author) + ": " + userReason;

        Member user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        try {
            ctx.guild.getController().ban(user, 0).reason(reason).queue();
        } catch (PermissionException e) {
            ctx.send(":warning: Error: `" + e.getMessage() + "`").queue();
            return;
        }

        ctx.send(":thumbsup: Banned.").queue();
    }
}
