package net.sf.dbinit;

import java.util.ArrayList;
import java.util.List;

public class DBSection {

	/**
	 * Name of the default section.
	 */
	public static final String SECTION_DEFAULT = "default";

	public static DBSection createDefault() {
		return new DBSection(SECTION_DEFAULT);
	}

	private final String name;
	private final List<String> statements = new ArrayList<String>();

	public DBSection(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addStatement(String statement) {
		statements.add(statement);
	}

	public List<String> getStatements() {
		return statements;
	}

}
