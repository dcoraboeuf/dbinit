package net.sf.dbinit;

/**
 * List the behaviours to follow when an error occurs in a script.
 * 
 * @author Damien Coraboeuf
 * 
 */
public enum ScriptErrorLevel {

	/**
	 * Ignore the error
	 */
	IGNORE,

	/**
	 * Logs the error
	 */
	LOG,

	/**
	 * Throws the error (default)
	 */
	THROWS;

}
