package net.sf.dbinit;


public class DBInitDriverNotFoundException extends RuntimeException {

	private final String driver;

	public DBInitDriverNotFoundException(String driver, ClassNotFoundException ex) {
		super(String.format("Cannot find JDBC driver %s", driver), ex);
		this.driver = driver;
	}

	public String getPath() {
		return driver;
	}

}
