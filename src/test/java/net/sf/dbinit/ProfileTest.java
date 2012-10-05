package net.sf.dbinit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link DBInit}.
 * @author Damien Coraboeuf
 */
public class ProfileTest {

	private static final String DIR_DB = "target/dbinit/profile";

	private static final String FILE_DB = DIR_DB + "/profile";

	private static final String JDBC_URL = "jdbc:h2:file:" + FILE_DB;

	private DBInit db;

	@Before
	public void before() throws IOException {
		// Clean-up
		File dir = new File(DIR_DB);
		if (dir.exists()) {
			FileUtils.forceDelete(dir);
		}
		// General initialisation
		db = new DBInit();
		db.setJdbcDriver("org.h2.Driver");
		db.setJdbcUser("SA");
		db.setJdbcPassword("");
		db.setJdbcURL(JDBC_URL);
		db.setResourceInitialization("/dbinit/profile/init.sql");
		db.setResourceUpdate("/dbinit/profile/update.{0}.sql");
		db.setVersionTable("VERSION");
		db.setVersionColumnName("value");
		db.setVersionColumnTimestamp("value_date");
	}

	@Test
	public void profile() throws SQLException {
		System.setProperty(DBInit.SYSTEM_PROFILE, "other");
		db.setVersion(0);
		db.run();
		// Checks
		Connection c = DriverManager.getConnection(JDBC_URL, "SA", "");
		try {
			// Checks the default table is not created
			{
				ResultSet rs = c.getMetaData().getTables(null, null, "PROJECT", null);
				try {
					assertFalse (rs.next());
				} finally {
					rs.close();
				}
			}
			// Checks the 'other' table is created
			{
				ResultSet rs = c.getMetaData().getTables(null, null, "XPROJECT", null);
				try {
					assertTrue (rs.next());
				} finally {
					rs.close();
				}
			}
		} finally {
			c.close();
		}
	}

}
