package net.sf.dbinit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang.Validate;

public class DBInitScriptAction implements DBInitAction {
	
	private final String path;
	
	public DBInitScriptAction(String path) {
		Validate.notEmpty(path, "The path to the script must not be null or blank");
		this.path = path;
	}

	// FIXME Uses the DBInit instance for execution
	@Override
	public void run(Connection connection) throws SQLException {
		// Gets the SQL
		String sql = DBInit.readResource(path);
		// Splits the statements
		DBStatements statements = DBInit.readStatements(sql);
		// Gets the default section
		DBSection defaultSection = statements.getDefaultSection();
		// Applies the update
		Statement st = connection.createStatement();
		try {
			// Executes all statements
			for(String sqlStatement : defaultSection.getStatements()) {
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
