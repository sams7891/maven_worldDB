package operations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class ViewManager {

	public static List<String> getDatabaseViews(Connection con) throws SQLException {
		List<String> views = new ArrayList<>();
		java.sql.DatabaseMetaData meta = con.getMetaData();
		
		try(ResultSet rs = meta.getTables(con.getCatalog(), null, "%", new String[] {"VIEW"})){
			while(rs.next()) {
				views.add(rs.getString("TABLE_NAME"));
			}
		}
		return views;
	}

}
