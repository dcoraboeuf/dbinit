package net.sf.dbinit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.lang.Validate;

public class DBInitScriptAction implements DBInitAction {
	
	private final String path;
	
	public DBInitScriptAction(String path) {
		Validate.notEmpty(path, "The path to the script must not be null or blank");
		this.path = path;
	}

	@Override
	public void run(Connection connection) throws SQLException {
		// Gets the SQL
		String sql = DBInit.readResource(path);
		// Splits the statements
		List<String> statements = DBInit.splitStatements(sql);
		// Applies the update
		Statement st = connection.createStatement();
		try {
			// Executes all statements
			for(String sqlStatement : statements) {
				st.execute(sqlStatement);
			}
		} finally {
			st.close();
		}
	}
	
	@Override
	public String toString() {
		return String.format("Script [%s]", path);
	}

}
