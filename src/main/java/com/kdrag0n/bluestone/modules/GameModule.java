package com.kdrag0n.bluestone.modules;

import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.types.Module;
import com.kdrag0n.bluestone.Context;
import com.kdrag0n.bluestone.Emotes;
import com.kdrag0n.bluestone.annotations.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;

public class GameModule extends Module {
    private static final Logger logger = LoggerFactory.getLogger(GameModule.class);

    public GameModule(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Games";
    }

    @Command(name = "rps", desc = "Play a game of Rock, Paper, Scissors with the bot. (10 rounds)")
    public void cmdRps(Context ctx) {
        if (ctx.channel instanceof TextChannel && !ctx.member.hasPermission((GuildChannel) ctx.channel,
                Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS)) {
            ctx.send(Emotes.getFailure() + " I need to be able to **add reactions** and **embed links** here!").queue();
            return;
        }

        new RpsGame(ctx);
    }

    private final class RpsGame {
        private final Object[] REACTIONS = { "🗿", "📰", "✂", "⛔" };
        private final String[] IDX_CMAP = { "Rock", "Paper", "Scissors" };
        private final String[] IDX_MAP = { "rock", "paper", "scissors" };
        private final String[] WIN_STATUSES = { "seems to win over", "wins over", "beats", "goes over", "destroys",
                "eliminates", "takes down", "dramatically incinerates", "overpowers", "wrecks", "blows up", "escapes",
                "dominates", "is clearly superior to", "is superior to", "is definitely better than", "tackles" };
        private final String[] LOSE_STATUSES = { "can't beat", "loses to", "is taken down by", "goes under",
                "is tortured by", "cowers under", "hides from", "runs away from", "flees from", "begs for mercy from",
                "can't take on", "fails to hold its own against", "obeys", "bows down to" };
        private final String[] WIN_PREFIX = { "You're in luck.", "You win!", "I bow down to you.",
                "I'm crying right now.", "You dominate me.", "It's your lucky day.", "You're doing well.", "Good job.",
                "You fiercely take me down.", "You just tackle me. That can't be.",
                "Me, a puny little bot, is nothing for you.", "RIP me.", "I can't beat you. 😭",
                "I admit, I'm dumb. Happy now?", "You played well.", "Good choice.", "Smart choice.",
                "I'm is no match for you. But you're just a puny little human!",
                "CHEATER! That CAN'T be! I always win!" };
        private final String[] WIN_SUFFIX = { "That makes sense for you.", "Just what I expected.",
                "I expected nothing less.", "Good job.",
                "You did better than I thought. Maybe puny humans like you have some worth.", "I'm surprised.",
                "You weren't supposed to do so good.", "That's just wrong.", "Nice playing.", "Nice luck.",
                "Nice shot.", "You're doing a bit *too* good. Slow down, buddy.",
                "Whoa, whoa. Slow down there, buddy." };
        private final String[] LOSE_PREFIX = { "Uh-oh.", "RIP.", "The mighty AI takes you down.",
                "You're no match for the AI.", "You're no more.",
                "You helplessly cry as the AI's weapon of choice tears you apart.", "0/10 WANT MORE ACTION. 2ez.",
                "Too easy!", "You should be better than this.", "How can you not win? It's so easy.", "Amusing.",
                "WANT. MORE. ACTION. NOW.", "Has anyone ever told you that you suck? If not, well, you do.", "Noh.",
                "Not this time, human. And never.", "Boo-hoo. You're dead." };
        private final String[] LOSE_SUFFIX = { "It was worth a shot.", "At least you played.", "Better luck next time.",
                "0/10 Not enough action, IGN.", "You should be better than this.", "I expected more.",
                "Can't you do such a simple thing?", "I guess humans have no hope.",
                "Hopeless little humans... You're a better dinner than this.", "Dinner, mmm.",
                "Are you capable of winning? I doubt it.", "Meh, I'm not even sweating. You?",
                "Oh nooo. I was so scared." };

        private final EmbedBuilder emb = new EmbedBuilder();
        private Message message;
        private final Runnable onFinish;
        private final MessageChannel channel;
        private final long userId;

        private short plays = 0;
        private short wins = 0;
        private short losses = 0;
        private boolean isActive = true;

        private RpsGame(Context ctx) {
            this.channel = ctx.channel;
            this.userId = ctx.author.getIdLong();

            emb.setAuthor("Rock, Paper, Scissors!", null, ctx.jda.getSelfUser().getEffectiveAvatarUrl())
                    .setDescription("⌛ **Please wait, game is starting...**")
                    .setFooter("Game started at", ctx.author.getEffectiveAvatarUrl()).setTimestamp(Instant.now());

            if (ctx.guild == null)
                emb.setColor(randomColor());
            else
                emb.setColor(val(ctx.member.getColor()).or(Color.WHITE));

            message = channel.sendMessage(emb.build()).complete();
            for (Object emoji : REACTIONS) {
                message.addReaction((String) emoji).complete();
            }
            next();

            onFinish = () -> {
                String descSuffix = (wins == losses ? "That means you tied. " + loseSuffix()
                        : (wins > losses ? "That makes you the winner. " + winSuffix()
                                : "That makes you the loser. " + loseSuffix()));
                emb.setDescription("❌ Game ended.\n\nYou won **" + wins + "** times, and lost **" + losses + "** times."
                        + '\n' + descSuffix);

                message.editMessage(emb.build()).queue();
                try {
                    message.clearReactions().queue();
                } catch (ErrorResponseException | PermissionException ignored) {
                } catch (IllegalStateException ignored) { // DM
                    for (MessageReaction reaction : message.getReactions()) {
                        reaction.removeReaction().queue();
                        reaction.removeReaction(ctx.author).queue();
                    }
                }

                isActive = false;
            };

            scheduleEventWait(ctx);
        }

        private void scheduleEventWait(Context ctx) {
            bot.eventWaiter.waitForEvent(MessageReactionAddEvent.class,
                    ev -> ev.getChannel().getIdLong() == channel.getIdLong()
                            && ev.getMessageIdLong() == message.getIdLong() && ev.getUser().getIdLong() == userId,
                    ev -> {
                        if (!isActive)
                            return;

                        byte answer = (byte) ArrayUtils.indexOf(REACTIONS, ev.getReactionEmote().getName());

                        if (answer == 3) {
                            emb.clearFields().addField("Status", "Game was stopped before the end!", false);
                            onFinish.run();
                            return;
                        }

                        try {
                            ans(answer);
                        } finally {
                            try {
                                ev.getReaction().removeReaction(ctx.author).queue();
                            } catch (Throwable ignored) {
                            }

                            scheduleEventWait(ctx);
                        }
                    }, 2, TimeUnit.MINUTES, onFinish);
        }

        private void ans(byte answer) {
            byte ai = (byte) ThreadLocalRandom.current().nextInt(0, 2);
            if (answer == ai) {
                emb.clearFields().addField("Round #" + plays, "**Tie!** Try again.\n\nRock, paper, or scissors?",
                        false);
                message.editMessage(emb.build()).queue();
                return;
            }
            boolean win;

            if (answer == 0 && ai == 2)
                win = true;
            else if (answer == 0 && ai == 1)
                win = false;
            else if (answer == 1 && ai == 2)
                win = false;
            else if (answer == 1 && ai == 0)
                win = true;
            else if (answer == 2 && ai == 1)
                win = true;
            else if (answer == 2 && ai == 0)
                win = false;
            else
                win = false;

            boolean flip = ThreadLocalRandom.current().nextBoolean();
            int a;
            int b;
            if (flip) {
                a = (int) ai;
                b = (int) answer;
            } else {
                a = (int) answer;
                b = (int) ai;
            }

            String msg = IDX_CMAP[a] + ' '
                    + (flip ? (win ? loseStatus() : winStatus()) : (win ? winStatus() : loseStatus())) + ' '
                    + IDX_MAP[b] + '.';

            boolean wlSide = ThreadLocalRandom.current().nextBoolean();
            if (wlSide) {
                if (win) {
                    msg = winPrefix() + ' ' + msg;
                } else {
                    msg = losePrefix() + ' ' + msg;
                }
            } else {
                if (win) {
                    msg = msg + ' ' + winSuffix();
                } else {
                    msg = msg + ' ' + loseSuffix();
                }
            }

            emb.clearFields().addField("Round #" + plays, msg, false);
            message.editMessage(emb.build()).queue();

            if (win)
                ++wins;
            else
                ++losses;
            next();
        }

        private String winStatus() {
            return WIN_STATUSES[ThreadLocalRandom.current().nextInt(0, WIN_STATUSES.length)];
        }

        private String loseStatus() {
            return LOSE_STATUSES[ThreadLocalRandom.current().nextInt(0, LOSE_STATUSES.length)];
        }

        private String winPrefix() {
            return WIN_PREFIX[ThreadLocalRandom.current().nextInt(0, WIN_PREFIX.length)];
        }

        private String losePrefix() {
            return LOSE_PREFIX[ThreadLocalRandom.current().nextInt(0, LOSE_PREFIX.length)];
        }

        private String winSuffix() {
            return WIN_SUFFIX[ThreadLocalRandom.current().nextInt(0, WIN_SUFFIX.length)];
        }

        private String loseSuffix() {
            return LOSE_SUFFIX[ThreadLocalRandom.current().nextInt(0, LOSE_SUFFIX.length)];
        }

        private void next() {
            if (plays == 10) {
                onFinish.run();
                return;
            }

            emb.setDescription(null).addField("Round #" + ++plays, "Rock, paper, or scissors?", false);

            message.editMessage(emb.build()).queue();
        }
    }
}
