package net.sf.dbinit;

import java.sql.SQLException;

public class DBInitSQLException extends RuntimeException {

	private final String operation;

	public DBInitSQLException(String operation, SQLException ex) {
		super(String.format("SQL error while executing \"%s\" operation", operation), ex);
		this.operation = operation;
	}

	public String getOperation() {
		return operation;
	}

}
