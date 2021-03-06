package es.ubu.lsi.perikymata.vista;

import es.ubu.lsi.perikymata.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Controller for the rootLayout. The BorderLayout contains a common menu bar
 * for the entire application in the top border and a free space at the center
 * to put in other layouts. This controller grants access to the FXML components
 * and has the handlers for this window actions.
 *
 * @author Sergio Chico Carrancio
 * @author Andres Miguel Teran
 */
public class RootLayoutController {

	/**
	 * Reference to the main application
	 */
	private MainApp mainApp;

	/**
	 * Method called by the main application to set a reference to itself. This
	 * is done to be able to call mainapp's methods.
	 *
	 * @param mainApp
	 *            Reference to the main controller.
	 */
	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;
	}

	/**
	 * Handler that calls the main controller and changes the content of the
	 * graphic interface to the Image Selection stage.
	 */
	@FXML
	private void windowImageSelection() {
		mainApp.showImageSelection();
	}

	/**
	 * Handler that calls the main controller and changes the content of the
	 * graphic interface to the Image filters application stage.
	 */
	@FXML
	private void windowRotationCrop() {
		mainApp.showRotationCrop();
	}

	/**
	 * Handler that calls the main controller and changes the content of the
	 * graphic interface to the perikymata counting stage.
	 */
	@FXML
	private void windowPerikymataCount() {
		mainApp.showPerikymataCount();
	}

	/**
	 * Handler that creates a new project (folder structure and project xml).
	 */
	@FXML
	private void handleNew() {
		mainApp.createProject();
	}

	/**
	 * Opens a FileChooser to let the user select a perikymata project file
	 * (xml) to load.
	 */
	@FXML
	private void handleOpen() {
		mainApp.openProject();
	}

	/**
	 * Forces a save to the XML, it is not used because XML is created on every
	 * modification of the data, but it's here just in case.
	 */
	@FXML
	private void handleSave() {
		mainApp.makeProjectXml();
	}

	/**
	 * Opens an about dialog.
	 */
	@FXML
	private void handleAbout() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
		window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
		alert.setTitle("Paleontological analysis of dental images - Perikymata");
		alert.setHeaderText("About");
		alert.setContentText("v2.0\n" + "Author: Andres Miguel Teran\n" + "Tutor: Dr. Jose Francisco Diez Pastor\n"
				+ "Tutor: Dr Raul Marticorena Sanchez  \n" + "Universidad de Burgos, July 2017\n"
				+ "Perikymata v2.0 (Analisis Paleontologico de piezas dentales 2.0)\n"
				+ "Perikymata v2.0 comes with ABSOLUTELY NO WARRANTY;\n"
				+ "for details view the file PERIKYMATA_LICENSE.txt\n"
				+ "This is free software, and you are welcome to redistribute it under "
				+ "the conditions found in the license." + "\n\n" + "v1.0\n" + "Author: Sergio Chico Carrancio\n"
				+ "Tutor: Dr. Jose Francisco Diez Pastor\n" + "Tutor: Dr Raul Marticorena Sanchez  \n"
				+ "Universidad de Burgos, July 2016\n"
				+ "Perikymata v1.0 (Analisis Paleontologico de piezas dentales)\n"
				+ "Copyright (C) 2016 Sergio Chico Carrancio.\n"
				+ "Perikymata v1.0 comes with ABSOLUTELY NO WARRANTY;\n"
				+ "for details view the file PERIKYMATA_LICENSE.txt\n"
				+ "This is free software, and you are welcome to redistribute it under "
				+ "the conditions found in the license.");
		alert.showAndWait();
	}

	/**
	 * Opens a window with the temporary folder selection layout.
	 */
	@FXML
	private void handleTempFolder() {
		mainApp.showTemporaryFolderSelection(false);
	}

	/**
	 * Set full screen view.
	 */
	@FXML
	private void handleFullScreen() {
		if (mainApp.getPrimaryStage().isMaximized()) {
			mainApp.getPrimaryStage().setMaximized(false);
		} else {
			mainApp.getPrimaryStage().setMaximized(true);
		}
	}

	/**
	 * Closes the application and the python server.
	 */
	@FXML
	private void handleExit() {
		mainApp.closeApplication();
	}

}