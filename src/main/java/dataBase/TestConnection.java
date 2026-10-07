package dataBase;

import java.sql.Connection;

public class TestConnection {
	public static void main(String[] args) {
		try(Connection con = DataBaseConnection.getConnection()){
			System.out.println("Savienojums ir veiksmīgi izveidots:"
					+ (con != null & !con.isClosed()));
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
