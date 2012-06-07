package net.sf.dbinit;

public class DBInitPatchException extends RuntimeException {

	private final int patch;

	public DBInitPatchException(int patch, Exception cause) {
		super(String.format("Cannot apply patch %d", patch), cause);
		this.patch = patch;
	}

	public int getPatch() {
		return patch;
	}

}
