package com.kdrag0n.bluestone.cogs;

public class LuckCog extends com.kdrag0n.bluestone.Cog {
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

    public LuckCog(com.kdrag0n.bluestone.Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Luck";
    }

    public String getDescription() {
        return "Take your chances with this cog.";
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "choose", desc = "Choose between the given choices.", aliases = {"choice"})
    public void cmdChoice(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.length > 1) {
            ctx.send("I choose **" + randomChoice(ctx.args.array) + "**").queue();
        } else {
            ctx.fail("You need at least 2 choices!");
        }
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "flip", desc = "Flip a coin.", aliases = {"coinflip"})
    public void cmdFlip(com.kdrag0n.bluestone.Context ctx) {
        ctx.send("The coin toss revealed... **" + (randint(0, 1) == 1 ? "heads" : "tails") + "**!").queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "roll", desc = "Roll a virtual die.", aliases = {"dice"})
    public void cmdRoll(com.kdrag0n.bluestone.Context ctx) {
        ctx.send("I rolled a **" + randint(1, 7) + "**.").queue();
    }

    @com.kdrag0n.bluestone.annotations.Command(name = "8ball", desc = "A magic 8 ball!", aliases = {"8"})
    public void cmd8Ball(com.kdrag0n.bluestone.Context ctx) {
        if (ctx.args.empty)
            ctx.fail("I need a question!");
        else
            ctx.send("🔮 " + randomChoice(EIGHT_BALL_CHOICES)).queue();
    }
}
