package net.sf.dbinit;

import java.io.IOException;

public class DBInitCannotReadResourceException extends RuntimeException {

	private final String path;

	public DBInitCannotReadResourceException(String path, IOException ex) {
		super(String.format("Cannot read resource at %s", path), ex);
		this.path = path;
	}

	public String getPath() {
		return path;
	}

}
