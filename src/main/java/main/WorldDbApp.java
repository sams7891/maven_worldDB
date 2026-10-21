package main;

import java.sql.Connection;
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
import operations.InsertOperation;
import operations.SelectOperation;
import operations.UpdateOperation;
import operations.ViewManager;
import utility.AlertManager;

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
		tableSelector.getItems().addAll("city");
		
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
//		btnDelete.setOnAction(e -> performDelete());
		
		tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isSeperator = (newVal == null || newVal.startsWith("---"));
			btnSelect.setDisable(isSeperator);
			btnInsert.setDisable(isSeperator);
			btnUpdate.setDisable(isSeperator);
			btnDelete.setDisable(isSeperator);

		});
		
		tableSelector.getSelectionModel().select(1);
		HBox topBox = new HBox(10, new Label("Tabula/Skats:"), tableSelector, btnSelect);
		
		topBox.setPadding(new Insets(10));
		
		HBox bottomBox = new HBox(10, btnInsert, btnUpdate, btnDelete);
		
		VBox root = new VBox(10, topBox, table, bottomBox);
		stage.setScene(new Scene(root, 900, 600));
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
		stage.show();
		loadData();
		
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
		
		List<String> activeColums = new ArrayList<>();
		List<TextField> textFields = new ArrayList<>();
		
		int gridRow = 0;
		
		for(int i = 0; i < currentColumns.size(); i++) {
			String colName = currentColumns.get(i);
			
			if(!isUpdate && colName.equalsIgnoreCase("id")) {
				continue;
			}
			
			activeColums.add(colName);
			grid.add(new Label(colName + ": "), 0, gridRow);
			
			TextField tf = new TextField();
			
			if(isUpdate && i == 0) {
				tf.setPromptText("Ievadi esošu ID/ valsts kodu");
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
					String colName = activeColums.get(i);
					String rawValue = textFields.get(i).getText();
					values.add(rawValue);
				}
				
				if(isUpdate) {
					int rowAffected = UpdateOperation.updateRecord(con, tableName, currentColumns, values);
					
					if(rowAffected > 0) {
						AlertManager.showAlert("Veiksmīgi", "Ieraksts tika atjaunināts");
						loadData();
					}else {
						String pkColumns = currentColumns.get(0);
						String pkValue = values.get(0);
						
						AlertManager.showAlert("Informācija", "Ieraksts ar " + pkColumns + " = " + pkValue + " netika atrasts!");
						event.consume();
					}
				}else {
					InsertOperation.insertRecord(con, tableName, activeColums, values);
					AlertManager.showAlert("Veiksmīgi", "Jauns ieraksts ievietots!");
					loadData();
				}
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
