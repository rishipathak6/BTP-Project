package application;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) {

		Stage ClientStage = new Stage();

		try {
			// load the FXML resource
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Sample.fxml"));
			// store the root element so that the controllers can use it
			BorderPane rootElement = (BorderPane) loader.load();
			// create and style a scene
			Scene scene = new Scene(rootElement);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			primaryStage.setTitle("IP based Camera Video feed encryption");
			primaryStage.setScene(scene);
			// show the GUI
			primaryStage.show();

			// set the proper behavior on closing the application
			SampleController controller = loader.getController();
			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we) {
					controller.setClosed();
				}
			}));

			// load the FXML resource
			FXMLLoader loader2 = new FXMLLoader(getClass().getResource("ClientWindow.fxml"));
			// store the root element so that the controllers can use it
			BorderPane rootElement2 = (BorderPane) loader2.load();
			// create and style a scene
			Scene scene2 = new Scene(rootElement2);
			scene2.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			ClientStage.setTitle("User Window");
			ClientStage.setScene(scene2);
			// show the GUI
			ClientStage.show();

			// set the proper behavior on closing the application
			SampleController controller2 = loader2.getController();
			ClientStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we) {
					controller2.setClosed();
				}
			}));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * For launching the application...
	 * 
	 * @param args optional params
	 */
	public static void main(String[] args) {
		// load the native OpenCV library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		launch(args);
	}
}