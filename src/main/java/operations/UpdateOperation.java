package operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import utility.InputValidator;

public class UpdateOperation {

	public static int updateRecord(Connection con, String tableName, List<String> columns, List<String> values,
			List<String> primaryKeyColumns) throws SQLException {
		if (columns.size() != values.size() || primaryKeyColumns.isEmpty()) {
			throw new IllegalArgumentException("Nav norādīti derīgi lauki vai primārā atslēga.");
		}
		List<String> validatedValues = InputValidator.validateValues(con, tableName, columns, values);
		List<Integer> keyIndexes = new ArrayList<>();
		List<Integer> updateIndexes = new ArrayList<>();
		for (String primaryKeyColumn : primaryKeyColumns) {
			int index = -1;
			for (int i = 0; i < columns.size(); i++) {
				if (primaryKeyColumn.equalsIgnoreCase(columns.get(i))) {
					index = i;
					break;
				}
			}
			if (index < 0) {
				throw new IllegalArgumentException("Neizdevās atrast primārās atslēgas lauku " + primaryKeyColumn + ".");
			}
			keyIndexes.add(index);
		}
		for (int i = 0; i < columns.size(); i++) {
			String columnName = columns.get(i);
			if (primaryKeyColumns.stream().noneMatch(key -> key.equalsIgnoreCase(columnName))) {
				updateIndexes.add(i);
			}
		}
		if (updateIndexes.isEmpty()) {
			throw new IllegalArgumentException("Nav lauku, kurus atjaunināt.");
		}

		StringBuilder sql = new StringBuilder("UPDATE ").append(InputValidator.quoteIdentifier(tableName)).append(" SET ");
		for (int i = 0; i < updateIndexes.size(); i++) {
			if (i > 0) {
				sql.append(", ");
			}
			sql.append(InputValidator.quoteIdentifier(columns.get(updateIndexes.get(i)))).append(" = ?");
		}
		sql.append(" WHERE ");
		for (int i = 0; i < keyIndexes.size(); i++) {
			if (i > 0) {
				sql.append(" AND ");
			}
			sql.append(InputValidator.quoteIdentifier(columns.get(keyIndexes.get(i)))).append(" = ?");
		}

		try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
			int parameter = 1;
			for (int index : updateIndexes) {
				ps.setObject(parameter++, validatedValues.get(index));
			}
			for (int index : keyIndexes) {
				ps.setObject(parameter++, validatedValues.get(index));
			}
			return ps.executeUpdate();
		}
	}
}
