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

	Boolean isUser;

	@Override
	public void start(Stage primaryStage) {
		isUser = true;

		try {
			if (!isUser) {
				// load the FXML resource
				FXMLLoader loader = new FXMLLoader(getClass().getResource("CameraWindow.fxml"));
				// store the root element so that the controllers can use it
				BorderPane rootElement = (BorderPane) loader.load();
				// create and style a scene
				Scene scene = new Scene(rootElement);
				scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
				// create the stage with the given title and the previously created
				// scene
				primaryStage.setTitle("Camera Window");
				primaryStage.setScene(scene);
				// show the GUI
				primaryStage.show();

				// set the proper behavior on closing the application
				CameraController controller = loader.getController();
				primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
					public void handle(WindowEvent we) {
						controller.setClosed();
					}
				}));
			} else {
				// load the FXML resource
				FXMLLoader loader = new FXMLLoader(getClass().getResource("UserWindow.fxml"));
				// store the root element so that the controllers can use it
				BorderPane rootElement = (BorderPane) loader.load();
				// create and style a scene
				Scene scene = new Scene(rootElement);
				scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
				// create the stage with the given title and the previously created
				// scene
				primaryStage.setTitle("User Window");
				primaryStage.setScene(scene);
				// show the GUI
				primaryStage.show();

				// set the proper behavior on closing the application
				UserController controller = loader.getController();
				primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
					public void handle(WindowEvent we) {
						controller.setClosed();
					}
				}));
			}

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