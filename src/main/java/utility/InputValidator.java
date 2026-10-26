package utility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputValidator {
	private static final Pattern ENUM_VALUE = Pattern.compile("'((?:''|[^'])*)'");

	public static List<String> validateValues(Connection con, String tableName, List<String> columns,
			List<String> values) throws SQLException {
		if (columns.size() != values.size()) {
			throw new IllegalArgumentException("Ievadīto vērtību skaits neatbilst lauku skaitam.");
		}

		List<String> validatedValues = new ArrayList<>();
		for (int i = 0; i < columns.size(); i++) {
			validatedValues.add(validateValue(con, tableName, columns.get(i), values.get(i)));
		} 
		return validatedValues;
	}

	public static String validateValue(Connection con, String tableName, String columnName, String rawValue)
			throws SQLException {
		ColumnInfo column = getColumnInfo(con, tableName, columnName);
		String value = rawValue == null ? "" : rawValue.trim();

		if (value.isEmpty()) {
			if (column.nullable) {
				return null;
			}
			throw new IllegalArgumentException("Lauks " + columnName + " ir obligāts.");
		}

		if (isTextType(column.dataType) && value.length() > column.size) {
			throw new IllegalArgumentException("Laukā " + columnName + " drīkst ievadīt ne vairāk kā "
					+ column.size + " rakstzīmes.");
		}

		try {
			validateType(column, value);
			validateEnum(column, columnName, value);
			validateForeignKey(con, tableName, columnName, value);
		} catch (IllegalArgumentException e) {
			String message = e.getMessage() == null ? "Ievadītā vērtība nav derīga." : e.getMessage();
			if (message.toLowerCase(Locale.ROOT).contains(columnName.toLowerCase(Locale.ROOT))) {
				throw e;
			}
			throw new IllegalArgumentException("Lauks " + columnName + ": " + message, e);
		}
		return value;
	}

	public static List<String> getPrimaryKeyColumns(Connection con, String tableName) throws SQLException {
		DatabaseMetaData meta = con.getMetaData();
		List<String> columns = new ArrayList<>();
		try (ResultSet rs = meta.getPrimaryKeys(con.getCatalog(), null, tableName)) {
			while (rs.next()) {
				int position = rs.getInt("KEY_SEQ") - 1;
				while (columns.size() <= position) {
					columns.add(null);
				}
				columns.set(position, rs.getString("COLUMN_NAME"));
			}
		}
		columns.removeIf(value -> value == null);
		if (columns.isEmpty()) {
			throw new IllegalArgumentException("Tabulai " + tableName + " nav atrasta primārā atslēga.");
		}
		return columns;
	}

	
}
