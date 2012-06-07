package net.sf.dbinit;

public class DBInitCannotGetResourceException extends RuntimeException {

	private final String path;

	public DBInitCannotGetResourceException(String path) {
		super(String.format("Cannot find resource at %s", path));
		this.path = path;
	}

	public String getPath() {
		return path;
	}

}
