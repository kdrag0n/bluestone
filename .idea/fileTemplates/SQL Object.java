#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "${NAME}")
public class ${NAME} {
    @DatabaseField(id = true, canBeNull = false)
    private long id;
    
    public ${NAME}() {}
    
    public ${NAME}(long id) {
        this.id = id;
    }
}
