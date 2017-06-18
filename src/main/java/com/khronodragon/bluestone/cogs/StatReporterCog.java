package com.khronodragon.bluestone.cogs;

import com.khronodragon.bluestone.Bot;
import com.khronodragon.bluestone.Cog;
import com.khronodragon.bluestone.EventedCog;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;

public class StatReporterCog extends Cog implements EventedCog {
    public StatReporterCog(Bot bot) {
        super(bot);
    }

    public String getName() {
        return "Statistic Reporter";
    }

    public String getDescription() {
        return "A cog to report bot stats to services like Discord Bots and Carbonitex.";
    }

    public void load() {
        super.load();
        report();
    }

    public void onReady(ReadyEvent event) {
        report();
    }

    public void onGuildJoin(GuildJoinEvent event) {
        report();
    }

    public void onGuildLeave(GuildLeaveEvent event) {
        report();
    }

    public void report() {

    }
}
