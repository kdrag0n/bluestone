package com.kdrag0n.bluestone.sql;

import com.j256.ormlite.db.MysqlDatabaseType;
import com.j256.ormlite.field.FieldType;

public class MySQLDatabaseType extends MysqlDatabaseType {
    private static final String DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver";

    public MySQLDatabaseType() {
        setCreateTableSuffix("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    @Override
    protected String getDriverClassName() {
        return DRIVER_CLASS_NAME;
    }

    @Override
    protected void appendBooleanType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
        sb.append("BOOLEAN"); // because TINYINT(1) is ugly
    }

    @Override
    public boolean isCreateIndexIfNotExistsSupported() {
        return true;
    }
}
