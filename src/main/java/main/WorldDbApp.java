package main;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import atlantafx.base.theme.PrimerDark;
import dataBase.DataBaseConnection;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import operations.SelectOperation;
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
//			dataBaseView = ViewManager.getDatabaseViews(con);
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
		
		tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isSeperator = (newVal == null || newVal.startsWith("---"));
			btnSelect.setDisable(isSeperator);
			//velak parejas podzinas
		});
		
		tableSelector.getSelectionModel().select(1);
		HBox topBox = new HBox(10, new Label("Tabula/Skats:"), tableSelector, btnSelect);
		
		topBox.setPadding(new Insets(10));
		
		//pēctam ir jauztaisa bottombox priekš pārēja
		
		VBox root = new VBox(10, topBox, table);
		stage.setScene(new Scene(root, 900, 600));
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
		stage.show();
		loadData();
		
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
