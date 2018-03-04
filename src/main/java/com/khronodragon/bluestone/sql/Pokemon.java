package com.khronodragon.bluestone.sql;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "pokemon")
public class Pokemon {
    @DatabaseField(id = true, canBeNull = false)
    public short id;

    @DatabaseField(index = true, width = 24, canBeNull = false)
    public String name;

    @DatabaseField(index = true, width = 3, canBeNull = false)
    public short hp;

    @DatabaseField(index = true, width = 3, canBeNull = false)
    public short atk;

    @DatabaseField(index = true, width = 3, canBeNull = false)
    public short def;

    @DatabaseField(width = 3, canBeNull = false)
    public short sp_atk;

    @DatabaseField(width = 3, canBeNull = false)
    public short sp_def;

    @DatabaseField(width = 3, canBeNull = false)
    public short speed;

    @DatabaseField(width = 28, canBeNull = false)
    public String types;

    @DatabaseField(width = 250, canBeNull = false)
    public String description;
}
