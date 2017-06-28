package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import static java.text.MessageFormat.format;

@DatabaseTable(tableName = "quotes")
public class Quote {
    public static final String QUOTE_FORMAT = "[{0}] {4}\"{1}\"{4} — `{2}` ({3,date})";

    @DatabaseField(generatedId = true, canBeNull = false, width = 4)
    private int id;

    @DatabaseField(width = 360, canBeNull = false)
    private String quote;

    @DatabaseField(canBeNull = false)
    private Date date;

    @DatabaseField(canBeNull = false, index = true)
    private long authorId;

    @DatabaseField(canBeNull = false, width = 32)
    private String authorName;

    public int getId() {
        return id;
    }

    public String getQuote() {
        return quote;
    }

    public Date getDate() {
        return date;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public Quote() {}

    public Quote(String quote, long authorId, String authorName) {
        this.quote = quote;
        this.authorId = authorId;
        this.authorName = authorName;
        this.date = new Date();
    }

    public String render() {
        char italicKey = quote.indexOf('*') == -1 ? '*' : '\00';

        return format(QUOTE_FORMAT, id, quote, authorName, date, italicKey);
    }
}
