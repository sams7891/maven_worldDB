package operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class InsertOperation {
	public static void insertRecord(Connection con, String tableName, List<String> columns, List<String> values) throws SQLException{
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
		
		for(int i = 0; i < columns.size(); i++) {
			sql.append(columns.get(i));
			
			if(i < columns.size() - 1)
				sql.append(", ");
		}
		
		sql.append(") VALUES (");
		
		for(int i = 0; i < values.size(); i++) {
			sql.append(i == 0 ? "?" : ", ?");
		}
		
		sql.append(")");
		
		try(PreparedStatement ps = con.prepareStatement(sql.toString())){
			for(int i = 0; i < values.size(); i++) {
				ps.setString(i + 1, values.get(i));
			}
			ps.executeUpdate();
		}
	}
}
