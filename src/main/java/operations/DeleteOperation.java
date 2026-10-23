package operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteOperation {
	public static int deleteRecord(Connection con, String tableName, String pkColumn, String pkValue) throws SQLException{
		String sql = "DELETE FROM " + tableName + " WHERE " + pkColumn + " = ?";
		
		try(PreparedStatement ps = con.prepareStatement(sql)){
			ps.setString(1, pkValue);
			
			return ps.executeUpdate();
		}
	}
}
