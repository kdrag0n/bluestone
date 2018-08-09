package com.kdrag0n.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.kdrag0n.bluestone.*;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.annotations.EventHandler;
import com.kdrag0n.bluestone.enums.AutoroleConditions;
import com.kdrag0n.bluestone.errors.PassException;
import com.kdrag0n.bluestone.sql.GuildAutorole;
import com.kdrag0n.bluestone.util.Strings;
import gnu.trove.list.TLongList;
import gnu.trove.list.linked.TLongLinkedList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;

public class ModerationCog extends Cog {
    private static final Logger logger = LogManager.getLogger(ModerationCog.class);
    private static final String PURGE_NO_PARAMS = Emotes.getFailure() + " **No valid parameters included!**\n" +
            "Valid parameters:\n" +
            "    \u2022 `<num 1-800>` - number of messages to include **(required)**\n" +
            "    \u2022 `links` - include messages with links\n" +
            "    \u2022 `attach` - include messages with an attachment\n" +
            "    \u2022 `embeds` - include messages with embeds\n" +
            "    \u2022 `@user` - include messages by `user`\n" +
            "    \u2022 `bots` - include messages by bots\n" +
            "    \u2022 `\"text\"` - include messages containing `text`\n" +
            "    \u2022 `[regex]` - include messages that match the regex";
    private static final String NO_COMMAND = "🤔 **I need an action!**\n" +
            "The following are valid:\n" +
            "    \u2022 `list` - list autoroles\n" +
            "    \u2022 `add [id/name/@role]` - add a role to autoroles\n" +
            "    \u2022 `remove [id/name/@role]` - remove a role from autoroles\n" +
            "    \u2022 `clear` - clear autoroles (remove all)";
    private static final String[] BYTE_UNITS = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    private static final Pattern FIRST_ID_PATTERN = Pattern.compile("^[0-9]{17,20}");
    private static final Pattern PURGE_LINK_PATTERN = Pattern.compile("https?://.+");
    private static final Pattern PURGE_QUOTE_PATTERN = Pattern.compile("[\"“](.*?)[\"”]", Pattern.DOTALL);
    private static final Pattern PURGE_REGEX_PATTERN = Pattern.compile("\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern PURGE_MENTION_PATTERN = Pattern.compile("<@!?(\\d{17,20})>");
    private static final Pattern PURGE_NUM_PATTERN = Pattern.compile("(?:^|\\s)(\\d{1,3})(?:$|\\s)");
    private static final Collection<Permission> MUTED_PERMS = Arrays.asList(Permission.MESSAGE_WRITE,
            Permission.MESSAGE_ADD_REACTION);
    private static final Pattern MENTION_PATTERN = Pattern.compile("<@!?(\\d{17,20})>");
    private static final Field embFields;
    private static final Field embDescription;
    private final TLongSet archivingGuilds = new TLongHashSet();
    private final Dao<GuildAutorole, Long> autoroleDao;

    static {
        try {
            embFields = EmbedBuilder.class.getDeclaredField("fields");
            embFields.setAccessible(true);

            embDescription = EmbedBuilder.class.getDeclaredField("description");
            embDescription.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public ModerationCog(Bot bot) {
        super(bot);

        autoroleDao = setupDao(GuildAutorole.class);
    }

    public String getName() {
        return "Moderation";
    }

    public String getDescription() {
        return "Some handy moderation tools.";
    }

    @EventHandler(threaded = true)
    public void onMemberJoin(GuildMemberJoinEvent event) throws SQLException {
        if (!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES))
            return;

        List<Role> toAdd;
        List<GuildAutorole> autoroles = autorolesFor(event.getGuild().getIdLong());
        if (autoroles.size() > 0)
            toAdd = new ArrayList<>(autoroles.size());
        else
            return;

        for (GuildAutorole autorole: autoroles) {
            Role role = event.getGuild().getRoleById(autorole.getRoleId());
            if (role == null) continue;
            if (!event.getGuild().getSelfMember().canInteract(role)) continue;

            if (AutoroleConditions.test(autorole.getConditions()))
                toAdd.add(role);
        }

        if (toAdd.size() > 0)
            event.getGuild().getController().addRolesToMember(event.getMember(), toAdd)
                    .reason("Autorole: new member matched specified conditions for role(s)")
                    .queue();
    }

    private List<GuildAutorole> autorolesFor(long guildId) throws SQLException {
        return autoroleDao.queryBuilder()
                .where()
                .eq("guildId", guildId)
                .query();
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

        return pattern.matcher(input).replaceAll(" ");
    }

    @Perm.Combo.ManageMessagesAndReadHistory
    @Command(name = "purge", desc = "Purge messages from a channel.", guildOnly = true,
            aliases = {"clean", "nuke", "prune", "clear"},
            usage = "[parameters]", thread = true)
    public void cmdPurge(Context ctx) {
        if (ctx.args.empty) {
            ctx.send(PURGE_NO_PARAMS).queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        Matcher matcher;
        String args = ctx.rawArgs;
        Pattern pattern = null;
        List<String> substrings = new LinkedList<>();
        TLongList userIds = new TLongLinkedList();
        int limit = 0;
        TextChannel channel = ctx.event.getTextChannel();

        // match all the params
        args = match(PURGE_QUOTE_PATTERN, args, m -> substrings.add(m.group(1).toLowerCase().trim()));

        matcher = PURGE_REGEX_PATTERN.matcher(args);
        if (matcher.find()) {
            try {
                pattern = Pattern.compile(matcher.group(1));
            } catch (PatternSyntaxException e) {
                ctx.fail("Invalid regex given!");
                return;
            }
        }

        args = match(PURGE_MENTION_PATTERN, args, m -> userIds.add(MiscUtil.parseSnowflake(m.group(1))));

        matcher = PURGE_NUM_PATTERN.matcher(args);
        if (matcher.find()) {
            try {
                limit = Integer.parseInt(matcher.group(1).trim());
            } catch (NumberFormatException e) {
                ctx.fail("Invalid number given for limit!");
                return;
            }
        }
        args = PURGE_NUM_PATTERN.matcher(args).replaceAll(" ").trim();

        if (limit > 800) {
            ctx.fail("Invalid message limit!");
            return;
        }
        limit += 1;

        boolean bots = args.contains("bot");
        boolean embeds = args.contains("embed");
        boolean links = args.contains("link");
        boolean attachments = args.contains("attach");
        boolean none = substrings.isEmpty() && pattern == null && userIds.isEmpty() && !bots && !embeds && !links && !attachments;

        String twoWeekWarn = "";
        OffsetDateTime maxAge = ctx.message.getCreationTime().minusWeeks(2).plusMinutes(1);
        List<Message> toDelete = new LinkedList<>();

        for (Message msg: channel.getIterableHistory()) {
            if (toDelete.size() >= limit)
                break;

            if (msg.getCreationTime().isBefore(maxAge)) {
                twoWeekWarn = "\n:vertical_traffic_light: *Some messages may not have been deleted, because they were more than 2 weeks old.*";
                break;
            }

            if (none || userIds.contains(msg.getAuthor().getIdLong()) || (bots && msg.getAuthor().isBot()) ||
                    (embeds && !msg.getEmbeds().isEmpty()) || (attachments && !msg.getAttachments().isEmpty()) ||
                    (links && PURGE_LINK_PATTERN.matcher(msg.getContentRaw()).find())) {
                toDelete.add(msg);
                continue;
            }

            if (substrings.stream()
                    .anyMatch(ss -> msg.getContentRaw().toLowerCase().contains(ss))) {
                toDelete.add(msg);
                continue;
            }

            if (pattern != null && pattern.matcher(msg.getContentRaw()).matches())
                toDelete.add(msg);
        }

        if (toDelete.isEmpty()) {
            ctx.fail("No messages match your criteria!");
            return;
        }

        if (toDelete.size() == 1) {
            try {
                toDelete.get(0).delete().reason("Purge command - deleting a single message").complete();
            } catch (ErrorResponseException ignored) {}
        } else if (toDelete.size() <= 100) {
            try {
                channel.deleteMessages(toDelete).complete();
            } catch (ErrorResponseException ignored) {}
        } else {
            for (int i = 0; i <= toDelete.size(); i += 99) {
                List<Message> list = toDelete.subList(i, Math.min(i + 99, toDelete.size()));
                if (list.isEmpty()) break;

                if (list.size() == 1)
                    try {
                        toDelete.get(0).delete().reason("Purge command - deleting a single message").complete();
                    } catch (ErrorResponseException ignored) {}
                else
                    try {
                        channel.deleteMessages(list).complete();
                    } catch (ErrorResponseException ignored) {}
            }
        }

        String k = toDelete.size() == 1 ? "" : "s";
        ctx.send(Emotes.getSuccess() + " Deleted **" + toDelete.size() +
                "** message" + k + '!' + twoWeekWarn).queue(
                        msg -> msg.delete().queueAfter(2, TimeUnit.SECONDS, null, exp -> {}));
    }

    @Perm.ManageRoles
    @Perm.ManageChannels
    @Perm.ManagePermissions
    @Command(name = "mute", desc = "Mute someone in all text channels.", guildOnly = true,
            usage = "[@user] {reason}")
    public void cmdMute(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need someone to mute!");
            return;
        } else if (!Strings.isMention(ctx.rawArgs) || ctx.message.getMentionedUsers().size() < 1) {
            ctx.fail("Invalid mention!");
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_PERMISSIONS)) {
            ctx.fail("I need the **Manage Channels** permission!");
            return;
        }

        Member user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.fail("I need a higher-level role to mute that user, and I can't mute the owner.");
            return;
        }

        ctx.send(":hourglass: Muting...").queue(status -> {
            String reason;
            String userReason = MENTION_PATTERN.matcher(ctx.rawArgs).replaceAll("").trim();

            if (userReason.length() < 1 || userReason.length() > 450)
                reason = getTag(ctx.author) + " used the mute command (with sufficient permissions)";
            else
                reason = getTag(ctx.author) + ": " + userReason;

            for (TextChannel channel: ctx.guild.getTextChannels()) {
                if (!user.hasPermission(channel, Permission.MESSAGE_WRITE))
                    continue;

                PermissionOverride override = channel.getPermissionOverride(user);
                try {
                    if (override == null)
                        channel.createPermissionOverride(user)
                                .setDeny(MUTED_PERMS)
                                .reason(reason).queue();
                    else
                        override.getManager().deny(MUTED_PERMS).reason(reason).queue();
                } catch (InsufficientPermissionException ignored) {}
            }

            status.editMessage(Emotes.getSuccess() + " Muted **" +
                    user.getUser().getName() +
                    '#' +
                    user.getUser().getDiscriminator() +
                    "**.").queue();
        });
    }

    @Perm.ManageRoles
    @Perm.ManageChannels
    @Command(name = "unmute", desc = "Unmute someone in all text channels.", guildOnly = true, usage = "[@user] {reason}")
    public void cmdUnmute(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need someone to unmute!");
            return;
        } else if (!Strings.isMention(ctx.rawArgs) || ctx.message.getMentionedUsers().size() < 1) {
            ctx.fail("Invalid mention!");
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_PERMISSIONS)) {
            ctx.fail("I need the **Manage Channels** permission!");
            return;
        }

        Member user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.fail("I need a higher-level role to unmute that user, and I can't unmute the owner.");
            return;
        }

        ctx.send(":hourglass: Unmuting...").queue(status -> {
            String reason;
            String userReason = MENTION_PATTERN.matcher(ctx.rawArgs).replaceAll("").trim();

            if (userReason.length() < 1 || userReason.length() > 450)
                reason = getTag(ctx.author) + " used the unmute command (with sufficient permissions)";
            else
                reason = getTag(ctx.author) + ": " + userReason;

            for (TextChannel channel: ctx.guild.getTextChannels()) {
                if (user.hasPermission(channel, Permission.MESSAGE_WRITE))
                    continue;

                PermissionOverride override = channel.getPermissionOverride(user);
                try {
                    if (override != null)
                        override.getManager().clear(MUTED_PERMS).reason(reason).queue();
                } catch (InsufficientPermissionException ignored) {}
            }

            status.editMessage(Emotes.getSuccess() + " Unmuted **" +
                    user.getUser().getName() +
                    '#' +
                    user.getUser().getDiscriminator() +
                    "**.").queue();
        });
    }

    @Perm.Ban
    @Command(name = "ban", desc = "Swing the ban hammer on someone.", guildOnly = true,
            usage = "[@user or user ID] {reason}")
    public void cmdBan(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need someone to ban!");
            return;
        } else if ((!MENTION_PATTERN.matcher(ctx.rawArgs).find() || ctx.message.getMentionedUsers().size() < 1) &&
                !Strings.isID(ctx.args.get(0))) {
            ctx.fail("Invalid mention or user ID!");
            return;
        }

        String reason;
        Matcher _m = MENTION_PATTERN.matcher(ctx.rawArgs);
        String _userReason = _m.replaceFirst("");
        final String userReason = _m.reset(_userReason).usePattern(FIRST_ID_PATTERN)
                .replaceFirst("").trim();
        final boolean validUreason = !(userReason.length() < 1 || userReason.length() > 450);

        if (validUreason)
            reason = getTag(ctx.author) + " used the ban command (with sufficient permissions)";
        else
            reason = getTag(ctx.author) + ": " + userReason;

        Member user;
        if (ctx.message.getMentionedUsers().size() > 0) {
            user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        } else {
            user = ctx.guild.getMemberById(ctx.args.get(0));
            if (user == null) {
                ctx.fail("I can't find that member!\n*hackbanning / banning by ID before an user ever joins is coming Soon™*");
                return;
            }
        }

        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.fail("I need a higher-level role to ban that user, and I can't ban the owner.");
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            ctx.fail("I need permission to **ban members**!");
            return;
        }

        if (user.getUser().isBot()) {
            ctx.guild.getController().ban(user, 0, reason).reason(reason).queue();
            ctx.send("🔨 Banned.").queue();
            return;
        }

        user.getUser().openPrivateChannel().queue(ch -> {
            if (validUreason)
                ch.sendMessage("You were banned from **" + ctx.guild.getName() + "** for `" + userReason + "`.").queue();
            else
                ch.sendMessage("You were banned from **" + ctx.guild.getName() + "**. No reason was specified.").queue();

            ctx.guild.getController().ban(user, 0, reason).reason(reason).queue();
            ctx.send("🔨 Banned.").queue();
        }, ignored -> {
            ctx.guild.getController().ban(user, 0, reason).reason(reason).queue();
            ctx.send("🔨 Banned.").queue();
        });
    }

    @Perm.Kick
    @Command(name = "kick", desc = "Kick a member of the server.", guildOnly = true,
            usage = "[@user or user ID] [reason]")
    public void cmdKick(Context ctx) {
        if (ctx.args.empty) {
            ctx.fail("I need someone to kick!");
            return;
        } else if ((!MENTION_PATTERN.matcher(ctx.rawArgs).find() || ctx.message.getMentionedUsers().size() < 1) &&
                !Strings.isID(ctx.args.get(0))) {
            ctx.fail("Invalid mention or user ID!");
            return;
        }

        String reason;
        Matcher _m = MENTION_PATTERN.matcher(ctx.rawArgs);
        String _userReason = _m.replaceFirst("");
        final String userReason = _m.reset(_userReason).usePattern(FIRST_ID_PATTERN)
                .replaceFirst("").trim();
        final boolean validUreason = !(userReason.length() < 1 || userReason.length() > 450);

        if (validUreason)
            reason = getTag(ctx.author) + " used the kick command (with sufficient permissions)";
        else
            reason = getTag(ctx.author) + ": " + userReason;

        Member user;
        if (ctx.message.getMentionedUsers().size() > 0) {
            user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        } else {
            user = ctx.guild.getMemberById(ctx.args.get(0));
            if (user == null) {
                ctx.fail("No such member!");
                return;
            }
        }

        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.fail("I need a higher-level role to kick that user, and I can't kick the owner.");
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
            ctx.fail("I need permission to **kick members**!");
            return;
        }

        if (user.getUser().isBot()) {
            ctx.guild.getController().kick(user, reason).reason(reason).queue();
            ctx.send("👢 Kicked.").queue();
            return;
        }

        user.getUser().openPrivateChannel().queue(ch -> {
            if (validUreason)
                ch.sendMessage("You've been kicked from **" + ctx.guild.getName() + "** for `" + userReason + "`.").queue();
            else
                ch.sendMessage("You've been kicked from **" + ctx.guild.getName() + "**. No reason was specified.").queue();

            ctx.guild.getController().kick(user, reason).reason(reason).queue();
            ctx.send("👢 Kicked.").queue();
        }, ignored -> {
            ctx.guild.getController().kick(user, reason).reason(reason).queue();
            ctx.send("👢 Kicked.").queue();
        });
    }

    @Perm.ManageRoles
    @Command(name = "autorole", desc = "Manage autoroles in this server.", guildOnly = true,
            usage = "[action] {role}", aliases = {"autoroles", "ar"}, thread = true)
    public void cmdAutorole(Context ctx) throws SQLException {
        if (ctx.args.empty) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        switch (invoked) {
            case "list":
                autoroleList(ctx);
                break;
            case "add":
                autoroleAdd(ctx);
                break;
            case "remove":
                autoroleRemove(ctx);
                break;
            case "clear":
                autoroleClear(ctx);
                break;
            default:
                ctx.send(NO_COMMAND).queue();
                break;
        }
    }

    private Role parseRole(Guild guild, String roleArg) {
        if (roleArg == null)
            return null;

        if (Strings.isRoleMention(roleArg)) {
            return guild.getRoleById(roleArg.substring(3, roleArg.length() - 1));
        } else if (Strings.isID(roleArg)) {
            return guild.getRoleById(roleArg);
        } else {
            List<Role> roles = guild.getRolesByName(roleArg, false);
            if (roles.size() < 1)
                return null;
            else
                return roles.get(0);
        }
    }

    private Role requireRole(Context ctx) {
        Role role;

        if (ctx.args.length < 2 ||
                (role = parseRole(ctx.guild, ctx.rawArgs.substring(ctx.args.get(0).length()).trim())) == null) {
            ctx.fail("I need a role in the form of the name, @role, or ID!");
            throw new PassException();
        }

        return role;
    }

    private void autoroleList(Context ctx) throws SQLException {
        Collection<GuildAutorole> autoroles = autorolesFor(ctx.guild.getIdLong());
        if (autoroles.size() < 1) {
            ctx.fail("There are no autoroles in this server!");
            return;
        }

        EmbedBuilder emb = new EmbedBuilder()
                .setAuthor("Autorole List", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                .setDescription("Here are the autoroles in this server:")
                .setColor(val(ctx.guild.getSelfMember().getColor()).or(Color.WHITE))
                .setTimestamp(Instant.now());

        for (GuildAutorole autorole: autoroles)
            emb.getDescriptionBuilder().append("\n    \u2022 <@&")
                    .append(autorole.getRoleId())
                    .append("> (ID: `")
                    .append(autorole.getRoleId())
                    .append("`)");

        ctx.send(emb.build()).queue();
    }

    private void autoroleAdd(Context ctx) throws SQLException {
        Role role = requireRole(ctx);
        if (autoroleDao.idExists(role.getIdLong())) {
            ctx.fail("Role is already an autorole!");
            return;
        } else if (role.isManaged()) {
            ctx.fail("That role is a special bot role, or is managed by an integration!");
            return;
        } else if (!ctx.guild.getSelfMember().canInteract(role)) {
            ctx.fail("I need a higher-level role to apply that role, and I can't apply the owner.");
            return;
        }

        GuildAutorole autorole = new GuildAutorole(role.getIdLong(), ctx.guild.getIdLong(), 0, "{}");
        autoroleDao.create(autorole);

        ctx.success("Role added to autoroles.");
    }

    private void autoroleRemove(Context ctx) throws SQLException {
        Role role = requireRole(ctx);
        if (!autoroleDao.idExists(role.getIdLong())) {
            ctx.fail("Role isn't an already autorole!");
            return;
        }

        autoroleDao.deleteById(role.getIdLong());

        ctx.success("Role removed from autoroles.");
    }

    private void autoroleClear(Context ctx) throws SQLException {
        DeleteBuilder builder = autoroleDao.deleteBuilder();
        builder.where()
                .eq("guildId", ctx.guild.getIdLong());
        int deleted = builder.delete();

        ctx.success("Cleared " + deleted + " autoroles.");
    }

    @Perm.Invite
    @Command(name = "instant_invite", desc = "Create an instant invite that never expires.",
            usage = "{#channel - default current channel}", guildOnly = true,
            aliases = {"inv", "make_invite", "mkinvite", "makeinvite", "createinvite", "instantinvite", "create_invite"})
    public void cmdMakeInvite(Context ctx) {
        MessageChannel channel = ctx.channel;
        if (ctx.message.getMentionedChannels().size() > 0) {
            channel = ctx.message.getMentionedChannels().get(0);
        }

        final TextChannel ch = ((TextChannel) channel);

        if (!ctx.guild.getSelfMember().hasPermission(ch, Permission.CREATE_INSTANT_INVITE)) {
            ctx.fail("I need to be able to **create instant invites**!");
            return;
        }

        ch.createInvite()
                .setUnique(false)
                .setTemporary(false)
                .setMaxAge(0)
                .setMaxUses(0)
                .queue(i -> ctx.send(Emotes.getSuccess() + " Invite created to " +
                        ch.getAsMention() + ".\n" + i.getURL()).queue(), e -> {
                    ctx.fail("Failed to create invite!");
                    logger.error("Invite creation error", e);
                });
    }
}
