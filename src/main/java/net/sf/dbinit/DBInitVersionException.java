package net.sf.dbinit;

public class DBInitVersionException extends RuntimeException {

	private final int currentVersion;
	private final int targetVersion;

	public DBInitVersionException(int currentVersion, int targetVersion) {
		super(String.format("Current version %d is greater than target version %d", currentVersion, targetVersion));
		this.currentVersion = currentVersion;
		this.targetVersion = targetVersion;
	}

	public int getCurrentVersion() {
		return currentVersion;
	}

	public int getTargetVersion() {
		return targetVersion;
	}

}
