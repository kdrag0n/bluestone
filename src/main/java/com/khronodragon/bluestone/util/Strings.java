package com.khronodragon.bluestone.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import net.dv8tion.jda.core.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Strings {
    private static final int bmpThreshold = 1 << 16;
    private static final int offset = 1 << 10;
    private static final int high = 0xd800;
    private static final int low = 0xdc00;
    private static final Pattern mentionPattern = Pattern.compile("^<@!?(\\d{17,20})>$");
    private static final Pattern roleMentionPattern = Pattern.compile("^<@&(\\d{17,20})>$");
    private static final Pattern packagePattern = Pattern.compile("^(?:[a-z0-9\\-_]+\\.)+[a-zA-Z0-9]+$");
    private static final Pattern idPattern = Pattern.compile("^\\d{17,20}$");
    private static final Pattern tagPattern = Pattern.compile("^.{2,32}#\\d{4}$");
    private static final Pattern d4Pattern = Pattern.compile("^\\d{1,4}$");
    private static final Pattern channelNamePattern = Pattern.compile("^[a-z0-9_-]{2,100}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ipDomainPattern = Pattern.compile("^(?:localhost|[a-zA-Z\\-.]+\\.[a-z]{2,15}|(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|[0-9a-f:]+)$");
    private static final Pattern mcNamePattern = Pattern.compile("^[a-zA-Z0-9_]{1,32}$");
    private static final LoadingCache<String, MessageFormat> formatCache = CacheBuilder.newBuilder()
            .maximumSize(120)
            .concurrencyLevel(6)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, MessageFormat>() {
                @Override
                public MessageFormat load(String key) throws Exception {
                    return new MessageFormat(key);
                }
            });

    public static int[] spread(String str) {
        TIntList codePoints = new TIntLinkedList();
        int i = 0;

        while (i < str.length()) {
            int first = Character.codePointAt(str, i);
            i++;
            if (high <= first && first < high + offset) {
                int second = Character.codePointAt(str, i);
                i++;
                if (low <= second && second < low + offset) {
                    codePoints.add((first - high) * offset + (second - low) + bmpThreshold);
                } else {
                    return null;
                }
            } else {
                codePoints.add(first);
            }
        }

        return codePoints.toArray();
    }

    public static String smartJoin(String[] array) {
        return smartJoin(array, "and");
    }

    public static String smartJoin(List<String> list) {
        return smartJoin(list, "and");
    }

    public static String smartJoin(String[] array, String sep) {
        if (array.length == 2)
            return array[0] + ' ' + sep + ' ' + array[1];

        if (array.length > 1)
            array[array.length - 1] = sep + ' ' + array[array.length - 1];

        return String.join(", ", array);
    }

    public static String smartJoin(List<String> list, String sep) {
        if (list.size() == 2)
            return list.get(0) + ' ' + sep + ' ' + list.get(1);

        if (list.size() > 1)
            list.set(list.size() - 1, sep + ' ' + list.get(list.size() - 1));

        return String.join(", ", list);
    }

    public static String str(long value) {
        return Long.toString(value);
    }

    public static String str(int value) {
        return Integer.toString(value);
    }

    public static String str(short value) {
        return Short.toString(value);
    }

    public static String str(double value) {
        return Double.toString(value);
    }

    public static String simpleJoin(List<String> strings) {
        StringBuilder builder = new StringBuilder();

        for (String str: strings) {
            builder.append(str);
        }

        return builder.toString();
    }

    public static String replace(String input, Pattern regex, StringReplacerCallback callback) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = regex.matcher(input);
        while (matcher.find()) {
            String rep = callback.replace(matcher.group(1));

            if (rep == null)
                matcher.appendReplacement(result, "**[__unknown key__]**");
            else
                matcher.appendReplacement(result, Matcher.quoteReplacement(rep));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public static StringMapper createMap() {
        return StringMapper.match();
    }

    public static String buildQueryUrl(String base, String... args) {
        if (args.length < 1)
            return base;
        if (args.length % 2 != 0)
            throw new IllegalArgumentException("Query parameters must be key, value");

        try {
            StringBuilder builder = new StringBuilder(base)
                    .append('?');

            for (int i = 0; i < args.length - 1; i += 2) {
                if (args.length > 2)
                    builder.append('&');

                builder.append(args[i])
                        .append('=')
                        .append(URLEncoder.encode(args[i + 1], "UTF-8"));
            }

            return builder.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String renderMessage(Message message, Guild guild, String msg) {
        String tmp = msg;

        for (User user : message.getMentionedUsers()) {
            if (message.isFromType(ChannelType.PRIVATE) || message.isFromType(ChannelType.GROUP)) {
                tmp = StringUtils.replace(StringUtils.replace(tmp, "<@" + user.getId() + '>', '@' + user.getName())
                        , "<@!" + user.getId() + '>', '@' + user.getName());
            } else {
                String name;

                if (guild != null && guild.isMember(user))
                    name = guild.getMember(user).getEffectiveName();
                else name = user.getName();

                tmp = StringUtils.replace(StringUtils.replace(tmp, "<@" + user.getId() + '>', '@' + name)
                        , "<@!" + user.getId() + '>', '@' + name);
            }
        }

        for (Emote emote : message.getEmotes())
            tmp = StringUtils.replace(tmp, emote.getAsMention(), ":" + emote.getName() + ":");

        for (TextChannel mentionedChannel : message.getMentionedChannels())
            tmp = StringUtils.replace(tmp, "<#" + mentionedChannel.getId() + '>', '#' + mentionedChannel.getName());

        for (Role mentionedRole : message.getMentionedRoles())
            tmp = StringUtils.replace(tmp, "<@&" + mentionedRole.getId() + '>', '@' + mentionedRole.getName());

        return tmp;
    }

    public static String statify(IntStream stream) {
        IntSummaryStatistics stats = stream.summaryStatistics();

        return String.format("Min: %d\nAvg: %.2f\nMax: %d", stats.getMin(), stats.getAverage(), stats.getMax());
    }

    public static boolean isMention(CharSequence str) {
        return mentionPattern.matcher(str).matches();
    }

    public static boolean isRoleMention(CharSequence str) {
        return roleMentionPattern.matcher(str).matches();
    }

    public static boolean isPackage(CharSequence str) {
        return packagePattern.matcher(str).matches();
    }

    public static boolean isID(CharSequence str) {
        return idPattern.matcher(str).matches();
    }

    public static boolean isTag(CharSequence str) {
        return tagPattern.matcher(str).matches();
    }

    public static boolean is4Digits(CharSequence str) {
        return d4Pattern.matcher(str).matches();
    }

    public static boolean isChannelName(CharSequence str) {
        return channelNamePattern.matcher(str).matches();
    }

    public static boolean isIPorDomain(CharSequence str) {
        return ipDomainPattern.matcher(str).matches();
    }

    public static boolean isMinecraftName(CharSequence str) {
        return mcNamePattern.matcher(str).matches();
    }

    public static String format(String pattern, Object... args) {
        return formatCache.getUnchecked(pattern).format(args, new StringBuffer(), null).toString();
    }
}
