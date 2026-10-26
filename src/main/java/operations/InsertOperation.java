package operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import utility.InputValidator;

public class InsertOperation {
	public static void insertRecord(Connection con, String tableName, List<String> columns, List<String> values) throws SQLException{
		if (columns.size() != values.size() || columns.isEmpty()) {
			throw new IllegalArgumentException("Nav norādīti ievietojamie lauki vai vērtības.");
		}
		List<String> validatedValues = InputValidator.validateValues(con, tableName, columns, values);
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(InputValidator.quoteIdentifier(tableName)).append(" (");
		
		for(int i = 0; i < columns.size(); i++) {
			sql.append(InputValidator.quoteIdentifier(columns.get(i)));
			
			if(i < columns.size() - 1)
				sql.append(", ");
		}
		
		sql.append(") VALUES (");
		
		for(int i = 0; i < values.size(); i++) {
			sql.append(i == 0 ? "?" : ", ?");
		}
		
		sql.append(")");
		
		try(PreparedStatement ps = con.prepareStatement(sql.toString())){
			for(int i = 0; i < validatedValues.size(); i++) {
				ps.setObject(i + 1, validatedValues.get(i));
			}
			ps.executeUpdate();
		}
	}
}
