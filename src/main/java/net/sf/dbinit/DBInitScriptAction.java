package net.sf.dbinit;

import org.apache.commons.lang3.Validate;

import java.sql.Connection;
import java.sql.SQLException;

public class DBInitScriptAction implements DBInitAction {

    private final String path;

    public DBInitScriptAction(String path) {
        Validate.notEmpty(path, "The path to the script must not be null or blank");
        this.path = path;
    }

    @Override
    public void run(DBExecutor executor, Connection connection) throws SQLException {
        executor.runScript(connection, path);
    }

    @Override
    public String toString() {
        return String.format("Script [%s]", path);
    }

}
