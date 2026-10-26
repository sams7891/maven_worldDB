package main;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import atlantafx.base.theme.PrimerDark;
import dataBase.DataBaseConnection;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import operations.DeleteOperation;
import operations.InsertOperation;
import operations.SelectOperation;
import operations.UpdateOperation;
import operations.ViewManager;
import utility.AlertManager;
import utility.InputValidator;

public class WorldDbApp extends Application{
	private Connection con;
	
	private TableView<ObservableList<String>> table = new TableView<>();
	
	private ComboBox<String> tableSelector = new ComboBox<>();
	
	private List<String> currentColumns = new ArrayList<>();
	private List<String> dataBaseView = new ArrayList<>();
	
	private Button btnSelect = new Button("Atlasīt (SELECT)");
	private Button btnInsert = new Button("Pievienot (INSERT)");
	private Button btnUpdate = new Button("Atjaunināt (UPDATE)");
	private Button btnDelete = new Button("Dzēst (DELETE)");
	
	public static void main(String[] args) {
		launch(args);
		
	}

	@Override
	public void start(Stage stage) {
		Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
		
		try{
			con = DataBaseConnection.getConnection();
			dataBaseView = ViewManager.getDatabaseViews(con);
		}catch(Exception e) {
			AlertManager.showAlert("Savienojuma kļūda: ", e.getMessage());
			return;
		}
		
		tableSelector.getItems().add("--- Tabulas ---");
		try {
			tableSelector.getItems().addAll(InputValidator.getDatabaseTables(con));
		} catch (Exception e) {
			AlertManager.showAlert("Kļūda", e.getMessage());
			return;
		}
		
		if(dataBaseView.isEmpty()) {
			tableSelector.getItems().add("--- NAV ATRASTS NEVIENS SKATS ---");
			AlertManager.showAlert("Informācija", "Datu bāzē pagaidām nav neviens skats");
		}else{
			tableSelector.getItems().add("--- SKATI ---");
			tableSelector.getItems().addAll(dataBaseView);
		};
		
		btnSelect.setOnAction(e -> loadData());
		btnInsert.setOnAction(e -> showInsertUpdateDialog(false));
		btnUpdate.setOnAction(e -> showInsertUpdateDialog(true));
		btnDelete.setOnAction(e -> performDelete());
		
		tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isSeperator = (newVal == null || newVal.startsWith("---"));
			btnSelect.setDisable(isSeperator);
			btnInsert.setDisable(isSeperator);
			btnUpdate.setDisable(isSeperator);
			btnDelete.setDisable(isSeperator);

		});
		
		if (tableSelector.getItems().size() > 1) {
			tableSelector.getSelectionModel().select(1);
		}
		HBox topBox = new HBox(10, new Label("Tabula/Skats:"), tableSelector, btnSelect);
		
		topBox.setPadding(new Insets(10));
		
		HBox bottomBox = new HBox(10, btnInsert, btnUpdate, btnDelete);
		
		VBox root = new VBox(10, topBox, table, bottomBox);
		stage.setScene(new Scene(root, 900, 600));
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
		stage.show();
		loadData();
		
	}
	
	private void performDelete() {
		String tableName = tableSelector.getValue();
		
		if(tableName == null || tableName.startsWith("---"))
			return;
			
		if(dataBaseView.contains(tableName)) {
			AlertManager.showAlert("Uzmanību", "No skata dzēst nedrīkst");
			return;
		}
		
		loadData();
		Alert dialog = new Alert(Alert.AlertType.ERROR);
		dialog.setTitle("DELETE FROM " + tableName);
		
		dialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20));
		List<String> pkColumns;
		List<TextField> textFields = new ArrayList<>();
		try {
			pkColumns = InputValidator.getPrimaryKeyColumns(con, tableName);
		} catch (Exception e) {
			AlertManager.showAlert("Kļūda", e.getMessage());
			return;
		}
		dialog.setHeaderText("Ievadi primārās atslēgas laukus, lai dzēstu:");
		
		for(int i = 0; i < pkColumns.size(); i++) {
			String colName = pkColumns.get(i);
			grid.add(new Label(colName + ": "), 0, i);
			TextField tf = new TextField();
			textFields.add(tf);
			grid.add(tf, 1, i);
		}
		
		dialog.getDialogPane().setContent(grid);
		Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
		
		okButton.addEventFilter(ActionEvent.ACTION, event -> {
			try {
				List<String> validPkValues = new ArrayList<>();
				for(int i = 0; i < pkColumns.size(); i++) {
					String rawValue = textFields.get(i).getText();
					validPkValues.add(rawValue);
				}
				
				int rowsAffected = DeleteOperation.deleteRecord(con, tableName, pkColumns, validPkValues);
				
				if(rowsAffected > 0) {
					AlertManager.showAlert("Veiksmīgi dzēsts", "Ieraksti dzēsti " + rowsAffected);
					loadData();
				}else {
					AlertManager.showAlert("Dzēšana nenotika", "Norādītais ieraksts netika atrasts");
					event.consume();
				}
				
			}catch(IllegalArgumentException e) {
				AlertManager.showAlert("Ievades datu kļūda", e.getMessage());
				event.consume();
			}catch(SQLException e) {
				AlertManager.showAlert("SQL kļūda", InputValidator.formatSqlError(con, tableName, pkColumns, e));
				event.consume();
			}catch(Exception e) {
				AlertManager.showAlert("SQL kļūda", e.getMessage());
				event.consume();
			}
		});
		dialog.showAndWait();
		
	}

	private void showInsertUpdateDialog(boolean isUpdate) {
		String tableName = tableSelector.getValue();
		
		if(tableName == null || tableName.startsWith("---"))
			return;
		
		if(dataBaseView.contains(tableName)) {
			AlertManager.showAlert("Uzmanību", "Izvēlētais objekts ir skats!"
					+ "Datus tajā mainīt un ievietot nevar!");
			return;
		}
		
		loadData();
		Alert dialog = new Alert(Alert.AlertType.INFORMATION);
		
		dialog.setTitle(isUpdate ? "UPDATE " + tableName : "INSERT " + tableName);
		
		dialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
		
		if(isUpdate) {
			dialog.setHeaderText("Aizpildi laukus, lai atjauninātu:");
		}else {
			dialog.setHeaderText("Aizpildi laukus, lai pievienotu");
		}
		
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20));
		
		List<String> activeColumns = new ArrayList<>();
		List<TextField> textFields = new ArrayList<>();
		List<String> autoIncrementColumns = new ArrayList<>();
		List<String> primaryKeyColumns;
		try {
			autoIncrementColumns = InputValidator.getAutoIncrementColumns(con, tableName);
			primaryKeyColumns = InputValidator.getPrimaryKeyColumns(con, tableName);
		} catch (Exception e) {
			AlertManager.showAlert("Kļūda", e.getMessage());
			return;
		}
		
		int gridRow = 0;
		
		for(int i = 0; i < currentColumns.size(); i++) {
			String colName = currentColumns.get(i);
			
			if(!isUpdate && autoIncrementColumns.stream().anyMatch(column -> column.equalsIgnoreCase(colName))) {
				continue;
			}
			
			activeColumns.add(colName);
			grid.add(new Label(colName + ": "), 0, gridRow);
			
			TextField tf = new TextField();
			
			if(isUpdate && primaryKeyColumns.stream().anyMatch(column -> column.equalsIgnoreCase(colName))) {
				tf.setPromptText("Ievadi esošo primārās atslēgas vērtību");
			}
			textFields.add(tf);
			grid.add(tf, 1, gridRow);
			gridRow++;
			
		}
		
		dialog.getDialogPane().setContent(grid);
		
		Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
		
		okButton.addEventFilter(ActionEvent.ACTION, event -> {
			try {
				List<String> values = new ArrayList<>();
				
				for(int i = 0; i < textFields.size(); i ++) {
					values.add(textFields.get(i).getText());
				}
				
				if(isUpdate) {
					int rowAffected = UpdateOperation.updateRecord(con, tableName, currentColumns, values,
							primaryKeyColumns);
					
					if(rowAffected > 0) {
						AlertManager.showAlert("Veiksmīgi", "Ieraksts tika atjaunināts");
						loadData();
					}else {
						String pkColumns = String.join(", ", primaryKeyColumns);
						String pkValue = primaryKeyColumns.stream()
								.map(column -> values.get(currentColumns.indexOf(column))).reduce((a, b) -> a + ", " + b).orElse("");
						
						AlertManager.showAlert("Informācija", "Ieraksts ar " + pkColumns + " = " + pkValue + " netika atrasts!");
						event.consume();
					}
				}else {
					InsertOperation.insertRecord(con, tableName, activeColumns, values);
					AlertManager.showAlert("Veiksmīgi", "Jauns ieraksts ievietots!");
					loadData();
				}
			}catch(IllegalArgumentException e) {
				AlertManager.showAlert("Ievades datu kļūda", e.getMessage());
				event.consume();
			}catch(SQLException e) {
				List<String> candidateColumns = isUpdate ? currentColumns : activeColumns;
				AlertManager.showAlert("SQL kļūda", InputValidator.formatSqlError(con, tableName, candidateColumns, e));
				event.consume();
			}catch(Exception e) {
				AlertManager.showAlert("SQL kļūda", e.getMessage());
				event.consume();
			}
		});
		dialog.showAndWait();
	}
	
	private void loadData() {
		String selectedTable = tableSelector.getValue();
		if(selectedTable == null || selectedTable.startsWith("---"))
			return;
		
		try {
			currentColumns = SelectOperation.populateTable(con, selectedTable, table);
		}catch(Exception e){
			AlertManager.showAlert("Kļūda atlasot datus", e.getMessage());
		}
	}

}
