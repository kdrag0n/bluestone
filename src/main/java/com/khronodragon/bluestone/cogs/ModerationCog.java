package com.khronodragon.bluestone.cogs;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.TableUtils;
import com.khronodragon.bluestone.*;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.Cooldown;
import com.khronodragon.bluestone.annotations.EventHandler;
import com.khronodragon.bluestone.enums.AutoroleConditions;
import com.khronodragon.bluestone.enums.BucketType;
import com.khronodragon.bluestone.errors.PassException;
import com.khronodragon.bluestone.sql.GuildAutorole;
import com.khronodragon.bluestone.util.Paginator;
import com.khronodragon.bluestone.util.Strings;
import gnu.trove.list.TLongList;
import gnu.trove.list.linked.TLongLinkedList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import net.dv8tion.jda.core.utils.Checks;
import net.dv8tion.jda.core.utils.MiscUtil;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessage;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

import javax.annotation.CheckReturnValue;
import java.awt.*;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.khronodragon.bluestone.util.NullValueWrapper.val;

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
    private Dao<GuildAutorole, Long> autoroleDao;

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

        try {
            TableUtils.createTableIfNotExists(bot.getShardUtil().getDatabase(), GuildAutorole.class);
        } catch (SQLException e) {
            logger.error("Failed to create autorole table!", e);
        }

        try {
            autoroleDao = DaoManager.createDao(bot.getShardUtil().getDatabase(), GuildAutorole.class);
        } catch (SQLException e) {
            logger.error("Failed to create autorole DAO!", e);
        }
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

        List<Role> toAdd = null;
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
        if (bot.isSelfbot()) {
            ctx.send(Emotes.getFailure() + " Discord doesn't allow selfbots to purge.").queue();
            return;
        }
        if (ctx.rawArgs.length() < 1) {
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
        args = match(PURGE_QUOTE_PATTERN, args, m -> {
            substrings.add(m.group(1).toLowerCase().trim());
        });

        matcher = PURGE_REGEX_PATTERN.matcher(args);
        if (matcher.find()) {
            try {
                pattern = Pattern.compile(matcher.group(1));
            } catch (PatternSyntaxException e) {
                ctx.send(Emotes.getFailure() + " Invalid regex given!").queue();
                return;
            }
        }

        args = match(PURGE_MENTION_PATTERN, args, m -> {
            userIds.add(MiscUtil.parseSnowflake(m.group(1)));
        });

        matcher = PURGE_NUM_PATTERN.matcher(args);
        if (matcher.find()) {
            try {
                limit = Integer.parseInt(matcher.group(1).trim());
            } catch (NumberFormatException e) {
                ctx.send(Emotes.getFailure() + " Invalid number given for limit!").queue();
                return;
            }
        }
        args = PURGE_NUM_PATTERN.matcher(args).replaceAll(" ").trim();

        if (limit > 800) {
            ctx.send(Emotes.getFailure() + " Invalid message limit!").queue();
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
            ctx.send(Emotes.getFailure() + " No messages match your criteria!").queue();
            return;
        }

        if (toDelete.size() == 1) {
            toDelete.get(0).delete().reason("Purge command - deleting a single message").complete();
        } else if (toDelete.size() <= 100) {
            channel.deleteMessages(toDelete).complete();
        } else {
            for (int i = 0; i <= toDelete.size(); i += 99) {
                List<Message> list = toDelete.subList(i, Math.min(i + 99, toDelete.size()));
                if (list.isEmpty()) break;

                if (list.size() == 1)
                    toDelete.get(0).delete().reason("Purge command - deleting a single message").complete();
                else
                    channel.deleteMessages(list).complete();
            }
        }

        ctx.send(Emotes.getSuccess() + " Deleted **" + toDelete.size() +
                "** messages!" + twoWeekWarn).queue(msg -> {
            msg.delete().queueAfter(2, TimeUnit.SECONDS, null, exp -> {
                if (exp instanceof ErrorResponseException) {
                    if (((ErrorResponseException) exp).getErrorCode() != 10008) {
                        RestAction.DEFAULT_FAILURE.accept(exp);
                    }
                }
            });
        });
    }

    @Perm.ManageRoles
    @Perm.ManageChannels
    @Command(name = "mute", desc = "Mute someone in all text channels.", guildOnly = true,
            usage = "[@user] {reason}")
    public void cmdMute(Context ctx) {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need someone to mute!").queue();
            return;
        } else if (!Strings.isMention(ctx.rawArgs) || ctx.message.getMentionedUsers().size() < 1) {
            ctx.send(Emotes.getFailure() + " Invalid mention!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_PERMISSIONS)) {
            ctx.send(Emotes.getFailure() + " I need the **Manage Channels** permission!").queue();
            return;
        }

        Member user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.send(Emotes.getFailure() + " I need to be higher on the role ladder to mute that user!").queue();
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
                if (override == null)
                    channel.createPermissionOverride(user)
                            .setDeny(MUTED_PERMS)
                            .reason(reason).queue();
                else
                    override.getManager().deny(MUTED_PERMS).reason(reason).queue();
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
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need someone to unmute!").queue();
            return;
        } else if (!Strings.isMention(ctx.rawArgs) || ctx.message.getMentionedUsers().size() < 1) {
            ctx.send(Emotes.getFailure() + " Invalid mention!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.MANAGE_PERMISSIONS)) {
            ctx.send(Emotes.getFailure() + " I need the **Manage Channels** permission!").queue();
            return;
        }

        Member user = ctx.guild.getMember(ctx.message.getMentionedUsers().get(0));
        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.send(Emotes.getFailure() + " I need to be higher on the role ladder to unmute that user!").queue();
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
                if (override != null)
                    override.getManager().clear(MUTED_PERMS).reason(reason).queue();
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
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need someone to ban!").queue();
            return;
        } else if ((!MENTION_PATTERN.matcher(ctx.rawArgs).find() || ctx.message.getMentionedUsers().size() < 1) &&
                !Strings.isID(ctx.args.get(0))) {
            ctx.send(Emotes.getFailure() + " Invalid mention or user ID!").queue();
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
                ctx.send(Emotes.getFailure() + " I can't find that member!\n*hackbanning / banning by ID before an user ever joins is coming Soon™*").queue();
                return;
            }
        }

        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.send(Emotes.getFailure() + " I need to be higher on the role ladder to ban that user!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
            ctx.send(Emotes.getFailure() + " I need permission to **ban members**!").queue();
            return;
        }

        if (user.getUser().isBot()) {
            ctx.guild.getController().ban(user, 0, reason).reason(reason).queue();
            ctx.send("🔨 Banned.").queue();
            return;
        }

        user.getUser().openPrivateChannel().queue(ch -> {
            if (validUreason)
                ch.sendMessage("You've been banned from **" + ctx.guild.getName() + "** for `" + userReason + "`.").queue();
            else
                ch.sendMessage("You've been banned from **" + ctx.guild.getName() + "**. No reason was specified.").queue();

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
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + " I need someone to kick!").queue();
            return;
        } else if ((!MENTION_PATTERN.matcher(ctx.rawArgs).find() || ctx.message.getMentionedUsers().size() < 1) &&
                !Strings.isID(ctx.args.get(0))) {
            ctx.send(Emotes.getFailure() + " Invalid mention or user ID!").queue();
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
                ctx.send(Emotes.getFailure() + " No such member!").queue();
                return;
            }
        }

        if (!ctx.guild.getSelfMember().canInteract(user)) {
            ctx.send(Emotes.getFailure() + " I need to be higher on the role ladder to kick that user!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
            ctx.send(Emotes.getFailure() + " I need permission to **kick members**!").queue();
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
        if (ctx.rawArgs.length() < 1) {
            ctx.send(NO_COMMAND).queue();
            return;
        }
        String invoked = ctx.args.get(0);

        if (invoked.equals("list"))
            autoroleList(ctx);
        else if (invoked.equals("add"))
            autoroleAdd(ctx);
        else if (invoked.equals("remove"))
            autoroleRemove(ctx);
        else if (invoked.equals("clear"))
            autoroleClear(ctx);
        else
            ctx.send(NO_COMMAND).queue();
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

        if (ctx.args.size() < 2 ||
                (role = parseRole(ctx.guild, ctx.rawArgs.substring(ctx.args.get(0).length()).trim())) == null) {
            ctx.send(Emotes.getFailure() + " I need a role in the form of the name, @role, or ID!").queue();
            throw new PassException();
        }

        return role;
    }

    private void autoroleList(Context ctx) throws SQLException {
        Collection<GuildAutorole> autoroles = autorolesFor(ctx.guild.getIdLong());
        if (autoroles.size() < 1) {
            ctx.send(Emotes.getFailure() + " There are no autoroles in this server!").queue();
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
            ctx.send(Emotes.getFailure() + " Role is already an autorole!").queue();
            return;
        } else if (role.isManaged()) {
            ctx.send(Emotes.getFailure() + " That role is a special bot role, or is managed by an integration!").queue();
            return;
        } else if (!ctx.guild.getSelfMember().canInteract(role)) {
            ctx.send(Emotes.getFailure() + " I need to be higher up on the role ladder to apply that role!").queue();
            return;
        }

        GuildAutorole autorole = new GuildAutorole(role.getIdLong(), ctx.guild.getIdLong(), 0, "{}");
        autoroleDao.create(autorole);

        ctx.send(Emotes.getSuccess() + " Role added to autoroles.").queue();
    }

    private void autoroleRemove(Context ctx) throws SQLException {
        Role role = requireRole(ctx);
        if (!autoroleDao.idExists(role.getIdLong())) {
            ctx.send(Emotes.getFailure() + " Role isn't an already autorole!").queue();
            return;
        }

        autoroleDao.deleteById(role.getIdLong());

        ctx.send(Emotes.getSuccess() + " Role removed from autoroles.").queue();
    }

    private void autoroleClear(Context ctx) throws SQLException {
        DeleteBuilder builder = autoroleDao.deleteBuilder();
        builder.where()
                .eq("guildId", ctx.guild.getIdLong());
        int deleted = builder.delete();

        ctx.send(Emotes.getSuccess() + " Cleared " + deleted + " autoroles.").queue();
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
            ctx.send(Emotes.getFailure() + " I need to be able to **create instant invites**!").queue();
            return;
        }

        ch.createInvite()
                .setUnique(false)
                .setTemporary(false)
                .setMaxAge(0)
                .setMaxUses(0)
                .queue(i -> {
                    ctx.send(Emotes.getSuccess() + " Invite created to " +
                            ch.getAsMention() + ".\n" + i.getURL()).queue();
                }, e -> {
                    ctx.send(Emotes.getFailure() + " Failed to create invite!").queue();
                    logger.error("Invite creation error", e);
                });
    }

    @Perm.Combo.ManageChannelsAndMessages
    @Perm.Combo.ManageServerAndMessages
    @Cooldown(scope = BucketType.GUILD, delay = 30)
    @Command(name = "archive", desc = "Archive a channel's messages into another.", guildOnly = true,
            usage = "[channel to archive] [destination channel] {number of messages, default = all}", thread = true)
    public void cmdArchive(Context ctx) {
        if (ctx.message.getMentionedChannels().size() < 2) {
            ctx.send(Emotes.getFailure() +
                    " You must specify a #channel to archive, and a destination #channel.").queue();
            return;
        } else if (archivingGuilds.contains(ctx.guild.getIdLong())) {
            ctx.send(Emotes.getFailure() + " There is already an archival running in this server!").queue();
            return;
        }

        archivingGuilds.add(ctx.guild.getIdLong());

        try {
            TextChannel from = ctx.message.getMentionedChannels().get(0);
            TextChannel to = ctx.message.getMentionedChannels().get(1);

            if (!ctx.guild.getSelfMember().hasPermission(from, Permission.MESSAGE_HISTORY)) {
                ctx.send(Emotes.getFailure() + " I need to be able to **read message history** in " +
                        from.getAsMention() + '!').queue();
                return;
            } else if (!ctx.guild.getSelfMember().hasPermission(to, Permission.MANAGE_WEBHOOKS)) {
                ctx.send(Emotes.getFailure() + " I need to be able to **manage webhooks** in " +
                        to.getAsMention() + '!').queue();
                return;
            }

            String fromId = from.getId();
            String fromMsgId = fromId;

            EmbedBuilder statusEmb = newEmbedWithAuthor(ctx)
                    .setTitle("Archiving channel into #" + to.getName() + "...")
                    .setColor(val(ctx.guild.getSelfMember().getColor()).or(Color.WHITE))
                    .addField("Status", "Processed **0** messages so far.", false)
                    .setFooter("Last updated at", null)
                    .setTimestamp(OffsetDateTime.now());
            Message statusMsg = ctx.send(statusEmb.build()).complete();

            int sz = 0; // number of messages
            short oneHundred = (short)100;

            try {
                statusMsg.pin().queue(null, f -> {});
            } catch (Throwable ignored) {}
            // TODO: in embed msg, add image to embed for the link to image in msg

            String b = "(Temp) Archival to #";
            Webhook hook = to.createWebhook(b +
                    (to.getName().substring(0, Math.min(to.getName().length(), 32 - b.length()))))
                    .reason("Creating temporary webhook for the archival of messages from #" +
                            from.getName() + " to #" + to.getName() +
                            ". This will be deleted afterwards, and is used to speed up the process by orders of magnitude.")
                    .complete();

            List<Message> historyChunk = new ArrayList<>(100);
            List<MessageEmbed> embedQueue = new ArrayList<>(10);
            embedQueue.add(null);

            try (WebhookClient client = new WebhookClientBuilder(hook)
                    .setHttpClient(Bot.http)
                    .setDaemon(true)
                    .setExecutorService(bot.getScheduledExecutor())
                    .build()) {

                // webhook setup
                WebhookMessageBuilder wmb = new WebhookMessageBuilder()
                        .setUsername("Message Archive")
                        .setAvatarUrl(ctx.jda.getSelfUser().getEffectiveAvatarUrl());

                try {
                    Field f = wmb.getClass().getDeclaredField("embeds");
                    f.setAccessible(true);
                    f.set(wmb, embedQueue);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }

                WebhookMessage wm = wmb.build();
                embedQueue.remove(0);

                EmbedBuilder emb = new EmbedBuilder();

                Paginator embMeta = new Paginator(1024);
                Paginator fieldPager = new Paginator(1024);

                getHistoryAfter(from, fromId, fromMsgId, oneHundred, historyChunk).complete();
                while (historyChunk.size() > 0) {
                    getHistoryAfter(from, fromId, fromMsgId, oneHundred, historyChunk).complete();
                    fromMsgId = historyChunk.get(historyChunk.size() - 1).getId();

                    for (int i = historyChunk.size() - 1; i > 0; --i) {
                        Message msg = historyChunk.get(i);
                        User author = msg.getAuthor();

                        try {
                            embFields.set(emb, new LinkedList<>());
                        } catch (ReflectiveOperationException e) {
                            logger.fatal("Error setting fields = new LinkedList on EmbedBuilder", e);
                            emb.clearFields();
                        }

                        emb.setImage(null)
                                .setThumbnail(null)
                                .setFooter(null, null)
                                .setAuthor(author.getName() + '#' + author.getDiscriminator(), null,
                                        author.getEffectiveAvatarUrl())
                                .setTimestamp(msg.getCreationTime());

                        try {
                            embDescription.set(emb, new StringBuilder(msg.getContentRaw()));
                        } catch (ReflectiveOperationException e) {
                            logger.fatal("Error setting description on EmbedBuilder", e);
                            emb.setDescription(msg.getContentRaw());
                        }

                        Member member;
                        if ((member = ctx.guild.getMember(author)) != null) {
                            Color col = member.getColor();

                            if (col == null) {
                                emb.setColor(Color.WHITE);
                            } else {
                                emb.setColor(col);
                            }
                        } else {
                            emb.setColor(Color.WHITE);
                        }

                        boolean fiImg = true;

                        if (msg.getEmbeds().size() > 0) {
                            final String em = "Embed: ";
                            boolean fiThumb = true;
                            boolean fiCol = true;
                            boolean fiFooterIcon = true;
                            int embedsProcessed = 0;

                            embedRenderLoop:
                            for (MessageEmbed embed : msg.getEmbeds()) {
                                int fieldsUsedForEmb = 0;
                                if (embedsProcessed >= 5) {
                                    break;
                                }

                                String title;
                                if (embed.getTitle() != null) {
                                    title = em + embed.getTitle().substring(0,
                                            Math.min(embed.getTitle().length(), 1025 - em.length()));
                                } else {
                                    title = em + "[no title]";
                                }

                                embMeta.reset();

                                if (embed.getThumbnail() != null && fiThumb) {
                                    emb.setThumbnail(embed.getThumbnail().getUrl());
                                    fiThumb = false;
                                }

                                if (embed.getAuthor() != null) {
                                    MessageEmbed.AuthorInfo aInfo = embed.getAuthor();

                                    if (aInfo.getUrl() != null) {
                                        embMeta.addLine("Author: **[" + aInfo.getName() + "](" + aInfo.getUrl() +
                                                ")**" + (aInfo.getIconUrl() == null ? "" : " | [Icon](" +
                                                aInfo.getIconUrl() + ')'));
                                    } else {
                                        embMeta.addLine("Author: **" + aInfo.getName() + "**" +
                                                (aInfo.getIconUrl() == null ? "" : " | [Icon](" +
                                                        aInfo.getIconUrl() + ')'));
                                    }
                                }

                                if (embed.getColor() != null) {
                                    Color col = embed.getColor();

                                    if (fiCol) {
                                        emb.setColor(col);
                                        fiCol = false;
                                    }

                                    embMeta.addLine("Color: r=" + col.getRed() + ", g=" + col.getGreen() +
                                            ", b=" + col.getBlue());
                                }

                                if (embed.getImage() != null) {
                                    MessageEmbed.ImageInfo img = embed.getImage();
                                    if (fiImg) {
                                        emb.setImage(img.getUrl());
                                        fiImg = false;
                                    }

                                    embMeta.addLine("[" + img.getWidth() + "x" + img.getHeight() + " Image](" +
                                            img.getUrl() + ')');
                                }

                                if (embed.getFooter() != null) {
                                    MessageEmbed.Footer footer = embed.getFooter();
                                    String ic = "";

                                    if (footer.getIconUrl() != null) {
                                        if (fiFooterIcon) {
                                            emb.setFooter(null, footer.getIconUrl());
                                            fiFooterIcon = true;
                                        }

                                        ic = "[Icon](" + footer.getIconUrl() + ") | ";
                                    }

                                    embMeta.addLine(ic + "Footer: " + footer.getText());
                                }

                                embMeta.addLine("Total Length: " + embed.getLength() + " characters");
                                embMeta.addLine("Fields: " + embed.getFields().size());

                                for (String page : embMeta.getPages()) {
                                    emb.addField(title, page, false);
                                    fieldsUsedForEmb++;

                                    if (fieldsUsedForEmb >= 4)
                                        continue embedRenderLoop;
                                }

                                // Description
                                if (embed.getDescription() != null) {
                                    for (String page : embedFieldPages(embed.getDescription())) {
                                        emb.addField("Description", page, false);
                                        fieldsUsedForEmb++;

                                        if (fieldsUsedForEmb >= 4)
                                            continue embedRenderLoop;
                                    }
                                }

                                // Fields
                                if (embed.getFields().size() > 0) {
                                    fieldPager.reset();

                                    for (MessageEmbed.Field field : embed.getFields()) {
                                        fieldPager.addLine("**" + field.getName() + "**");
                                        fieldPager.addLine(field.getValue());
                                        fieldPager.addLine("");
                                    }

                                    for (String page : fieldPager.getPages()) {
                                        emb.addField("Fields", page, false);

                                        fieldsUsedForEmb++;

                                        if (fieldsUsedForEmb >= 4)
                                            continue embedRenderLoop;
                                    }
                                }

                                embedsProcessed++;
                            }
                        }

                        if (msg.getAttachments().size() > 0) {
                            for (Message.Attachment attachment : msg.getAttachments()) {
                                if (fiImg && attachment.isImage()) {
                                    emb.setImage(attachment.getUrl());
                                    fiImg = false;
                                } else {
                                    String ct;
                                    if (attachment.isImage()) {
                                        ct = "**Image** \u2022 Dimensions: " + attachment.getWidth() + "x" +
                                                attachment.getHeight() + "\n[Link to Image](" + attachment.getUrl() + ')';
                                    } else {
                                        ct = "[Link to Attachment](" + attachment.getUrl() + ')';
                                    }

                                    float bytes = attachment.getSize();
                                    int unitIdx = 0;

                                    while (bytes >= 1000) {
                                        bytes /= 1000;
                                        unitIdx++;
                                    }

                                    String szz = Strings.number(bytes) + " " + BYTE_UNITS[unitIdx > 8 ? 8 : unitIdx];

                                    emb.addField("Attachment: " + attachment.getFileName() +
                                            " (" + szz + ')', ct, false);
                                }
                            }
                        }

                        embedQueue.add(emb.build());

                        if (embedQueue.size() == 10) {
                            try {
                                client.send(wm).get();

                                Thread.sleep(50);
                            } catch (InterruptedException ignored) {
                            } catch (ExecutionException e) {
                                logger.warn("Archival embed send error", e);
                                client.send(Emotes.getFailure() + " Failed to send messages.");

                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException ign) {
                                }
                            } finally {
                                embedQueue.clear();
                            }
                        }

                        if (++sz % 50 == 0) {
                            statusMsg.editMessage(statusEmb.clearFields()
                                    .addField("Status", "Processed **" + Strings.number(sz) +
                                            "** messages so far.", false)
                                    .setTimestamp(OffsetDateTime.now())
                                    .build()).queue();
                        }
                    }

                    if (embedQueue.size() > 0) {
                        client.send(wm);
                    }
                }

                statusMsg.editMessage(statusEmb.clearFields()
                        .addField("Stage", "Completed!", false)
                        .addField("Status", "Successfully archived " + sz + " messages from " +
                                from.getAsMention() + " to " + to.getAsMention() + '.', false)
                        .setTimestamp(OffsetDateTime.now())
                        .build()).queue();

                ctx.send(Emotes.getSuccess() + " Archival from " + from.getAsMention() + " to " +
                        to.getAsMention() + " has **finished**!").queue();
            } catch (PermissionException e) {
                ctx.send(Emotes.getFailure() + " Don't revoke my permissions! I need **" +
                        e.getPermission().getName() + "**!").queue();
            } finally {
                if (ctx.guild.getSelfMember().hasPermission(to, Permission.MANAGE_WEBHOOKS)) {
                    hook.delete()
                            .reason("Archival from #" + from.getName() + " to #" + to.getName() +
                                    " has finished, so the temporary webhook is being deleted.")
                            .queue();
                } // if they revoke the perm during message creation...
            }
        } finally {
            archivingGuilds.remove(ctx.guild.getIdLong());
        }
    }

    @CheckReturnValue
    private RestAction<List<Message>> getHistoryAfter(MessageChannel channel, String channelId, String messageId,
                                                       short limit, List<Message> target) {
        Checks.check(limit >= 1 && limit <= 100,
                "Provided limit was out of bounds. Minimum: 1, Max: 100. Provided: %d", limit);

        Route.CompiledRoute route = Route.Messages.GET_MESSAGE_HISTORY.compile(channelId)
                .withQueryParams("limit", Short.toString(limit), "after", messageId);

        return new RestAction<>(bot.getJda(), route) {
            @Override
            protected void handleResponse(Response response, Request<List<Message>> request) {
                if (!response.isOk()) {
                    request.onFailure(response);
                    return;
                }

                EntityBuilder builder = api.getEntityBuilder();
                target.clear();
                JSONArray historyJson = response.getArray();

                for (int i = 0; i < historyJson.length(); i++)
                    target.add(builder.createMessage(historyJson.getJSONObject(i), channel, false));

                request.onSuccess(target);
            }
        };
    }
}
