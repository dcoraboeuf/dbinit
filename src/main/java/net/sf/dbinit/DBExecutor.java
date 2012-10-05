package net.sf.dbinit;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBExecutor {

	boolean runScript(Connection connection, String path) throws SQLException;

}
