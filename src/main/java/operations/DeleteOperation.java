package operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import utility.InputValidator;

public class DeleteOperation {
	public static int deleteRecord(Connection con, String tableName, List<String> primaryKeyColumns,
			List<String> primaryKeyValues) throws SQLException {
		if (primaryKeyColumns.isEmpty() || primaryKeyColumns.size() != primaryKeyValues.size()) {
			throw new IllegalArgumentException("Primārās atslēgas lauki vai vērtības nav derīgas.");
		}
		List<String> validatedValues = InputValidator.validateValues(con, tableName, primaryKeyColumns, primaryKeyValues);
		StringBuilder sql = new StringBuilder("DELETE FROM ").append(InputValidator.quoteIdentifier(tableName))
				.append(" WHERE ");
		for (int i = 0; i < primaryKeyColumns.size(); i++) {
			if (i > 0) {
				sql.append(" AND ");
			}
			sql.append(InputValidator.quoteIdentifier(primaryKeyColumns.get(i))).append(" = ?");
		}

		try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
			for (int i = 0; i < validatedValues.size(); i++) {
				ps.setObject(i + 1, validatedValues.get(i));
			}
			return ps.executeUpdate();
		}
	}
}
