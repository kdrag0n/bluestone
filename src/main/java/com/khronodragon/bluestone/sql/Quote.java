package com.khronodragon.bluestone.sql;

import javax.persistence.*;
import java.util.Date;

import static java.text.MessageFormat.format;

@Entity
@Table(name = "quotes", indexes = {@Index(columnList = "authorId")})
public class Quote {
    public static final String QUOTE_FORMAT = "**[{0}]** {4}\"{1}\"{4} — `{2}` (__{3,date}__)";

    @Id
    @GeneratedValue
    @Column(length = 4, nullable = false)
    private int id;

    @Column(length = 360, nullable = false)
    private String quote;

    @Column(nullable = false)
    private Date date;

    @Column(nullable = false)
    private long authorId;

    @Column(length = 32, nullable = false)
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
