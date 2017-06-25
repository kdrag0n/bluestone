package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.Context;
import com.khronodragon.bluestone.annotations.Command;
import com.khronodragon.bluestone.annotations.Cooldown;
import com.khronodragon.bluestone.enums.BucketType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.List;

public class KewlCog extends Cog {
    private static final Logger logger = LogManager.getLogger(KewlCog.class);
    private static final Language language = new AmericanEnglish();
    private final ThreadLocal<JLanguageTool> langTool = ThreadLocal.withInitial(() -> {
        logger.info("Creating new LanguageTool instance...");
        return new JLanguageTool(language);
    });

    public KewlCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Kewl Stuff";
    }

    public String getDescription() {
        return "All the kewl extensions belong here.";
    }

    @Cooldown(scope = BucketType.USER, delay = 5)
    @Command(name = "correct", desc = "Correct spelling in some text.", thread = true)
    public void cmdSpellcheck(Context ctx) throws IOException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(":warning: I need something to correct!").queue();
            return;
        }
        final String text = ctx.rawArgs;
        StringBuilder result = new StringBuilder(text);

        List<RuleMatch> matches = langTool.get().check(text);
        for (RuleMatch match: matches) {
            if (match.getSuggestedReplacements().size() > 0)
                result.replace(match.getFromPos(), match.getToPos(), match.getSuggestedReplacements().get(0));
        }

        ctx.send("Result: `" + result + "`").queue();
    }
}
