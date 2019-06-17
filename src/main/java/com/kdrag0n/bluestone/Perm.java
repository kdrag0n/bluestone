package com.kdrag0n.bluestone;

import com.kdrag0n.bluestone.errors.PermissionException;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;

import java.lang.annotation.*;
import java.util.List;

public enum Perm {
    // Discord permissions (must copy for security reasons)
    CREATE_INSTANT_INVITE(Permission.CREATE_INSTANT_INVITE),
    KICK_MEMBERS(Permission.KICK_MEMBERS),
    BAN_MEMBERS(Permission.BAN_MEMBERS),
    ADMINISTRATOR(Permission.ADMINISTRATOR),
    MANAGE_CHANNEL(Permission.MANAGE_CHANNEL),
    MANAGE_SERVER(Permission.MANAGE_SERVER),
    MESSAGE_ADD_REACTION(Permission.MESSAGE_ADD_REACTION),
    VIEW_AUDIT_LOGS(Permission.VIEW_AUDIT_LOGS),
    PRIORITY_SPEAKER(Permission.PRIORITY_SPEAKER),

    // Applicable to all channel types
    VIEW_CHANNEL(Permission.VIEW_CHANNEL),

    // Text permissions
    MESSAGE_READ(Permission.MESSAGE_READ),
    MESSAGE_WRITE(Permission.MESSAGE_WRITE),
    MESSAGE_TTS(Permission.MESSAGE_TTS),
    MESSAGE_MANAGE(Permission.MESSAGE_MANAGE),
    MESSAGE_EMBED_LINKS(Permission.MESSAGE_EMBED_LINKS),
    MESSAGE_ATTACH_FILES(Permission.MESSAGE_ATTACH_FILES),
    MESSAGE_HISTORY(Permission.MESSAGE_HISTORY),
    MESSAGE_MENTION_EVERYONE(Permission.MESSAGE_MENTION_EVERYONE),
    MESSAGE_EXT_EMOJI(Permission.MESSAGE_EXT_EMOJI),

    // Voice permissions
    VOICE_CONNECT(Permission.VOICE_CONNECT),
    VOICE_SPEAK(Permission.VOICE_SPEAK),
    VOICE_MUTE_OTHERS(Permission.VOICE_MUTE_OTHERS),
    VOICE_DEAF_OTHERS(Permission.VOICE_DEAF_OTHERS),
    VOICE_MOVE_OTHERS(Permission.VOICE_MOVE_OTHERS),
    VOICE_USE_VAD(Permission.VOICE_USE_VAD),

    NICKNAME_CHANGE(Permission.NICKNAME_CHANGE),
    NICKNAME_MANAGE(Permission.NICKNAME_MANAGE),

    MANAGE_ROLES(Permission.MANAGE_ROLES),
    MANAGE_PERMISSIONS(Permission.MANAGE_PERMISSIONS),
    MANAGE_WEBHOOKS(Permission.MANAGE_WEBHOOKS),
    MANAGE_EMOTES(Permission.MANAGE_EMOTES),

    UNKNOWN(Permission.UNKNOWN),

    // Special bot permissions
    BOT_OWNER(60, "Bot Owner");

    public final long raw;
    public final String name;
    private final Permission discordPerm;

    Perm(long offset, String name) {
        this.raw = 1 << offset;
        this.name = name;
        this.discordPerm = null;
    }

    Perm(Permission discordPerm) {
        this.raw = discordPerm.getRawValue();
        this.name = discordPerm.getName();
        this.discordPerm = discordPerm;
    }

    public static boolean check(Context ctx, List<Perm> perms) {
        // this loop functions as OR
        for (Perm perm: perms) {
            if (perm.check(ctx)) {
                return true;
            }
        }

        return false;
    }

    public static void checkThrow(Context ctx, List<Perm> perms) throws PermissionException {
        if (!check(ctx, perms))
            throw new PermissionException("Missing permissions", perms);
    }

    public boolean check(Context ctx) {
        if (ctx.author.getIdLong() == Bot.ownerId) {
            return true;
        }

        if (this == BOT_OWNER) { // BOT owner
            return false; // because of the above check
        } else if (discordPerm != null) { // this is a Discord permission
            if (ctx.guild != null) {
                return ctx.member.hasPermission((TextChannel) ctx.channel, discordPerm);
            }

            return false;
        } else {
            throw new PermissionException("Unknown permission " + name + " value=" + raw, this);
        }
    }

    // Permission annotations
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Owner {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Invite {
        Perm value() default Perm.CREATE_INSTANT_INVITE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Kick {
        Perm value() default Perm.KICK_MEMBERS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Ban {
        Perm value() default Perm.BAN_MEMBERS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Administrator {
        Perm value() default Perm.ADMINISTRATOR;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageChannels {
        Perm value() default Perm.MANAGE_CHANNEL;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageServer {
        Perm value() default Perm.MANAGE_SERVER;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManagePermissions {
        Perm value() default Perm.MANAGE_PERMISSIONS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface AddReactions {
        Perm value() default Perm.MESSAGE_ADD_REACTION;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ViewAuditLogs {
        Perm value() default Perm.VIEW_AUDIT_LOGS;
    }

    public static class Message {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Read {
            Perm value() default Perm.MESSAGE_READ;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Send {
            Perm value() default Perm.MESSAGE_WRITE;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface SendTTS {
            Perm value() default Perm.MESSAGE_TTS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Manage {
            Perm value() default Perm.MESSAGE_MANAGE;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Embed {
            Perm value() default Perm.MESSAGE_EMBED_LINKS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Attach {
            Perm value() default Perm.MESSAGE_ATTACH_FILES;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface ReadHistory {
            Perm value() default Perm.MESSAGE_HISTORY;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface MentionEveryone {
            Perm value() default Perm.MESSAGE_MENTION_EVERYONE;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface UseExternalEmojis {
            Perm value() default Perm.MESSAGE_EXT_EMOJI;
        }
    }

    public static class Voice {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Connect {
            Perm value() default Perm.VOICE_CONNECT;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Speak {
            Perm value() default Perm.VOICE_SPEAK;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Mute {
            Perm value() default Perm.VOICE_MUTE_OTHERS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Deafen {
            Perm value() default Perm.VOICE_DEAF_OTHERS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Move {
            Perm value() default Perm.VOICE_MOVE_OTHERS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface VoiceActivity {
            Perm value() default Perm.VOICE_USE_VAD;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ChangeNickname {
        Perm value() default Perm.NICKNAME_CHANGE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageNicknames {
        Perm value() default Perm.NICKNAME_MANAGE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageEmotes {
        Perm value() default Perm.MANAGE_EMOTES;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageWebhooks {
        Perm value() default Perm.MANAGE_WEBHOOKS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageRoles {
        Perm value() default Perm.MANAGE_ROLES;
    }
}
