package net.sf.dbinit;

import java.io.IOException;

public class DBInitCannotSplitStatementsException extends RuntimeException {

	private final String sql;

	public DBInitCannotSplitStatementsException(String sql, IOException ex) {
		super(String.format("Cannot split statements for SQL=[%s]", sql), ex);
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}

}
