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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KewlCog extends Cog {
    private static final Logger logger = LogManager.getLogger(KewlCog.class);
    private static final Pattern DATE_WEEKDAY_PATTERN = Pattern.compile("^The date [0-9 a-zA-Z]+ is not a ([MTWFS][a-z]+), but a ([MTWFS][a-z]+)\\.$");
    private static final Language language = new AmericanEnglish();
    private final JLanguageTool langTool = new JLanguageTool(language);

    public KewlCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Kewl";
    }

    @Override
    public String getCosmeticName() {
        return "Kewl Stuff";
    }

    public String getDescription() {
        return "All the kewl extensions belong here.";
    }

    @Cooldown(scope = BucketType.USER, delay = 5)
    @Command(name = "correct", desc = "Correct spelling in some text.", thread = true)
    public void cmdSpellcheck(Context ctx) throws IOException {
        if (ctx.rawArgs.length() < 1) {
            ctx.send(Emotes.getFailure() + ' ' + "I need something to correct!").queue();
            return;
        }
        ctx.channel.sendTyping().queue();

        final String text = ctx.rawArgs;
        StringBuilder result = new StringBuilder(text);

        List<RuleMatch> matches;
        synchronized (langTool) {
            matches = langTool.check(text);
        }
        Collections.reverse(matches);

        for (RuleMatch match: matches) {
            if (match.getSuggestedReplacements().size() > 0) {
                result.replace(match.getFromPos(), match.getToPos(), match.getSuggestedReplacements().get(0));
            } else if (match.getRule().getId().equals("DATE_WEEKDAY")) {
                Matcher m = DATE_WEEKDAY_PATTERN.matcher(match.getMessage());
                if (!m.find()) continue;

                String wrongWeekday = m.group(1);
                String correctWeekday = m.group(2);

                result.replace(match.getFromPos(), match.getToPos(),
                        result.substring(match.getFromPos(), match.getToPos())
                                .replace(wrongWeekday, correctWeekday));
            }
        }
        String finalResult = result.toString().replace(".M..", "M.");

        ctx.send("Result: `" + finalResult + "`").queue();
    }
}
