package net.sf.dbinit;

import java.sql.Connection;
import java.sql.SQLException;

public interface DBInitAction {

	void run(DBExecutor executor, Connection connection) throws SQLException;

}
