package net.sf.dbinit;

import java.sql.Connection;

/**
 * This interface defines a component that can run actions when
 * applying a patch.
 */
public interface DBPatchAction {

    /**
     * Can this action be applied for a patch number?
     *
     * @param patch Patch number to apply this action for
     * @return true if the patch action must be run
     */
    boolean appliesTo(int patch);

    /**
     * Applies the action
     *
     * @param connection Connection to use if needed
     * @param patch      Patch which is applied
     * @throws Exception In case of problem
     */
    void apply(Connection connection, int patch) throws Exception;
}
