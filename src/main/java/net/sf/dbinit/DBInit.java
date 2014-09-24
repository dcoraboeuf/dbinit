package net.sf.dbinit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * DB configuration.
 * @author Damien Coraboeuf
 */

/**
 * @author dcoraboe
 */
public class DBInit implements DBExecutor, Runnable {

    /**
     * Task which is executed at shutdwon.
     *
     * @author Damien Coraboeuf
     */
    protected class ShutdownTask implements Runnable {

        @Override
        public void run() {
            try {
                // Get a connection
                Connection connection = getConnection();
                try {
                    log.info("Executing " + sqlAtShutdown + " at shutdown");
                    connection.createStatement().execute(sqlAtShutdown);
                } finally {
                    connection.close();
                }
            } catch (Exception th) {
                log.error("Cannot execute SQL at shutdown", th);
            }
        }
    }

    /**
     * System property to set in order to define a profile. Value is {@value}.
     */
    public static final String SYSTEM_PROFILE = "dbinit.profile";

    /**
     * Rollback section
     */
    private static final String SECTION_ROLLBACK = "rollback";

    /**
     * Logger
     */
    private static final Logger log = LoggerFactory.getLogger(DBInit.class);

    /**
     * Reads resource as a string
     *
     * @param path Resource path
     * @return Resource content as a string
     */
    public static String readResource(String path) {
        if (StringUtils.isBlank(path)) {
            throw new DBInitCannotGetResourceException(path);
        } else {
            InputStream in = DBInit.class.getResourceAsStream(path);
            if (in == null) {
                // Tries with a file
                File file = new File(path);
                if (file.exists()) {
                    try {
                        in = new FileInputStream(file);
                    } catch (IOException ex) {
                        throw new DBInitCannotReadResourceException(path, ex);
                    }
                }
                // Not found
                else {
                    throw new DBInitCannotGetResourceException(path);
                }
            }
            try {
                try {
                    String text = IOUtils.toString(in);
                    return text;
                } finally {
                    in.close();
                }
            } catch (IOException ex) {
                throw new DBInitCannotReadResourceException(path, ex);
            }
        }
    }

    /**
     * Parses a query
     *
     * @param query Query to parse
     * @return Extracted parameters. The returned map can be empty but never <code>null</code>.
     */
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<String, String>();
        if (StringUtils.isNotBlank(query)) {
            String[] tokens = StringUtils.split(query, "&");
            for (String token : tokens) {
                String[] pair = StringUtils.split(token, "=");
                if (pair.length == 1) {
                    params.put(pair[0], pair[0]);
                } else if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        // Ok
        return params;
    }

    /**
     * Runs one script which is found using a resource path
     *
     * @param connection Connection to use
     * @param scriptPath Resource path to the script
     * @return <code>true</code> if the script was applied successfully, <code>false</code> if there was an error and the script was rolled back
     * @throws SQLException If an error occurs while executing the script
     * @see #readResource(String)
     */
    @Override
    public boolean runScript(Connection connection, String scriptPath) throws SQLException {
        // Gets the SQL content
        String sql = readResource(scriptPath);
        // Applies the update
        Statement st = connection.createStatement();
        try {
            // Slices all statements
            DBStatements statements = readStatements(sql);
            // Gets the default section
            DBSection defaultSection = getSection(statements);
            // Executes all statements
            for (String sqlStatement : defaultSection.getStatements()) {
                try {
                    st.execute(sqlStatement);
                } catch (SQLException ex) {
                    log.debug(String.format("Looking for rollback section: %s", SECTION_ROLLBACK));
                    // Performs a normal rollback
                    connection.rollback();
                    // Gets a rollback section
                    DBSection rollbackSection = getRollbackSection(statements);
                    if (rollbackSection != null) {
                        log.debug("Applying rollback section");
                        for (String rollbackStatement : rollbackSection.getStatements()) {
                            try {
                                st.execute(rollbackStatement);
                            } catch (SQLException rollbackException) {
                                throw new SQLException(
                                        String.format(
                                                "Could not rollback after error. Rollback exception is: %s",
                                                rollbackException),
                                        ex);
                            }
                        }
                        // Rollback done
                        log.debug("Rollback applied");
                        return false;
                    }
                    // No rollback section, throws the exception
                    else {
                        throw ex;
                    }
                }
            }
            // OK
            return true;
        } finally {
            st.close();
        }
    }


    /**
     * Gets the section to execute when rolling back, according to the profile.
     *
     * @param statements All sections
     * @return Section to execute
     * @see #getProfile()
     */
    protected DBSection getRollbackSection(DBStatements statements) {
        String profile = getProfile();
        if (StringUtils.isNotBlank(profile)) {
            DBSection section = statements.getSection(String.format("%s-%s", profile, SECTION_ROLLBACK));
            if (section != null) {
                return section;
            }
        }
        return statements.getSection(SECTION_ROLLBACK);
    }

    /**
     * Splits all statements
     *
     * @param sql Initial SQL file
     * @return List of SQL statements, indexed by sections
     */
    public static DBStatements readStatements(String sql) {
        DBStatements statements = new DBStatements();
        BufferedReader reader = new BufferedReader(new StringReader(sql));
        try {
            try {
                String line;
                StringBuffer statement = new StringBuffer();
                DBSection section = DBSection.createDefault();
                statements.addSection(section);
                while ((line = reader.readLine()) != null) {
                    if (StringUtils.isNotBlank(line)) {
                        // Comment
                        if (line.startsWith("--")) {
                            String commentValue = trim(substring(line, 2));
                            if (startsWith(commentValue, "@")) {
                                String sectionName = trim(substring(commentValue, 1));
                                section = new DBSection(lowerCase(sectionName));
                                statements.addSection(section);
                            }
                        }
                        // Anything else
                        else {
                            if (line.endsWith(";")) {
                                line = stripEnd(line, ";");
                                statement.append(line);
                                section.addStatement(statement.toString());
                                statement.setLength(0);
                            } else {
                                statement.append(line).append(" ");
                            }
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException ex) {
            throw new DBInitCannotSplitStatementsException(sql, ex);
        }
        return statements;
    }

    /**
     * Properties
     */
    private Properties properties;

    /**
     * SQL to execute at shutdown
     */
    private String sqlAtShutdown;

    /**
     * Resource containing the initial SQL script.
     */
    private String resourceInitialization;

    /**
     * Pattern for the resource containing the update
     */
    private String resourceUpdate;

    /**
     * Version to setup
     */
    private int version;

    /**
     * Table containing the version information
     */
    private String versionTable;

    /**
     * Column containing the version name
     */
    private String versionColumnName;

    /**
     * Column containing the version timestamp
     */
    private String versionColumnTimestamp;

    /**
     * JDBC driver class name
     */
    private String jdbcDriver;

    /**
     * JDBC URL
     */
    private String jdbcURL;

    /**
     * JDBC user name
     */
    private String jdbcUser;

    /**
     * JDBC password
     */
    private String jdbcPassword;

    /**
     * JDBC datasource
     */
    private DataSource jdbcDataSource;

    /**
     * Actions to execute BEFORE
     */
    private List<DBInitAction> preActions;

    /**
     * Actions to execute AFTER
     */
    private List<DBInitAction> postActions;

    /**
     * Actions to run when applying a patch
     */
    private List<? extends DBPatchAction> patchActions;

    /**
     * Applies one patch
     *
     * @param connection Connection to be used
     * @param patch      Patch to apply
     */
    protected void applyPatch(Connection connection, int patch) {
        log.info("Applying patch " + patch + "...");
        try {
            // Read the update
            String updatePath = MessageFormat.format(resourceUpdate, patch);
            boolean success = runScript(connection, updatePath);
            // Applying any suitable patch action
            if (patchActions != null) {
                for (DBPatchAction patchAction : patchActions) {
                    if (patchAction.appliesTo(patch)) {
                        log.info("Applying action {} for patch {}...", patchAction.getClass().getName(), patch);
                        patchAction.apply(connection, patch);
                    }
                }
            }
            // Upgrading the version after success
            if (success) {
                setVersion(connection, patch);
            }
            // Ok
            log.info("End of patch " + patch);
        } catch (Exception ex) {
            throw new DBInitPatchException(patch, ex);
        }
    }

    /**
     * Applies patches for the current version
     *
     * @param connection     Connection to be used
     * @param currentVersion Version which patches must be applied from.
     */
    protected void applyPatches(Connection connection, int currentVersion) {
        List<Integer> patchList = getPatchList(currentVersion);
        if (patchList.isEmpty()) {
            log.info("No patch is needed for version " + currentVersion);
        } else {
            log.info("List of patches to apply : " + patchList);
            // Applies all patches
            for (int patch : patchList) {
                applyPatch(connection, patch);
            }
        }
    }

    protected List<Integer> getPatchList(int currentVersion) {
        if (version > currentVersion) {
            List<Integer> patches = new ArrayList<Integer>();
            for (int patch = currentVersion + 1; patch <= version; patch++) {
                patches.add(patch);
            }
            return patches;
        } else if (version == currentVersion) {
            return Collections.emptyList();
        } else {
            throw new DBInitVersionException(currentVersion, version);
        }
    }

    /**
     * Creates the tables
     *
     * @param connection Connection to be used
     */
    protected void createTables(Connection connection) {
        try {
            Statement st = connection.createStatement();
            try {
                // Reads the batch file
                String sql = readResource(resourceInitialization);
                // Slices all statements
                DBStatements statements = readStatements(sql);
                // Gets the default section
                DBSection defaultSection = getSection(statements);
                // Executes all statements
                for (String sqlStatement : defaultSection.getStatements()) {
                    log.debug("Executing\n" + sqlStatement);
                    st.execute(sqlStatement);
                }
            } finally {
                st.close();
            }
            // Apply patch for full version
            applyPatches(connection, 0);
        } catch (SQLException ex) {
            throw new DBInitSQLException("Creation of tables", ex);
        }
    }

    /**
     * Gets the section to execute according to the profile.
     *
     * @param statements All sections
     * @return Section to execute
     * @see #getProfile()
     */
    protected DBSection getSection(DBStatements statements) {
        String profile = getProfile();
        if (StringUtils.isNotBlank(profile)) {
            DBSection section = statements.getSection(profile);
            if (section != null) {
                return section;
            }
        }
        return statements.getDefaultSection();
    }

    /**
     * Executes a list of scripts. The given attribute is used to fetch a context attribute whose value contains a
     * comma-separated list of resource paths to SQL scripts.
     *
     * @param connection    Connection to use
     * @param attributeName Attribute that contains the list of scripts
     * @throws SQLException If an error occurs while executing the scripts
     * @see #runScript(Connection, String)
     */
    protected void executeScripts(Connection connection, String attributeName) throws SQLException {
        // Executes some arbitrary scripts
        String scripts = getProperty(attributeName);
        if (StringUtils.isNotBlank(scripts)) {
            String[] listScripts = StringUtils.split(scripts, ";");
            for (String script : listScripts) {
                log.info("Executing script " + script);
                runScript(connection, script);
            }
        }
    }

    /**
     * Creates a connection.
     *
     * @return Connection to be used
     * @throws SQLException If there is problem while initializing the connection
     */
    protected Connection getConnection() throws SQLException {
        if (jdbcDataSource != null) {
            return jdbcDataSource.getConnection();
        } else {
            Connection connection = DriverManager.getConnection(jdbcURL, jdbcUser, jdbcPassword);
            return connection;
        }
    }

    /**
     * Get the current installed version
     *
     * @param connection Connection to use
     * @return Current version of the database
     * @throws SQLException If there is a problem while accessing the database
     */
    protected Integer getCurrentVersion(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT " + versionColumnName + ", "
                + versionColumnTimestamp + " FROM " + versionTable);
        try {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int name = rs.getInt(1);
                Timestamp ts = rs.getTimestamp(2);
                log.info("Last installation date is " + ts);
                return name;
            } else {
                return null;
            }
        } finally {
            ps.close();
        }
    }

    /**
     * Gets the optional data source
     *
     * @return Data source
     */
    public DataSource getJdbcDataSource() {
        return jdbcDataSource;
    }

    /**
     * @return JDBC driver to use
     */
    public String getJdbcDriver() {
        return jdbcDriver;
    }

    /**
     * @return Password for the DB user
     */
    public String getJdbcPassword() {
        return jdbcPassword;
    }

    /**
     * @return JDBC URL to the database
     */
    public String getJdbcURL() {
        return jdbcURL;
    }

    /**
     * @return Database user
     */
    public String getJdbcUser() {
        return jdbcUser;
    }

    /**
     * Returns the properties
     *
     * @return properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns a custom property
     *
     * @param name Property name
     * @return Property value or <code>null</code> if no property is found
     */
    protected String getProperty(String name) {
        if (properties != null) {
            String property = properties.getProperty(name);
            if (property != null) {
                return property;
            }
        }
        return System.getProperty(name);
    }

    /**
     * @return Resource path to the initialization script
     */
    public String getResourceInitialization() {
        return resourceInitialization;
    }

    /**
     * @return Resource path to the update script (it contains a {0} token for the version placeholder)
     */
    public String getResourceUpdate() {
        return resourceUpdate;
    }

    /**
     * Returns the SQL to execute at shutdown
     *
     * @return SQL to execute at shutdown
     */
    public String getSqlAtShutdown() {
        return sqlAtShutdown;
    }

    /**
     * @return Target version of the database
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return Name of the column that contains the version
     */
    public String getVersionColumnName() {
        return versionColumnName;
    }

    /**
     * @return Name of the column that contains the timestamp
     */
    public String getVersionColumnTimestamp() {
        return versionColumnTimestamp;
    }

    /**
     * @return Name of the table that contains the version information
     */
    public String getVersionTable() {
        return versionTable;
    }

    /**
     * Gets the profile to apply by using the {@link #SYSTEM_PROFILE} system
     * property.
     */
    protected String getProfile() {
        return getProperty(SYSTEM_PROFILE);
    }

    /**
     * Initialisation
     */
    @PostConstruct
    @Override
    public void run() {
        log.info("Checking the DB");
        try {
            // Registers the driver
            if (StringUtils.isNotBlank(jdbcDriver)) {
                try {
                    Class.forName(jdbcDriver);
                } catch (ClassNotFoundException ex) {
                    throw new DBInitDriverNotFoundException(jdbcDriver, ex);
                }
            }
            // Shutdown
            if (StringUtils.isNotBlank(sqlAtShutdown)) {
                Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownTask(), "SQL at Shutdown"));
            }
            // Get a connection
            Connection connection = getConnection();
            // Transaction
            boolean ok = false;
            try {
                try {
                    // Pre scripts
                    preActions(connection);
                    // Get the metadata
                    DatabaseMetaData metaData = connection.getMetaData();
                    // Get the list of tables
                    ResultSet tables = metaData.getTables(null, null, versionTable, null);
                    if (tables.next()) {
                        log.info("DB is already created");
                        // Get the current version
                        Integer currentVersion = getCurrentVersion(connection);
                        log.info("DB current version is " + currentVersion);
                        // Different version
                        if (currentVersion == null || !currentVersion.equals(version)) {
                            log.info("DB must be patched");
                            applyPatches(connection, currentVersion);
                            ok = true;
                        } else {
                            log.info("DB version is ok ; no change is required");
                            ok = true;
                        }
                    } else {
                        log.info("The DB must be created");
                        createTables(connection);
                        setVersion(connection, version);
                        ok = true;
                    }
                    // Post scripts
                    if (ok) {
                        postActions(connection);
                    }
                } finally {
                    // Transaction end
                    if (ok) {
                        log.info("DB update OK. Committing changes.");
                        connection.commit();
                    } else {
                        log.info("DB update went wrong. Rolling back changes (but structure updates).");
                        connection.rollback();
                    }
                }
            } finally {
                connection.close();
            }
        } catch (SQLException ex) {
            throw new DBInitSQLException("Initialisation", ex);
        }
    }

    protected void preActions(Connection connection) throws SQLException {
        log.info("Executing pre-actions");
        runActions(connection, preActions);
    }

    protected void postActions(Connection connection) throws SQLException {
        log.info("Executing post-actions");
        runActions(connection, postActions);
    }

    protected void runActions(Connection connection, List<DBInitAction> actions) throws SQLException {
        if (actions != null) {
            for (DBInitAction action : actions) {
                log.info(" - running " + action);
                action.run(this, connection);
            }
        }
    }

    /**
     * Post-initialisation
     */
    protected void postInit() {
    }

    /**
     * Pre-initialisation
     *
     * @return Result of the pre-initialisation
     */
    protected boolean preInit() {
        return true;
    }

    /**
     * Sets the data source to use
     *
     * @param jdbcDataSource Data source
     */
    public void setJdbcDataSource(DataSource jdbcDataSource) {
        this.jdbcDataSource = jdbcDataSource;
    }

    /**
     * @param jdbcDriver JDBC driver to use
     */
    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    /**
     * @param jdbcPassword Password for the DB user
     */
    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * @param jdbcURL JDBC URL to the database
     */
    public void setJdbcURL(String jdbcURL) {
        this.jdbcURL = jdbcURL;
    }

    /**
     * @param jdbcUser Database user
     */
    public void setJdbcUser(String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    /**
     * Sets the properties
     *
     * @param properties
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * @param resourceInitialization Resource path to the initialization script
     */
    public void setResourceInitialization(String resourceInitialization) {
        this.resourceInitialization = resourceInitialization;
    }

    /**
     * @param resourceUpdate Resource path to the update script (it contains a {0} token for the version placeholder)
     */
    public void setResourceUpdate(String resourceUpdate) {
        this.resourceUpdate = resourceUpdate;
    }

    /**
     * Sets the SQL to execute at shutdown
     *
     * @param sqlAtShutdown SQL to execute at shutdown
     */
    public void setSqlAtShutdown(String sqlAtShutdown) {
        this.sqlAtShutdown = sqlAtShutdown;
    }

    /**
     * Changes the version
     *
     * @param connection The connection to use.
     * @param theVersion The version to set.
     * @throws SQLException
     */
    protected void setVersion(Connection connection, int theVersion) throws SQLException {
        Statement st = connection.createStatement();
        try {
            // Delete all previous lines
            st.execute("DELETE FROM " + versionTable);
        } finally {
            st.close();
        }
        // Adds the current version
        PreparedStatement ps = connection.prepareStatement("INSERT INTO " + versionTable + " (" + versionColumnName
                + ", " + versionColumnTimestamp + ") VALUES (?, ?)");
        try {
            ps.setInt(1, theVersion);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    /**
     * @param version Target version of the database
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @param versionColumnName Name of the column that contains the version
     */
    public void setVersionColumnName(String versionColumnName) {
        this.versionColumnName = versionColumnName;
    }

    /**
     * @param versionColumnTimestamp Name of the column that contains the timestamp
     */
    public void setVersionColumnTimestamp(String versionColumnTimestamp) {
        this.versionColumnTimestamp = versionColumnTimestamp;
    }

    /**
     * @param versionTable Name of the table that contains the version information
     */
    public void setVersionTable(String versionTable) {
        this.versionTable = versionTable;
    }

    /**
     * List of actions to execute BEFORE the initialization
     *
     * @return List of actions to execute BEFORE the initialization (can be <code>null</code>)
     */
    public List<DBInitAction> getPreActions() {
        return preActions;
    }

    /**
     * Sets the list of actions to execute BEFORE the initialization
     *
     * @param preActions List of actions to execute BEFORE the initialization (can be <code>null</code>)
     */
    public void setPreActions(List<DBInitAction> preActions) {
        this.preActions = preActions;
    }

    /**
     * List of actions to execute AFTER the initialization
     *
     * @return List of actions to execute AFTER the initialization (can be <code>null</code>)
     */
    public List<DBInitAction> getPostActions() {
        return postActions;
    }

    /**
     * Sets the list of actions to execute AFTER the initialization
     *
     * @param postActions List of actions to execute AFTER the initialization (can be <code>null</code>)
     */
    public void setPostActions(List<DBInitAction> postActions) {
        this.postActions = postActions;
    }

    /**
     * List of actions to run when applying a patch.
     *
     * @return List of actions to run when applying a patch (can be <code>null</code>)
     */
    public List<? extends DBPatchAction> getPatchActions() {
        return patchActions;
    }

    /**
     * Sets the list of actions to run when applying a patch.
     *
     * @param patchActions List of actions to run when applying a patch (can be <code>null</code>)
     */
    public void setPatchActions(List<? extends DBPatchAction> patchActions) {
        this.patchActions = patchActions;
    }
}
