package com.khronodragon.bluestone.sql;

import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "admins")
public class BotAdmin {
    @Id
    @Column(nullable = false, unique = true)
    private long userId;

    @ColumnDefault("")
    @Column(length = 32)
    private String lastUsername;

    public long getUserId() {
        return userId;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public BotAdmin() {}

    public BotAdmin(long uid, String uname) {
        userId = uid;
        lastUsername = uname;
    }
}
