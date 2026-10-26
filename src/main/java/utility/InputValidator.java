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

	public static List<String> getDatabaseTables(Connection con) throws SQLException {
		List<String> tables = new ArrayList<>();
		try (ResultSet rs = con.getMetaData().getTables(con.getCatalog(), null, "%", new String[] { "TABLE" })) {
			while (rs.next()) {
				tables.add(rs.getString("TABLE_NAME"));
			}
		}
		Collections.sort(tables, String.CASE_INSENSITIVE_ORDER);
		return tables;
	}

	public static List<String> getAutoIncrementColumns(Connection con, String tableName) throws SQLException {
		List<String> columns = new ArrayList<>();
		try (ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), null, tableName, "%")) {
			while (rs.next()) {
				if ("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"))) {
					columns.add(rs.getString("COLUMN_NAME"));
				}
			}
		}
		return columns;
	}

	private static ColumnInfo getColumnInfo(Connection con, String tableName, String columnName) throws SQLException {
		DatabaseMetaData meta = con.getMetaData();
		try (ResultSet rs = meta.getColumns(con.getCatalog(), null, tableName, columnName)) {
			if (!rs.next()) {
				throw new IllegalArgumentException("Lauks " + columnName + " tabulā " + tableName + " nav atrasts.");
			}
			String columnType = null;
			String sql = "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? "
					+ "AND TABLE_NAME = ? AND COLUMN_NAME = ?";
			try (PreparedStatement statement = con.prepareStatement(sql)) {
				statement.setString(1, con.getCatalog());
				statement.setString(2, tableName);
				statement.setString(3, columnName);
				try (ResultSet typeResult = statement.executeQuery()) {
					if (typeResult.next()) {
						columnType = typeResult.getString("COLUMN_TYPE");
					}
				}
			}
			return new ColumnInfo(rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME"), rs.getInt("COLUMN_SIZE"),
					rs.getInt("DECIMAL_DIGITS"), rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
					columnType);
		}
	}

	private static void validateType(ColumnInfo column, String value) {
		try {
			switch (column.dataType) {
			case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> validateInteger(column, value);
			case Types.NUMERIC, Types.DECIMAL -> validateDecimal(column, value);
			case Types.FLOAT, Types.REAL, Types.DOUBLE -> {
				double number = Double.parseDouble(value);
				if (!Double.isFinite(number)) {
					throw new NumberFormatException();
				}
			}
			case Types.BIT, Types.BOOLEAN -> {
				if (!value.equals("0") && !value.equals("1") && !value.equalsIgnoreCase("true")
						&& !value.equalsIgnoreCase("false")) {
						throw new IllegalArgumentException("jāievada 0, 1, true vai false");
				}
			}
			case Types.DATE -> java.time.LocalDate.parse(value);
			case Types.TIME -> java.time.LocalTime.parse(value);
			case Types.TIMESTAMP -> java.time.LocalDateTime.parse(value.replace(' ', 'T'));
			default -> {
			}
			}
		} catch (NumberFormatException | java.time.DateTimeException e) {
			throw new IllegalArgumentException("Laukā jāievada derīga " + typeLabel(column.dataType) + " vērtība.");
		}
	}

	private static void validateInteger(ColumnInfo column, String value) {
		BigInteger number = new BigInteger(value);
		String type = column.typeName.toLowerCase(Locale.ROOT);
		int bits = switch (type) {
		case "tinyint" -> 8;
		case "smallint" -> 16;
		case "mediumint" -> 24;
		case "int", "integer" -> 32;
		case "bigint" -> 64;
		default -> 64;
		};
		boolean unsigned = column.columnType != null && column.columnType.toLowerCase(Locale.ROOT).contains("unsigned");
		BigInteger min = unsigned ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(bits - 1).negate();
		BigInteger max = unsigned ? BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE)
				: BigInteger.ONE.shiftLeft(bits - 1).subtract(BigInteger.ONE);
		if (number.compareTo(min) < 0 || number.compareTo(max) > 0) {
			throw new IllegalArgumentException("Vērtība laukā pārsniedz " + column.typeName + " pieļaujamo diapazonu.");
		}
	}

	private static void validateDecimal(ColumnInfo column, String value) {
		BigDecimal number = new BigDecimal(value);
		BigDecimal normalized = number.stripTrailingZeros();
		int precision = Math.max(1, normalized.precision());
		int scale = Math.max(0, normalized.scale());
		int integerDigits = Math.max(0, precision - scale);
		if (integerDigits > column.size - column.decimalDigits || scale > column.decimalDigits) {
			throw new IllegalArgumentException("Laukā atļauts ievadīt ne vairāk kā " + column.size
					+ " ciparus un " + column.decimalDigits + " zīmes aiz komata.");
		}
	}

	private static void validateEnum(ColumnInfo column, String columnName, String value) {
		if (!"enum".equalsIgnoreCase(column.typeName)) {
			return;
		}
		Matcher matcher = ENUM_VALUE.matcher(column.columnType == null ? "" : column.columnType);
		List<String> allowedValues = new ArrayList<>();
		while (matcher.find()) {
			allowedValues.add(matcher.group(1).replace("''", "'"));
		}
		if (!allowedValues.contains(value)) {
			throw new IllegalArgumentException("Laukā " + columnName + " jāievada viena no vērtībām: "
					+ String.join(", ", allowedValues) + ".");
		}
	}

	private static void validateForeignKey(Connection con, String tableName, String columnName, String value)
			throws SQLException {
		DatabaseMetaData meta = con.getMetaData();
		try (ResultSet rs = meta.getImportedKeys(con.getCatalog(), null, tableName)) {
			while (rs.next()) {
				if (!columnName.equalsIgnoreCase(rs.getString("FKCOLUMN_NAME"))) {
					continue;
				}
				String referencedTable = rs.getString("PKTABLE_NAME");
				String referencedColumn = rs.getString("PKCOLUMN_NAME");
				String sql = "SELECT 1 FROM " + quoteIdentifier(referencedTable) + " WHERE "
						+ quoteIdentifier(referencedColumn) + " = ? LIMIT 1";
				try (PreparedStatement statement = con.prepareStatement(sql)) {
					statement.setString(1, value);
					try (ResultSet match = statement.executeQuery()) {
						if (!match.next()) {
							throw new IllegalArgumentException("Vērtība laukā " + columnName + " nav atrodama tabulā "
									+ referencedTable + ".");
						}
					}
				}
			}
		}
	}

	public static String quoteIdentifier(String identifier) {
		return "`" + identifier.replace("`", "``") + "`";
	}

	private static boolean isTextType(int type) {
		return type == Types.CHAR || type == Types.VARCHAR || type == Types.LONGVARCHAR
				|| type == Types.NCHAR || type == Types.NVARCHAR || type == Types.LONGNVARCHAR;
	}

	private static String typeLabel(int type) {
		return switch (type) {
		case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> "vesela skaitļa";
		case Types.NUMERIC, Types.DECIMAL, Types.FLOAT, Types.REAL, Types.DOUBLE -> "skaitļa";
		case Types.DATE -> "datuma";
		case Types.TIME -> "laika";
		case Types.TIMESTAMP -> "datuma un laika";
		default -> "norādītā tipa";
		};
	}

	private record ColumnInfo(int dataType, String typeName, int size, int decimalDigits, boolean nullable,
			String columnType) {
	}
}
