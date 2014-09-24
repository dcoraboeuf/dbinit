package net.sf.dbinit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestDBPatchAction implements DBPatchAction {

    private boolean applied = false;

    @Override
    public boolean appliesTo(int patch) {
        return patch == 2;
    }

    @Override
    public void apply(Connection connection, int patch) throws Exception {
        PreparedStatement ps = connection.prepareStatement("SELECT * FROM PROJECT", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        try {
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String url = "uri:" + name;
                    rs.updateString("url", url);
                    rs.updateRow();
                }
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
        }
        applied = true;
    }

    public boolean isApplied() {
        return applied;
    }
}
