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

||properties||Properties||Optional||Additional configuration properties (see below)||
||sqlAtShutdown||String||Optional||Piece of SQL code to execute at JVM shutdown||
||resourceInitialization||String||Required||Resource path to the initialisation SQL code||
||resourceUpdate||String||Required||Resource pattern path to the update scripts. The path must include {0} for the patch number placeholder.||
||version||int||Required||Version number. It starts from 0, for a database that does not need to be patched. It must then incremented by 1 units.||
||versionTable||String||Required||Name of the table that contains the version information (VERSION in the samples)||
||versionColumnName||String||Required||Column that contains the version information (VALUE in the samples)||
||versionColumnTimestamp||String||Required||Column that contains the version timestamp(UPDATE_DATE in the samples)||
||jdbcDriver||String||Optional*||Qualified class name of the JDBC driver to use||
||jdbcURL||String||Optional*||JDBC URL||
||jdbcUser||String||Optional*||JDBC connection user||
||jdbcPassword||String||Optional*||JDBC connection password||
||jdbcDataSource||!DataSource||Optional*||JDBC datasource||

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

=== Profiles ===

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
