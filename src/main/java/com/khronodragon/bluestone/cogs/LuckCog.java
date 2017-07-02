package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.Emotes;
import com.khronodragon.bluestone.annotations.Command;

public class LuckCog extends Cog {
    private static final String[] EIGHT_BALL_CHOICES = {"Yes, definitely!",
            "Of course!",
            "Yes!",
            "Probably.",
            "Hmm, I'm not sure...",
            "I'm not sure...",
            "I don't think so.",
            "Hmm, I don't really think so.",
            "Definitely not.",
            "No.",
            "Probably not.",
            "Sure!",
            "Try again later...",
            "I don't know.",
            "Maybe...",
            "Yes, of course!",
            "No, probably not."};

    public LuckCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Luck";
    }

    public String getDescription() {
        return "Take your chances with this cog.";
    }

    @Command(name = "choose", desc = "Choose between the given choices.", aliases = {"choice"})
    public void cmdChoice(Context ctx) {
        if (ctx.args.size() > 1) {
            ctx.send("I choose **" + randomChoice(ctx.args) + "**").queue();
        } else {
            ctx.send(Emotes.getFailure() + " You need at least 2 choices!").queue();
        }
    }

    @Command(name = "flip", desc = "Flip a coin.", aliases = {"coinflip"})
    public void cmdFlip(Context ctx) {
        ctx.send("The coin toss revealed... **" + (randint(0, 1) == 1 ? "heads" : "tails") + "**!").queue();
    }

    @Command(name = "roll", desc = "Roll a virtual die.", aliases = {"dice"})
    public void cmdRoll(Context ctx) {
        ctx.send("I rolled a **" + randint(1, 7) + "**.").queue();
    }

    @Command(name = "8ball", desc = "A magic 8 ball!", aliases = {"8"})
    public void cmd8Ball(Context ctx) {
        ctx.send(":crystal_ball: " + randomChoice(EIGHT_BALL_CHOICES)).queue();
    }
}
