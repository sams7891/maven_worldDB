package dataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataBaseConnection {
	private static final String URL = "jdbc:mysql://localhost:3307/world"
			+ "?useSSL=false"
			+ "&allowPublicKeyRetrieval=true"
			+ "&serverTimezone=Europe/Riga"
			+ "&characterEncoding=utf8"
			+ "&useUnicode=true";
	
	private static final String USER = "root";
	private static final String PASSWORD = "Parole1";
	
	public static Connection getConnection() throws SQLException{
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}
}
