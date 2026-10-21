package operations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class SelectOperation {

	public static List<String> populateTable(Connection con, String tableName, TableView<ObservableList<String>> table) throws SQLException{
		List<String> columnNames = new ArrayList<>();
		table.getColumns().clear();
		table.getItems();
		
		ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
		
		String sql = "SELECT * FROM " + tableName + " ORDER BY 1 ASC";
		
		try(Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(sql)){
					ResultSetMetaData meta = rs.getMetaData();
					int colCount = meta.getColumnCount();
					
					for (int i = 1; i <= colCount; i++) {
					    final int j = i - 1;

					    String colName = meta.getColumnName(i);
					    columnNames.add(colName);

					    TableColumn<ObservableList<String>, String> col =
					            new TableColumn<>(colName);

					    col.setCellValueFactory(param ->
					            new SimpleStringProperty(param.getValue().get(j))
					    );

					    table.getColumns().add(col);
					}
					
						while(rs.next()) {
							ObservableList<String> row = FXCollections.observableArrayList();
							
							for(int i = 1; i <= colCount; i++) {
								row.add(rs.getString(i) == null ? "" : rs.getString(i));	
							}
							
							data.add(row);
						}
					table.setItems(data);
				}
		
		return columnNames;
	}	

}
