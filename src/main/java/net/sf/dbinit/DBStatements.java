package net.sf.dbinit;

import java.util.HashMap;
import java.util.Map;

public class DBStatements {

	private final Map<String, DBSection> sections = new HashMap<String, DBSection>();

	public void addSection(DBSection section) {
		sections.put(section.getName(), section);
	}

	public DBSection getDefaultSection() {
		DBSection section = sections.get(DBSection.SECTION_DEFAULT);
		if (section != null) {
			return section;
		} else {
			return DBSection.createDefault();
		}
	}

	public DBSection getSection(String name) {
		return sections.get(name);
	}

}
