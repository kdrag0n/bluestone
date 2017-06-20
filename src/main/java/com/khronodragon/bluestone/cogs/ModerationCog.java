package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.restaction.pagination.MessagePaginationAction;
import net.dv8tion.jda.core.utils.MiscUtil;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
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
    private final Pattern PURGE_LINK_PATTERN = Pattern.compile("https?://.+");
    private final Pattern PURGE_QUOTE_PATTERN = Pattern.compile("\"(.*?)\"", Pattern.DOTALL);
    private final Pattern PURGE_REGEX_PATTERN = Pattern.compile("\\[(.*?)]", Pattern.DOTALL);
    private final Pattern PURGE_MENTION_PATTERN = Pattern.compile("<@!?(\\d{17,20})>");
    private final Pattern PURGE_NUM_PATTERN = Pattern.compile("(?:^|\\s)(\\d{1,3})(?:$|\\s)");

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
        if (matcher.find())
            limit = Integer.parseInt(matcher.group());
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

        OffsetDateTime maxAge = ctx.message.getCreationTime().minusWeeks(2).plusMinutes(1);
        List<Message> toDelete = new LinkedList<>();

        for (Message msg: channel.getIterableHistory()) {
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
        toDelete.remove(ctx.message);

        if (toDelete.isEmpty()) {
            ctx.send(":warning: No messages match your criteria!").queue();
            return;
        }

        if (toDelete.size() <= 100) {
            channel.deleteMessages(toDelete).complete();
        } else {
            for (int i = 0; i <= toDelete.size(); i += 100) {
                channel.deleteMessages(toDelete.subList(i, i + 100)).complete();
            }
        }

        ctx.send(":white_check_mark: Deleted **" + toDelete.size() + "** messages!").queue();
    }

    @Command(name = "mute", desc = "Mute someone, on voice and text chat.", guildOnly = true,
            perms = {"voiceMuteOthers", "manageRoles", "manageChannel", "messageManage"})
    public void cmdMute(Context ctx) {

    }

    @Command(name = "unmute", desc = "Unmute someone, on voice and text chat.", guildOnly = true,
            perms = {"voiceMuteOthers", "manageRoles", "manageChannel", "messageManage"})
    public void cmdUnmute(Context ctx) {

    }
}
