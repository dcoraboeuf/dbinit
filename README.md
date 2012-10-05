dbinit
======

DBInit is a Java service API that allows the incremental update of a database at the start-up of an application (web, stand-alone, etc.). It works with H2 but should work as well with other databases.

# Quick Start

DBInit expects:
* an initialisation script for the database
* an optional list of update scripts
* a DBInit call

# Initialisation script

A SQL script must be available on the classpath. It may contain any SQL code, but following declaration (or similar) is mandatory:

```
 CREATE TABLE VERSION (
 	VALUE INTEGER NOT NULL,
 	UPDATE_DATE TIMESTAMP NOT NULL
 );
```

The `VERSION` table will contain the update information.

# Update scripts

The update script can have any name, but must contain a patch number in them, starting from 1. For example: `update.1.sql`, `update.2.sql`, ...

# Initialisation

The `DBInit` class must be initialised with some properties:

<table>
	<tr>
		<th>Property</th>
		<th>Type</th>
		<th>Scope</th>
		<th>Description</th>
	</tr>
	<tr>
		<td>properties</td>
		<td>Properties</td>
		<td>Optional</td>
		<td>Additional configuration properties (see below)</td>
	</tr>
	<tr>
		<td>sqlAtShutdown</td>
		<td>String</td>
		<td>Optional</td>
		<td>Piece of SQL code to execute at JVM shutdown</td>
	</tr>
	<tr>
		<td>resourceInitialization</td>
		<td>String</td>
		<td>Required</td>
		<td>Resource path to the initialisation SQL code</td>
	</tr>
	<tr>
		<td>resourceUpdate</td>
		<td>String</td>
		<td>Required</td>
		<td>Resource pattern path to the update scripts. The path must include {0} for the patch number placeholder.</td>
	</tr>
	<tr>
		<td>version</td>
		<td>int</td>
		<td>Required</td>
		<td>Version number. It starts from 0, for a database that does not need to be patched. It must then incremented by 1 units.</td>
	</tr>
	<tr>
		<td>versionTable</td>
		<td>String</td>
		<td>Required</td>
		<td>Name of the table that contains the version information (VERSION in the samples)</td>
	</tr>
	<tr>
		<td>versionColumnName</td>
		<td>String</td>
		<td>Required</td>
		<td>Column that contains the version information (VALUE in the samples)</td>
	</tr>
	<tr>
		<td>versionColumnTimestamp</td>
		<td>String</td>
		<td>Required</td>
		<td>Column that contains the version timestamp(UPDATE_DATE in the samples)</td>
	</tr>
	<tr>
		<td>jdbcDriver</td>
		<td>String</td>
		<td>Optional*</td>
		<td>Qualified class name of the JDBC driver to use</td>
	</tr>
	<tr>
		<td>jdbcURL</td>
		<td>String</td>
		<td>Optional*</td>
		<td>JDBC URL</td>
	</tr>
	<tr>
		<td>jdbcUser</td>
		<td>String</td>
		<td>Optional*</td>
		<td>JDBC connection user</td>
	</tr>
	<tr>
		<td>jdbcPassword</td>
		<td>String</td>
		<td>Optional*</td>
		<td>JDBC connection password</td>
	</tr>
	<tr>
		<td>jdbcDataSource</td>
		<td>DataSource</td>
		<td>Optional*</td>
		<td>JDBC datasource</td>
	</tr>
</table>

(* either the datasource or the full driver/url/user/password is expected)

and called:

```
init.run();
```

For example, this may be achieved by declaring `DBInit` as a singleton in [http://www.springframework.org Spring]:

```
<bean class="net.sf.dbinit.DBInit" init-method="run">
	<property name="version" value="11" />
	<property name="jdbcDataSource" ref="DataSource" />
	<property name="versionTable" value="VERSION" />
	<property name="versionColumnName" value="VALUE" />
	<property name="versionColumnTimestamp" value="UPDATE_DATE" />
	<property name="resourceInitialization" value="/path/db/init.sql" />
	<property name="resourceUpdate" value="/path/db/update.{0}.sql" />
</bean>
```

# Rollback

By default, if the execution of a patch fails, the corresponding exception is thrown and the database remains in an indeterminate
state: the new version of the database is the number of the last successful patch but the patch in error may have been only partially
applied.

In a patch one, one can declare a `rollback` section that will be executed only when the normal execution of the patch has failed. Such a section is
introduced as follows:

```
...
SQL statements to execute normally
...
-- @rollback
...
SQL statements to execute when rolling back the changes
...
```

# Profiles

The SQL syntax may differ from one database to the other. One can use profiles in order to specify different sets of statements to be executed.

A set of statements is introduced using the same syntax than for the `rollback`:

```
...
SQL statements to execute by default
...
-- @myprofile
...
SQL statements to execute when 'myprofile' is active
...
```

The profile is activated using either:
* the `DBInit.setProperties(...)`
* the `dbinit.profile` system property

Rollback sections can also be enabled at profile level if needed:

```
...
SQL statements to execute by default
...
-- @myprofile
...
SQL statements to execute when 'myprofile' is active
...
-- @rollback
...
SQL statements to execute when rolling back the changes when no profile is defined
...
-- @myprofile-rollback
...
SQL statements to execute when rolling back the changes when 'myprofile' is active
...
```

Several profiles may be defined per file.
