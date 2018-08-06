package com.kdrag0n.bluestone;

import net.dv8tion.jda.core.Permission;

import java.lang.annotation.*;

public class Perm {
    // Permission annotations

    // Compounds/Operators
    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Repeatable(PermAnds.class)
    public @interface All {
        Permission[] value();
    }

    @Deprecated
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PermAnds {
        All[] value();
    }

    // Actual permissions
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Owner {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Admin {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Patron {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Invite {
        Permission value() default Permission.CREATE_INSTANT_INVITE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Kick {
        Permission value() default Permission.KICK_MEMBERS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Ban {
        Permission value() default Permission.BAN_MEMBERS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface GuildAdministrator {
        Permission value() default Permission.ADMINISTRATOR;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageChannels {
        Permission value() default Permission.MANAGE_CHANNEL;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageServer {
        Permission value() default Permission.MANAGE_SERVER;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManagePermissions {
        Permission value() default Permission.MANAGE_PERMISSIONS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface AddReactions {
        Permission value() default Permission.MESSAGE_ADD_REACTION;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ViewAuditLogs {
        Permission value() default Permission.VIEW_AUDIT_LOGS;
    }

    private static class Message {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Read {
            Permission value() default Permission.MESSAGE_READ;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Send {
            Permission value() default Permission.MESSAGE_WRITE;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface SendTTS {
            Permission value() default Permission.MESSAGE_TTS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Manage {
            Permission value() default Permission.MESSAGE_MANAGE;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Embed {
            Permission value() default Permission.MESSAGE_EMBED_LINKS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Attach {
            Permission value() default Permission.MESSAGE_ATTACH_FILES;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface ReadHistory {
            Permission value() default Permission.MESSAGE_HISTORY;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface MentionEveryone {
            Permission value() default Permission.MESSAGE_MENTION_EVERYONE;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface UseExternalEmojis {
            Permission value() default Permission.MESSAGE_EXT_EMOJI;
        }
    }

    public static class Voice {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Connect {
            Permission value() default Permission.VOICE_CONNECT;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Speak {
            Permission value() default Permission.VOICE_SPEAK;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Mute {
            Permission value() default Permission.VOICE_MUTE_OTHERS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Deafen {
            Permission value() default Permission.VOICE_DEAF_OTHERS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface Move {
            Permission value() default Permission.VOICE_MOVE_OTHERS;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface VoiceActivity {
            Permission value() default Permission.VOICE_USE_VAD;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ChangeNickname {
        Permission value() default Permission.NICKNAME_CHANGE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageNicknames {
        Permission value() default Permission.NICKNAME_MANAGE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageEmotes {
        Permission value() default Permission.MANAGE_EMOTES;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageWebhooks {
        Permission value() default Permission.MANAGE_WEBHOOKS;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ManageRoles {
        Permission value() default Permission.MANAGE_ROLES;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Unknown {
        Permission value() default Permission.UNKNOWN;
    }

    public static class Combo {
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface ManageMessagesAndReadHistory {
            Permission[] value() default {Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY};
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface ManageServerAndInvite {
            Permission[] value() default {Permission.MANAGE_SERVER, Permission.CREATE_INSTANT_INVITE};
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface ManageRolesAndInvite {
            Permission[] value() default {Permission.MANAGE_ROLES, Permission.CREATE_INSTANT_INVITE};
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface ManageChannelsAndMessages {
            Permission[] value() default {Permission.MANAGE_CHANNEL, Permission.MESSAGE_MANAGE};
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.METHOD)
        public @interface ManageServerAndMessages {
            Permission[] value() default {Permission.MANAGE_SERVER, Permission.MESSAGE_MANAGE};
        }
    }
}
