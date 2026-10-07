package main;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class WorldDbApp extends Application{
	private Connection con;
	private TableView<ObservableList<String>> table = new TableView<>();
	private ComboBox<String> tableSelector = new ComboBox<>();
	private List<String> currentColumns = new ArrayList<>();
	private List<String> dataBaseView = new ArrayList<>();
	
	public static void main(String[] args) {
		launch(args);
		
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
