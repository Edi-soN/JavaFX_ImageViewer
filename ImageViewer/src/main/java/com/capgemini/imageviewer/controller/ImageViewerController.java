package com.capgemini.imageviewer.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.capgemini.imageviewer.controller.WallpaperChanger.SPI;
import com.sun.jna.platform.win32.WinDef.UINT_PTR;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.DirectoryChooser;

public class ImageViewerController {

	@FXML
	private ImageView imageView;
	@FXML
	private ScrollPane scrollPane;
	@FXML
	private Button openFolderButton;
	@FXML
	private Button nextImageButton;
	@FXML
	private Button previousImageButton;
	@FXML
	private Button slideShowButton;
	@FXML
	private Label locationLabel;
	@FXML
	private ListView<String> imageList;

	private Map<String, File> images = new HashMap<String, File>();

	final DoubleProperty zoomProperty = new SimpleDoubleProperty(200);

	@FXML
	private void initialize() {

		imageList.setOnMouseClicked(new EventHandler<MouseEvent>() {

			public void handle(MouseEvent event) {
				if (!imageList.getItems().isEmpty()) {
					Image image = null;
					try {
						String nextImg = imageList.getSelectionModel().getSelectedItem();
						image = new Image(images.get(nextImg).toURI().toURL().toString(), false);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
					imageView.setFitWidth(0);
					imageView.setFitHeight(0);
					imageView.setImage(image);
					imageView.setRotate(0.0);
				}
			}
		});

		zoomProperty.addListener(new InvalidationListener() {
			public void invalidated(Observable arg0) {
				imageView.setFitWidth(zoomProperty.get() * 4);
				imageView.setFitHeight(zoomProperty.get() * 3);
			}
		});

		scrollPane.addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
			public void handle(ScrollEvent event) {
				if (event.getDeltaY() > 0) {
					zoomProperty.set(zoomProperty.get() * 1.1);
				} else if (event.getDeltaY() < 0) {
					zoomProperty.set(zoomProperty.get() / 1.1);
				}
			}
		});

		imageList.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.ENTER)) {
					if (!imageList.getItems().isEmpty()) {
						Image image = null;
						String nextImg = imageList.getSelectionModel().getSelectedItem();
						try {
							image = new Image(images.get(nextImg).toURI().toURL().toString(), false);
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						imageView.setFitWidth(0);
						imageView.setFitHeight(0);
						imageView.setImage(image);
						imageView.setRotate(0.0);
					}
				}
			}
		});

		scrollPane.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.LEFT)) {
					displayPreviousImage();
				}
				if (event.getCode().equals(KeyCode.RIGHT)) {
					displayNextImage();
				}
				if (event.getCode().equals(KeyCode.UP)) {
					rotateImageCounterClockwise();
				}
				if (event.getCode().equals(KeyCode.DOWN)) {
					rotateImageClockwise();
				}
				if (event.getCode().equals(KeyCode.SPACE)) {
					slideShowButtonAction(new ActionEvent());
				}
			}
		});

		ContextMenu contextMenu = new ContextMenu();
		MenuItem setAsBackgroundImage = new MenuItem("Set as desktop background");
		setAsBackgroundImage.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (!imageList.getItems().isEmpty()) {
					String nextImg = imageList.getSelectionModel().getSelectedItem();
					String path = images.get(nextImg).getAbsolutePath();
					SPI.INSTANCE.SystemParametersInfo(new UINT_PTR(SPI.SPI_SETDESKWALLPAPER), new UINT_PTR(0), path,
							new UINT_PTR(SPI.SPIF_UPDATEINIFILE | SPI.SPIF_SENDWININICHANGE));
				}
			}
		});
		MenuItem editImage = new MenuItem("Open in  MS Paint");
		editImage.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					if (!imageList.getItems().isEmpty()) {
						String nextImg = imageList.getSelectionModel().getSelectedItem();
						String path = images.get(nextImg).getAbsolutePath();
						Runtime.getRuntime().exec(new String[] { "C:\\WINDOWS\\system32\\mspaint.exe", path });
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		MenuItem openInLocation = new MenuItem("Open file location");
		openInLocation.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					if (!imageList.getItems().isEmpty()) {
						String nextImg = imageList.getSelectionModel().getSelectedItem();
						String path = images.get(nextImg).getParent();
						Runtime.getRuntime().exec("explorer " + path);
					}
				} catch (IllegalArgumentException | IOException iae) {
					iae.printStackTrace();
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle("Info");
					alert.setContentText("File not found");
					alert.show();
				}
			}
		});
		MenuItem rotateClockwise = new MenuItem("Rotate clockwise");
		rotateClockwise.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				rotateImageClockwise();
			}
		});
		MenuItem rotateCounterclockwise = new MenuItem("Rotate counterclockwise");
		rotateCounterclockwise.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				rotateImageCounterClockwise();
			}
		});
		contextMenu.getItems().addAll(setAsBackgroundImage, editImage, openInLocation, rotateClockwise,
				rotateCounterclockwise);
		scrollPane.setContextMenu(contextMenu);
	}

	@FXML
	private void openFolderButtonAction(ActionEvent event) {
		locateFile();
		displayFirstImage();
	}

	private void displayFirstImage() {
		if (!imageList.getItems().isEmpty()) {
			Image image = null;
			try {
				imageList.getSelectionModel().selectFirst();
				String nextImg = imageList.getSelectionModel().getSelectedItem();
				image = new Image(images.get(nextImg).toURI().toURL().toString(), false);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			imageView.setFitWidth(0);
			imageView.setFitHeight(0);
			imageView.setImage(image);
			imageView.setRotate(0.0);
		}
	}

	@FXML
	private void locateFile() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Open Folder");
		File fileDir = directoryChooser.showDialog(null);
		if (fileDir != null) {
			images.clear();
			locationLabel.setText(fileDir.getAbsolutePath());
			String[] fileExtension = new String[] { "jpg", "png", "gif", "bmp", "jpeg" };
			for (File nextFile : fileDir.listFiles()) {
				for (String extension : fileExtension) {
					if (nextFile.getName().toLowerCase().endsWith(extension)) {
						images.put(nextFile.getName(), nextFile);
					}
				}
			}
			if (images.isEmpty()) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Info");
				alert.setContentText("No images found");
				alert.show();
			}
			imageList.setItems(FXCollections.observableList(new ArrayList<String>(images.keySet())));
		}
	}

	@FXML
	private void nextImageButtonAction(ActionEvent event) {
		displayNextImage();
	}

	private void displayNextImage() {
		if (!imageList.getItems().isEmpty()) {
			Image image = null;
			try {
				if (imageList.getSelectionModel().getSelectedIndex() < imageList.getItems().size() - 1) {
					imageList.getSelectionModel().selectNext();
				} else {
					imageList.getSelectionModel().selectFirst();
				}
				String nextImg = imageList.getSelectionModel().getSelectedItem();
				image = new Image(images.get(nextImg).toURI().toURL().toString(), false);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			imageView.setFitWidth(0);
			imageView.setFitHeight(0);
			imageView.setImage(image);
			imageView.setRotate(0.0);
		}

	}

	@FXML
	private void previousImageButtonAction(ActionEvent event) {
		displayPreviousImage();
	}

	private void displayPreviousImage() {
		if (!imageList.getItems().isEmpty()) {
			Image image = null;
			try {
				if (imageList.getSelectionModel().getSelectedIndex() > 0) {
					imageList.getSelectionModel().selectPrevious();
				} else {
					imageList.getSelectionModel().selectLast();
				}
				String nextImg = imageList.getSelectionModel().getSelectedItem();
				image = new Image(images.get(nextImg).toURI().toURL().toString(), false);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			imageView.setFitWidth(0);
			imageView.setFitHeight(0);
			imageView.setImage(image);
			imageView.setRotate(0.0);
		}

	}

	private boolean isClicked = false;

	@FXML
	private void slideShowButtonAction(ActionEvent event) {
		if (!imageList.getItems().isEmpty()) {
			if (!isClicked) {
				isClicked = true;
				slideShowButton.setText("Stop SlideShow");
				slideShow();
			} else {
				isClicked = false;
				slideShowButton.setText("Start SlideShow");
				timer.cancel();
			}
		}
	}

	private Timer timer;

	private void slideShow() {
		if (!imageList.getItems().isEmpty()) {
			long delay = 3000;
			timer = new Timer();
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					displayNextImage();
				}
			}, delay, delay);
		}
	}

	private void rotateImageClockwise() {
		imageView.setRotate(imageView.getRotate() + 90.0);
	}

	private void rotateImageCounterClockwise() {
		imageView.setRotate(imageView.getRotate() - 90.0);
	}

}
