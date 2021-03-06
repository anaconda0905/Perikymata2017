package es.ubu.lsi.perikymata;

/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import es.ubu.lsi.perikymata.modelo.Project;
import es.ubu.lsi.perikymata.util.StitchingTemporaryUtil;
import es.ubu.lsi.perikymata.util.SystemUtil;
import es.ubu.lsi.perikymata.util.sockets.ClientSocket;
import es.ubu.lsi.perikymata.util.sockets.Request;
import es.ubu.lsi.perikymata.vista.ImageSelectionController;
import es.ubu.lsi.perikymata.vista.PerikymataCountController;
import es.ubu.lsi.perikymata.vista.RootLayoutController;
import es.ubu.lsi.perikymata.vista.RotationCropLayoutController;
import es.ubu.lsi.perikymata.vista.TemporaryFolderSelectionController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Controller for the main application, contains the data that needs to be
 * accessed by any of the other windows and has common operations, like
 * navigation between windows or data access.
 *
 * @author Sergio Chico Carrancio
 * @author Andres Miguel Teran
 */
public class MainApp extends Application {

	/**
	 * Main container of the application's layout.
	 */
	private Stage primaryStage;
	/**
	 * Main layout of the application.
	 */
	private BorderPane rootLayout;

	/**
	 * Gets the root layout
	 *
	 * @return the rootLayout
	 */
	public BorderPane getRootLayout() {
		return rootLayout;
	}

	/**
	 * Sets the root layout.
	 *
	 * @param rootLayout
	 *            the rootLayout to set
	 */
	public void setRootLayout(BorderPane rootLayout) {
		this.rootLayout = rootLayout;
	}

	/**
	 * File logger.
	 */
	private Logger logger = Logger.getLogger(MainApp.class.getName());

	/**
	 * Full image of a tooth, used to count perikyma.
	 */
	private Image fullImage;

	/**
	 * Cropped image of a tooth with the python filter applied.
	 */
	private Image filteredImage;

	/**
	 * Cropped image of a tooth with the python filter applied and overlapped
	 * with the cropped image.
	 */
	private Image filteredOverlappedImage;

	/**
	 * Cropped image with the dental crown.
	 */
	private Image croppedImage;

	/**
	 * List of files to stitch.
	 */
	private ObservableList<String> filesList = FXCollections.observableArrayList();

	/**
	 * Data of a perikymata project.
	 */
	private Project project;

	/**
	 * Opened project Path
	 */
	private String projectPath;

	/**
	 * Util for temporary folder validation.
	 */
	private StitchingTemporaryUtil tempUtil = new StitchingTemporaryUtil();

	/**
	 * Image selection breadcrumb.
	 */
	private Label imageSelectionBreadcrumb;

	/**
	 * Perikymata count breadcrumb.
	 */
	private Label perikymataCountBreadcrumb;

	/**
	 * Rotation and image crop breadcrumb.
	 */
	private Label rotationAndCropBreadcrumb;

	/**
	 * Launches the applications, no args needed.
	 *
	 * @param args
	 *            application arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Initializes the primary stage.
	 */
	@Override
	public void start(Stage primaryStage) {
		configureLogger();
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("Perikymata - Unsaved Project");
		this.primaryStage.getIcons()
				.add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));

		// Start Python Server
		startServer();

		initRootLayout();
		// Open the project depending on the last image available
		if (filteredImage != null) {
			showPerikymataCount();
		} else if (croppedImage != null) {
			showRotationCrop();
		} else {
			showImageSelection();
		}
	}

	/**
	 * Starts the Python Server.
	 *
	 * restart true if want to restart
	 */
	public void startServer() {
		Runnable initializeServer = () -> {
			try {
				ArrayList<String> command = new ArrayList<String>();
				if (SystemUtil.isWindows()) {
					// run start server command
					command.add("cmd.exe");
					command.add("/c");
					command.add("start");
					command.add("PythonApp\\StartServerWindows.bat");
					/*
					 * During application development run server in the python
					 * IDE, not here. This is only for deployment.
					 */

				} else {
					// Run in background
					command.add("python3");
					command.add("PythonApp/src/ServerSocket.py");
				}
				ProcessBuilder process = new ProcessBuilder(command);
				process.start();
			} catch (Exception e) {
				getLogger().log(Level.SEVERE, "Exception restarting server. Can't restart because is not running.", e);
			}
		};
		// start the thread
		new Thread(initializeServer).start();
	}

	/**
	 * Configures the logger to log in a file.
	 */
	public void configureLogger() {
		try {
			getLogger().setLevel(Level.ALL);
			Date date = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			FileHandler fileHandler = new FileHandler("errorLog_" + dateFormat.format(date) + ".log");
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new SimpleFormatter());
			getLogger().addHandler(fileHandler);
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Exception creating logging file.", e);
		}
	}

	/**
	 * Loads and shows the RootLayout and tries to load the last opened file.
	 */
	public void initRootLayout() {
		try {
			// Loads the FXML view.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("vista/RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			// Puts the rootlayout(menubar) into the scene.
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);

			// Save breadcrumbs
			imageSelectionBreadcrumb = (Label) scene.lookup("#ImageSelectionBreadcrumb");
			rotationAndCropBreadcrumb = (Label) scene.lookup("#RotationAndCropBreadcrumb");
			perikymataCountBreadcrumb = (Label) scene.lookup("#PerikymataCountBreadcrumb");

			// Initialize breadcrumbs state
			imageSelectionBreadcrumb.setCursor(Cursor.HAND);
			rotationAndCropBreadcrumb.setCursor(Cursor.HAND);
			perikymataCountBreadcrumb.setCursor(Cursor.HAND);
			disableBreadcrumbs(false, true, true);

			// Gives a mainapp's reference to the controller.
			RootLayoutController controller = loader.getController();
			controller.setMainApp(this);

			primaryStage.show();

			// Set minimum width and height
			this.primaryStage.setMinHeight(600.0);
			this.primaryStage.setMinWidth(670.0);

		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Exception occur loading the root layout.", e);
		}

		// Tries to load the last opened file.
		File file = getProjectFilePathProperty();
		if (file != null) {
			// if the file exists, it is loaded. If it doesn't the reference is
			// erased.
			loadProjectFromFile(file);
		} else {
			Boolean cont = false;
			while (!cont) {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
				window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
				alert.setTitle("A project needs to be open.");
				alert.setHeaderText("You need to open or create a new project to continue.");
				alert.setContentText("Cancel will close the application.");

				ButtonType buttonTypeNew = new ButtonType("New Project");
				ButtonType buttonTypeOpen = new ButtonType("Open Project");
				ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

				alert.getButtonTypes().setAll(buttonTypeNew, buttonTypeOpen, buttonTypeCancel);

				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == buttonTypeNew) {
					cont = createProject();
				} else if (result.get() == buttonTypeOpen) {
					cont = openProject();
				} else {
					closeApplication();
				}
			}
		}

	}

	/**
	 * Disable and enable each breadcrumb of the application, also handle their
	 * font wheight.
	 *
	 * @param disableImageSelection
	 *            true/false for disable image selection breadcrumb
	 * @param disableRotationAndCropImage
	 *            true/false for disable rotation and crop image breadcrumb
	 * @param disablePerikymataCount
	 *            true/false for disable perikymata count breadcrumb
	 */
	public void disableBreadcrumbs(boolean disableImageSelection, boolean disableRotationAndCropImage,
			boolean disablePerikymataCount) {

		if (!disableImageSelection && !disableRotationAndCropImage && !disablePerikymataCount) {
			imageSelectionBreadcrumb.setFont(Font.font(null, FontWeight.NORMAL, -1));
			rotationAndCropBreadcrumb.setFont(Font.font(null, FontWeight.NORMAL, -1));
			perikymataCountBreadcrumb.setFont(Font.font(null, FontWeight.BOLD, -1));
		} else if (!disableImageSelection && !disableRotationAndCropImage && disablePerikymataCount) {
			imageSelectionBreadcrumb.setFont(Font.font(null, FontWeight.NORMAL, -1));
			rotationAndCropBreadcrumb.setFont(Font.font(null, FontWeight.BOLD, -1));
			perikymataCountBreadcrumb.setFont(Font.font(null, FontWeight.NORMAL, -1));
		} else {
			imageSelectionBreadcrumb.setFont(Font.font(null, FontWeight.BOLD, -1));
			rotationAndCropBreadcrumb.setFont(Font.font(null, FontWeight.NORMAL, -1));
			perikymataCountBreadcrumb.setFont(Font.font(null, FontWeight.NORMAL, -1));
		}

		// Disable or enable labels
		imageSelectionBreadcrumb.setDisable(disableImageSelection);
		rotationAndCropBreadcrumb.setDisable(disableRotationAndCropImage);
		perikymataCountBreadcrumb.setDisable(disablePerikymataCount);
	}

	/**
	 * Creates or updates the current project XML.
	 */
	public void makeProjectXml() {
		File parent = new File(this.getProjectPath());

		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Project.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			// Marshalling and saving XML to the file.
			File projectXMLfile = new File(parent.toString() + File.separator + parent.getName() + ".xml");
			m.marshal(getProject(), projectXMLfile);

			// Save the file path to the registry.
			setProjectFilePathProperty(projectXMLfile);
		} catch (JAXBException e) {

			this.getLogger().log(Level.SEVERE, "Exception occur creating XML.", e);
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Error saving project");
			alert.setHeaderText("Project was not saved.\n");
			alert.setContentText("Error saving project on path: " + parent.toString());
			alert.showAndWait();
		}
	}

	/**
	 * Loads a Project XML file into the application.
	 *
	 * @param file
	 *            XML project.
	 */
	public void loadProjectFromFile(File file) {
		try {
			JAXBContext context = JAXBContext.newInstance(Project.class);
			Unmarshaller um = context.createUnmarshaller();

			// reads the XML and saves its data into a Project class.
			project = (Project) um.unmarshal(file);
			this.primaryStage.setTitle("Perikymata - " + project.getProjectName());

			// Adds the Full image to the project (if exists)
			File fullImageFile = Paths.get(file.getParent(), "Full_Image", "Full_Image.png").toFile();
			if (fullImageFile.exists()) {
				try {
					BufferedImage full = ImageIO.read(fullImageFile);
					setFullImage(SwingFXUtils.toFXImage(full, null));

					// Adds the cropped image to the project (if exists)
					File croppedImageFile = Paths.get(file.getParent(), "Cropped_Image", "Cropped_Image.png").toFile();
					if (croppedImageFile.exists()) {
						BufferedImage cropped = ImageIO.read(croppedImageFile);
						setCroppedImage(SwingFXUtils.toFXImage(cropped, null));

						// Adds the filtered image to the project (if exists)
						File filteredImageFile = Paths.get(file.getParent(), "Cropped_Image", "Filtered_Image.png")
								.toFile();
						if (filteredImageFile.exists()) {

							BufferedImage filtered = ImageIO.read(filteredImageFile);
							setFilteredImage(SwingFXUtils.toFXImage(filtered, null));

						} else {
							setFilteredImage(null);
						}

						// Adds the filtered overlapped image to the project (if
						// exists)
						File filteredOverlappedImageFile = Paths
								.get(file.getParent(), "Cropped_Image", "FilteredOverlapped_Image.png").toFile();
						if (filteredOverlappedImageFile.exists()) {
							BufferedImage filteredOverlapped = ImageIO.read(filteredOverlappedImageFile);
							setFilteredOverlappedImage(SwingFXUtils.toFXImage(filteredOverlapped, null));

						} else {
							setFilteredOverlappedImage(null);
						}

					} else {
						// If doesn't exists we set it to null
						setCroppedImage(null);
						setFilteredImage(null);
						setFilteredOverlappedImage(null);
					}
				} catch (IOException e) {
					this.getLogger().log(Level.SEVERE, "Exception occur loading images.", e);
					Alert alert = new Alert(Alert.AlertType.ERROR);
					Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
					window.getIcons()
							.add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
					alert.setTitle("Internal error.");
					alert.setHeaderText("Error loading images.\n");
					alert.setContentText("This application will close now, please try again.\n");
					alert.showAndWait();
					System.exit(-1);
				}

			}

			// Adds the names of the files under the folder "fragments" to the
			// list of
			// images to stitch.
			File fragmentsFolder = Paths.get(file.getParent(), "Fragments").toFile();
			for (File fragment : fragmentsFolder.listFiles()) {
				if (fragment != null && isImageFile(fragment)) {
					getFilesList().add(fragment.getName());
				}
			}

			// Saves the path of the opened file.
			setProjectFilePathProperty(file);
			setProjectPath(file.getParent());

		} catch (JAXBException e) {

			this.getLogger().log(Level.SEVERE, "Exception occur loading project.", e);
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Error loading project");
			alert.setHeaderText("Project cannot be loaded.\n");
			alert.setContentText("Error loading project on path: " + file.toString());
			alert.showAndWait();
			this.clearData();
		}
	}

	/**
	 * Checks if the file is a valid image.
	 *
	 * @param image
	 *            image file
	 * @return true/false is the file is a valid image
	 */
	private boolean isImageFile(File image) {
		boolean valid = false;
		int last = image.getAbsolutePath().lastIndexOf('.');
		String extension = image.getAbsolutePath().substring(last + 1);
		if (extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg")
				|| extension.equalsIgnoreCase("tif") || extension.equalsIgnoreCase("tiff")
				|| extension.equalsIgnoreCase("bmp")) {
			valid = true;
		}
		return valid;
	}

	/**
	 * Shows the Image Selection Window.
	 */
	public void showImageSelection() {
		try {
			// Loads the FXML view.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("vista/ImageSelection.fxml"));
			AnchorPane imageSelection = (AnchorPane) loader.load();

			// Shows this layout in the center of the rootLayout.
			rootLayout.setCenter(imageSelection);

			// Manage breadcrumbs
			disableBreadcrumbs(false, true, true);

			// Gives a mainapp's reference to the controller of the layout.
			ImageSelectionController controller = loader.getController();
			controller.setMainApp(this);

		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Exception occur loading imageSelection Stage.", e);
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Internal error.");
			alert.setHeaderText("Error loading image selection stage.\n");
			alert.setContentText("This application will close now, please try again.\n");
			alert.showAndWait();
			System.exit(-1);
		}
	}

	/**
	 * Shows the Rotation Window.
	 */
	public void showRotationCrop() {
		try {
			// Loads the FXML view.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("vista/RotationCropLayout.fxml"));
			BorderPane window = (BorderPane) loader.load();

			// Shows this layout in the center of the rootLayout.
			rootLayout.setCenter(window);

			// Manage breadcrumbs
			disableBreadcrumbs(false, false, true);

			// Gives a mainapp's reference to the controller of the layout.
			RotationCropLayoutController controller = loader.getController();
			controller.setMainApp(this);

		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Exception occur loading rotation and crop Stage.", e);
			Alert alert = new Alert(Alert.AlertType.ERROR);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Internal error.");
			alert.setHeaderText("Error loading rotation stage.\n");
			alert.setContentText("This application will close now, please try again.\n");
			alert.showAndWait();
			System.exit(-1);
		}
	}

	/**
	 * Shows the Perikymata counting window.
	 */
	public void showPerikymataCount() {
		try {
			// Loads the FXML view.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("vista/PerikymataCount.fxml"));

			BorderPane perikymataCount = (BorderPane) loader.load();

			// Shows this layout in the center of the rootLayout.
			rootLayout.setCenter(perikymataCount);

			// Manage breadcrumbs
			disableBreadcrumbs(false, false, false);

			// Gives a mainapp's reference to the controller of the layout.
			PerikymataCountController controller = loader.getController();
			controller.setMainApp(this);

		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Exception occur loading PerikymataCount Stage.", e);
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Internal error.");
			alert.setHeaderText("Error loading perikymata counting stage.\n");
			alert.setContentText("This application will close now, please try again.\n");
			alert.showAndWait();
			System.exit(-1);
		}
	}

	/**
	 * Shows the Temporary folder selection Window.
	 *
	 * @param showCancel
	 *            true/false enable/disable cancel button
	 */
	public void showTemporaryFolderSelection(boolean showCancel) {
		try {
			// Loads the FXML view.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("vista/TemporaryFolderSelection.fxml"));
			Parent parent = (Parent) loader.load();
			Stage window = new Stage();
			window.setScene(new Scene(parent));
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			window.setTitle("Temporary Folder Selection");
			window.show();

			// Gives a mainapp's reference to the controller of the layout.
			TemporaryFolderSelectionController controller = loader.getController();
			controller.setMainApp(this);
			controller.disableCancel(showCancel);
			controller.initializeComponents();

		} catch (Exception e) {
			this.getLogger().log(Level.SEVERE, "Exception occur loading temporary folder selection window.", e);
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Internal error.");
			alert.setHeaderText("Error loading temporary folder selection window.\n");
			alert.setContentText("This application will close now, please try again.\n");
			alert.showAndWait();
			System.exit(-1);
		}
	}

	/**
	 * Returns the main stage.
	 *
	 * @return primaryStage
	 */
	public Stage getPrimaryStage() {
		return primaryStage;

	}

	/**
	 * @return the fileList
	 */
	public ObservableList<String> getFilesList() {
		return filesList;
	}

	/**
	 * Gets the full image of the tooth.
	 *
	 * @return Image of the tooth.
	 */
	public Image getFullImage() {
		return fullImage;
	}

	/**
	 * Sets the full image of the tooth.
	 *
	 * @param fullImage
	 *            Full image of the tooth.
	 */
	public void setFullImage(Image fullImage) {
		this.fullImage = fullImage;
	}

	/**
	 * Gets the filtered image of the tooth.
	 *
	 * @return Image of the tooth.
	 */
	public Image getFilteredImage() {
		return filteredImage;
	}

	/**
	 * Sets the filtered image of the tooth.
	 *
	 * @param filteredImage
	 *            Filtered image of the tooth.
	 */
	public void setFilteredImage(Image filteredImage) {
		this.filteredImage = filteredImage;
	}

	/**
	 * Gets the filtered overlapped image of the tooth.
	 *
	 * @return Filtered overlapped image of the tooth.
	 */
	public Image getFilteredOverlappedImage() {
		return filteredOverlappedImage;
	}

	/**
	 * Sets the filtered overlapped image of the tooth.
	 *
	 * @param filteredOverlappedImage
	 *            filtered overlapped image of the tooth
	 */
	public void setFilteredOverlappedImage(Image filteredOverlappedImage) {
		this.filteredOverlappedImage = filteredOverlappedImage;
	}

	/**
	 * Gets the cropped image of the tooth.
	 *
	 * @return Image of the tooth.
	 */
	public Image getCroppedImage() {
		return croppedImage;
	}

	/**
	 * Sets the cropped image of the tooth.
	 *
	 * @param croppedImage
	 *            Cropped image of the tooth.
	 */
	public void setCroppedImage(Image croppedImage) {
		this.croppedImage = croppedImage;
	}

	/**
	 * Gets the project data.
	 *
	 * @return Project with the project data.
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * Sets the project data.
	 *
	 * @param project
	 *            Project with the data of a perikymata project.
	 */
	public void setProject(Project project) {
		this.project = project;
	}

	/**
	 * Saves the property of the last opened project file.
	 *
	 * @param file
	 *            Project file to store into preferences or null to remove the
	 *            preference.
	 */
	public void setProjectFilePathProperty(File file) {
		Properties properties = new Properties();
		try {

			if (file != null) {
				properties.setProperty("filePath", file.getPath());
			}
			FileOutputStream fout = new FileOutputStream("config.properties");
			properties.store(fout, null);
		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Exception writing properties file.", e);
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Error saving properties file.");
			alert.setHeaderText("Error saving properties file.\n");
			alert.setContentText("Check that you have writing permissions on the folder\n"
					+ "that contains this application, so config.properties can be written.\n");
			alert.showAndWait();
		}
	}

	/**
	 * Loads the preference of the last opened project file.
	 *
	 * @return null if project wasn't found in the preferences, File of the last
	 *         opened project otherwise.
	 */
	public File getProjectFilePathProperty() {
		Properties properties = new Properties();
		try {
			String filePath = null;
			if (Paths.get("config.properties").toFile().exists()) {
				properties.load(new FileInputStream("config.properties"));
				filePath = properties.getProperty("filePath", null);
				if (filePath != null) {
					File f = new File(filePath);
					if (f.exists()) {
						return new File(filePath);
					}
				}
			}

		} catch (FileNotFoundException e) {
			this.getLogger().log(Level.WARNING, "Properties file doesn't exists.", e);

		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Exception occur opening properties file .", e);
			Alert alert = new Alert(Alert.AlertType.INFORMATION);
			Stage window = (Stage) alert.getDialogPane().getScene().getWindow();
			window.getIcons().add(new Image(this.getClass().getResource("/rsc/Tooth-icon.png").toExternalForm()));
			alert.setTitle("Error opening properties file.");
			alert.setHeaderText("Error loading opening properties file.\n");
			alert.setContentText(
					"If this problem persists, please delete config.properties" + " on the program folder.\n");
			alert.showAndWait();
		}
		return null;
	}

	/**
	 * Creates a new project (folder structure and project xml) by choosing a
	 * file-chooser.
	 *
	 * @return true/false
	 */
	public Boolean createProject() {
		FileChooser fileChooser = new FileChooser();

		// Adds a filter that shows all the files..
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Project Folder", "*");
		fileChooser.getExtensionFilters().add(extFilter);
		fileChooser.initialFileNameProperty().set("Project_Name");

		// Shows the save dialog.
		File file = fileChooser.showSaveDialog(getPrimaryStage());

		if (file != null) {
			// destroys old data to create new.
			clearData();
			// Saves the project name.
			setProject(new Project());
			getProject().setProjectName(file.getName());

			if (tempUtil.isTempFolderValid(System.getProperty("java.io.tmpdir"))) {
				getProject().setTemporaryFolder("DEFAULT");
			} else {
				// Ask for the temp folder is system folder is not valid
				showTemporaryFolderSelection(true);
			}

			// Makes the folder structure.
			file.mkdir();
			// AMT Changed folder structure creation 30/01/2017
			new File(file.toString() + File.separator + "Fragments").mkdir();
			new File(file.toString() + File.separator + "Full_Image").mkdir();
			new File(file.toString() + File.separator + "Perikymata_Outputs").mkdir();
			new File(file.toString() + File.separator + "Cropped_Image").mkdir();
			setProjectPath(file.getPath());
			// Creates the XML project file.

			getPrimaryStage().setTitle("Perikymata - " + file.getName());
			makeProjectXml();
			showImageSelection();
			return true;
		}
		return false;

	}

	/**
	 * Opens a FileChooser to let the user select a perikymata project file
	 * (xml) to load.
	 *
	 * @return true/false
	 */
	public Boolean openProject() {
		FileChooser fileChooser = new FileChooser();
		// adds a XML project filter.
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Perikymata XML file (*.xml)", "*.xml");
		fileChooser.getExtensionFilters().add(extFilter);

		// shows the open project dialog.
		File file = fileChooser.showOpenDialog(getPrimaryStage());

		if (file != null) {
			// destroys old data to create new.
			clearData();
			loadProjectFromFile(file);
			setProjectPath(file.getParent());
			return true;
		}
		return false;

	}

	/**
	 * @return the projectPath
	 */
	public String getProjectPath() {
		return projectPath;
	}

	/**
	 * @param projectPath
	 *            the projectPath to set
	 */
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;

	}

	/**
	 * @return the logger
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Clear the data of all the variables to a new state to open or create a
	 * project.
	 */
	public void clearData() {
		this.filesList.clear();
		this.filteredImage = null;
		this.filteredOverlappedImage = null;
		this.fullImage = null;
		this.croppedImage = null;
		this.project = null;
		this.projectPath = null;
	}

	/**
	 * Stops the application.
	 */
	@Override
	public void stop() throws Exception {
		closeApplication();
	}

	/**
	 * Stops the server.
	 *
	 * @throws ConnectException
	 *             if the connection is not possible
	 * @throws Exception
	 *             If the server has other kind of errors
	 */
	public void stopServer() throws ConnectException, Exception {
		ClientSocket client = new ClientSocket();
		// Close server request
		Request request = new Request(Request.CLOSE_SERVER, "CLOSE_SERVER");
		client.send(request);
		String response = client.receive();
		client.close();
		if (!response.equals("OK")) {
			// Stop process if we are in Linux
			ArrayList<String> command = new ArrayList<String>();
			if (!SystemUtil.isWindows()) {
				command.add("pkill");
				command.add("python3");
			}
			ProcessBuilder process = new ProcessBuilder(command);
			process.start();
			throw new Exception("Error closing server. Response not OK");
		}
	}

	/**
	 * Closes the application and the python server.
	 */
	public void closeApplication() {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					stopServer();
					System.exit(-1);
				} catch (ConnectException e) {
					Platform.runLater(() -> {
						System.exit(-1);
					});
				} catch (Exception e) {
					Platform.runLater(() -> {
						System.exit(-1);
					});
				}
			}
		});
		thread.start();
	}
}