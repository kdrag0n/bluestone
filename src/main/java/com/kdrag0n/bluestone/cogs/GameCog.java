package com.kdrag0n.bluestone.cogs;

import com.kdrag0n.bluestone.Bot;
import com.kdrag0n.bluestone.Cog;
import com.kdrag0n.bluestone.Context;
import com.kdrag0n.bluestone.Emotes;
import com.kdrag0n.bluestone.annotations.Command;
import com.kdrag0n.bluestone.util.Strings;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.PermissionException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.kdrag0n.bluestone.util.NullValueWrapper.val;
import static com.kdrag0n.bluestone.util.Strings.str;

public class GameCog extends Cog {
    private static final Logger logger = LoggerFactory.getLogger(GameCog.class);

    public GameCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Games";
    }

    public String getDescription() {
        return "Quick games to play in chat.";
    }

    @Command(name = "rps", desc = "Play a game of Rock, Paper, Scissors with the bot. (10 rounds)")
    public void cmdRps(Context ctx) {
        if (ctx.channel instanceof TextChannel && !ctx.member.hasPermission((Channel) ctx.channel,
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

    @Command(name = "akinator", desc = "Play a game of Akinator, where you answer questions for it to guess your character.", aliases = {
            "guess" })
    public void cmdAkinator(Context ctx) {
        if (ctx.channel instanceof TextChannel && !ctx.member.hasPermission((Channel) ctx.channel,
                Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS)) {
            ctx.send(Emotes.getFailure() + " I need to be able to **add reactions** and **embed links** here!").queue();
            return;
        }

        try {
            new AkinatorGame(ctx);
        } catch (IOException e) {
            logger.error("Error contacting Akinator", e);
            ctx.fail("An error occurred contacting Akinator.");
        } catch (JSONException ignored) {
            ctx.fail("Akinator seems to be having some issues right now.");
        }
    }

    private final class AkinatorGame {
        private static final String NEW_SESSION_URL = "http://api-en4.akinator.com/ws/new_session?partner=1&player=";
        private static final String ANSWER_URL = "http://api-en4.akinator.com/ws/answer";
        private static final String GET_GUESS_URL = "http://api-en4.akinator.com/ws/list";
        private static final String CHOICE_URL = "http://api-en4.akinator.com/ws/choice";
        private static final String EXCLUSION_URL = "http://api-en4.akinator.com/ws/exclusion";
        private final Object[] REACTIONS = { "✅", "❌", "🤷", "👍", "👎", "⛔" };

        private final OkHttpClient client = new OkHttpClient();
        private final EmbedBuilder emb = new EmbedBuilder();
        private Message message;
        private final Runnable onFinish;
        private final MessageChannel channel;
        private final long userId;
        private StepInfo stepInfo;

        private final String signature;
        private final String session;
        private Guess guess;
        private boolean lastQuestionWasGuess = false;
        private boolean isActive = true;

        private AkinatorGame(Context ctx) throws IOException, JSONException {
            this.channel = ctx.channel;
            this.userId = ctx.author.getIdLong();

            // Start new session
            JSONObject json = new JSONObject(client
                    .newCall(new Request.Builder().get().url(NEW_SESSION_URL + RandomStringUtils.random(16)).build())
                    .execute().body().string());
            stepInfo = new StepInfo(json);

            signature = stepInfo.getSignature();
            session = stepInfo.getSession();

            emb.setAuthor("Akinator Game", "http://akinator.com", ctx.jda.getSelfUser().getEffectiveAvatarUrl())
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
            presentNextQuestion();

            onFinish = () -> {
                emb.setDescription("❌ Game ended.\n\nThere w");
                if (stepInfo.getStepNum() == 0)
                    emb.appendDescription("as 1 question");
                else
                    emb.getDescriptionBuilder().append("ere ").append(stepInfo.getStepNum() + 1).append(" questions");
                emb.getDescriptionBuilder().append('.');

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

                        if (answer == 5) {
                            emb.setImage(null).clearFields().addField("Status", "Game was stopped before the end!",
                                    false);
                            onFinish.run();
                            return;
                        }

                        try {
                            if (lastQuestionWasGuess) {
                                if (answer != 0 && answer != 1)
                                    return;

                                answerGuess(answer);
                            } else {
                                answerQuestion(answer);
                            }
                        } finally {
                            try {
                                ev.getReaction().removeReaction(ctx.author).queue();
                            } catch (Throwable ignored) {
                            }

                            scheduleEventWait(ctx);
                        }
                    }, 2, TimeUnit.MINUTES, onFinish);
        }

        private void endInvalidData() {
            emb.setImage(null).clearFields().addField("Status", "Akinator sent invalid data, or failed to respond.",
                    false);
            onFinish.run();
        }

        private void presentNextQuestion() {
            emb.setDescription(null).clearFields().setImage(null).addField("Question #" + (stepInfo.getStepNum() + 1),
                    stepInfo.getQuestion(), false);

            message.editMessage(emb.build()).queue();
            lastQuestionWasGuess = false;
        }

        private void presentGuess() {
            try {
                guess = new Guess();
            } catch (JSONException | IOException ignored) {
                endInvalidData();
                return;
            }

            emb.clearFields().addField("Is this your character?", guess.toString(), false).setImage(guess.getImgPath());

            message.editMessage(emb.build()).queue();
            lastQuestionWasGuess = true;
        }

        private void answerQuestion(byte answer) {
            try {
                JSONObject json = new JSONObject(client
                        .newCall(new Request.Builder().get()
                                .url(Strings.buildQueryUrl(ANSWER_URL, "session", session, "signature", signature,
                                        "step", str(stepInfo.getStepNum()), "answer", Byte.toString(answer)))
                                .build())
                        .execute().body().string());

                try {
                    stepInfo = new StepInfo(json);
                } catch (JSONException ignored) {
                    emb.setImage(null).clearFields().addField("Status", "Akinator ran out of questions.", false);
                    onFinish.run();
                }

                if (stepInfo.getProgression() > 90) {
                    presentGuess();
                } else {
                    presentNextQuestion();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void answerGuess(byte answer) {
            try {
                if (answer == 0) {
                    client.newCall(new Request.Builder().get()
                            .url(Strings.buildQueryUrl(CHOICE_URL, "session", session, "signature", signature, "step",
                                    str(stepInfo.getStepNum()), "element", guess.getId()))
                            .build()).execute().body().close();
                    onFinish.run();
                } else if (answer == 1) {
                    client.newCall(new Request.Builder().get()
                            .url(Strings.buildQueryUrl(EXCLUSION_URL, "session", session, "signature", signature,
                                    "step", str(stepInfo.getStepNum()), "forward_answer", Byte.toString(answer)))
                            .build()).execute().body().close();

                    lastQuestionWasGuess = false;
                    presentNextQuestion();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private class StepInfo {
            private String signature = "";
            private String session = "";
            private final String question;
            private final int stepNum;
            private final double progression;

            StepInfo(JSONObject json) {
                JSONObject params = json.getJSONObject("parameters");
                JSONObject info = params.has("step_information") ? params.getJSONObject("step_information") : params;
                question = info.getString("question");
                stepNum = info.getInt("step");
                progression = info.getDouble("progression");

                JSONObject identification = params.optJSONObject("identification");
                if (identification != null) {
                    signature = identification.getString("signature");
                    session = identification.getString("session");
                }
            }

            String getQuestion() {
                return question;
            }

            int getStepNum() {
                return stepNum;
            }

            String getSignature() {
                return signature;
            }

            String getSession() {
                return session;
            }

            double getProgression() {
                return progression;
            }
        }

        private class Guess {
            private final String id;
            private final String name;
            private final String desc;
            private final int ranking;
            private final String pseudo;
            private final String imgPath;

            Guess() throws IOException {
                JSONObject json = new JSONObject(client
                        .newCall(
                                new Request.Builder().get()
                                        .url(Strings.buildQueryUrl(GET_GUESS_URL, "session", session, "signature",
                                                signature, "step", str(stepInfo.getStepNum())))
                                        .build())
                        .execute().body().string());

                JSONObject character = json.getJSONObject("parameters").getJSONArray("elements").getJSONObject(0)
                        .getJSONObject("element");

                id = character.getString("id");
                name = character.getString("name");
                desc = character.getString("description");
                ranking = character.getInt("ranking");
                pseudo = character.getString("pseudo");
                imgPath = character.getString("absolute_picture_path");
            }

            public String getDesc() {
                return desc;
            }

            String getImgPath() {
                return imgPath;
            }

            public String getName() {
                return name;
            }

            public String getPseudo() {
                return pseudo;
            }

            public int getRanking() {
                return ranking;
            }

            String getId() {
                return id;
            }

            @Override
            public String toString() {
                return "**" + name + "**\n" + desc + '\n' + "Ranking as **#" + ranking + "**";
            }
        }
    }
}
