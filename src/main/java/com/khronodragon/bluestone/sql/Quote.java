package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.SimpleDateFormat;
import java.util.Date;

@DatabaseTable(tableName = "quotes")
public class Quote {
    private static final String QUOTE_FORMAT = "**[%d]** %s\"%s\"%s — `%s` \u2022 %s";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, 'yy");

    @DatabaseField(generatedId = true, canBeNull = false, width = 4, index = true)
    public int id;

    @DatabaseField(width = 360, canBeNull = false)
    public String quote;

    @DatabaseField(canBeNull = false)
    public Date date;

    @DatabaseField(canBeNull = false, index = true)
    public long authorId;

    @DatabaseField(canBeNull = false, width = 32, defaultValue = "Someone")
    public String authorName;

    @DatabaseField(canBeNull = false)
    public long quotedById = 0;

    public Quote() {}

    public Quote(String quote, long authorId, String authorName) {
        this.quote = quote;
        this.authorId = authorId;
        this.authorName = authorName;
        this.date = new Date();
    }

    public String render() {
        String italicKey = quote.indexOf('*') == -1 ? "*" : "";

        return String.format(QUOTE_FORMAT,
                id, italicKey, quote, italicKey, authorName, DATE_FORMAT.format(date));
    }
}
