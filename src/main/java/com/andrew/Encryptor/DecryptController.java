/*
 * Copyright (c) Andrew Pegg 2022.
 * All rights reversed
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.andrew.Encryptor;

import com.andrew.Encryptor.EncryptorService.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller of the Decrypt.fxml file
 * @author andrew pegg
 * @version 1.00 07/12/2022
 */
public class DecryptController implements Initializable {

    private enum DecryptionMethod {Password, Key}

    /**
     * The model of the class, holds data relevant to underlying file like path, encryption type, etc. It is also used a bridge between the backend service and front end
     */
    private EncryptedFile encryptedFile;
    /**
     * Used to verify that view has not been rendered
     */
    private boolean preDrawl = true;
    /**
     * Button to be used to select the file the user wants to encrypt
     */
    @FXML
    private Button chooseFileButton;
    /**
     * Back navigation button used to go back and forth in multistep menu
     */
    @FXML
    private Button backButton;
    /**
     * Next navigation button used to go forward in multistep menu
     */
    @FXML
    private Button nextButton;
    /**
     * Pane used for housing navigation and function buttons onto the screen
     */
    @FXML
    private Pane pane;
    /**
     * Label to give user feedback on what file they have selected
     */
    @FXML
    private Label fileLabel;
    /**
     * Used to show encryption/decryption progress
     */
    private ProgressIndicator indicator;
    /**
     * Used so the user can select what type of encryption/decryption method they would like to use. Password or Key based
     */
    private ComboBox<DecryptionMethod> decryptionMethodComboBox;
    /**
     * An arraylist used to help aid in changing scenes
     * @see DrawScene
     */
    private ArrayList<DrawScene> sceneCollection;
    //needed in order to set next button in case of cancel "anto-arrow-right"
    private FontIcon icon;
    /**
     * Used to indicate what scene the menu is currently on, 0 is used to render home page
     */
    private int SceneId = 1;

    private final DrawScene scene0 = ()-> {
        try{
            AnchorPane anchorPane = FXMLLoader.load(DecryptController.class.getResource("Main.fxml"));
            pane.getScene().setRoot(anchorPane);
        }catch (Exception e){
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Failed to create Scene");
            alert.setHeaderText("Failed to create Main Menu Scene");
            MainController.createErrorDialog(e,alert);
        }
    };
    private final DrawScene scene1 = () -> {
        nextButton.setText(null);
        nextButton.setGraphic(icon);
        nextButton.setOnAction(e-> changeScene(++SceneId));
        pane.getChildren().setAll(backButton,nextButton,chooseFileButton,fileLabel);
    };
    private final DrawScene scene2 = () -> {

        nextButton.setTextAlignment(TextAlignment.CENTER);
        nextButton.setTextFill(Paint.valueOf("#1CAEEE"));
        nextButton.setText("Decrypt");
        nextButton.setGraphic(null);
        nextButton.setOnAction(e -> handleEncryptionMethod());
        pane.getChildren().setAll(backButton,nextButton,decryptionMethodComboBox);

    };
    /**
     * Shows progress indicator in the center of the pane
     */
    private final DrawScene scene3 = () -> pane.getChildren().setAll(indicator);

    /**
     * Default constructor used to initialize controller
     */
    public DecryptController(){
    }

    /**
     * Inits components for feature use in view as well as constructs general styling, logic, and size of components
     * @param location
     * The location used to resolve relative paths for the root object, or
     * {@code null} if the location is not known.
     *
     * @param resources
     * The resources used to localize the root object, or {@code null} if
     * the root object was not localized.
     */
    @SuppressWarnings("Duplicates")
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //set text to center
        fileLabel.setAlignment(Pos.CENTER);
        sceneCollection = new ArrayList<>(List.of(scene0,scene1,scene2,scene3));
        icon = new FontIcon();
        icon.setIconLiteral("anto-arrow-right");
        icon.setIconSize(15);
        icon.setIconColor(Paint.valueOf("#1CAEEE"));
        //combo box declaration and custom cell factory
        decryptionMethodComboBox = new ComboBox<>();
        decryptionMethodComboBox.setCellFactory(e -> new ListCell<>(){
            @Override
            protected void updateItem(DecryptionMethod item, boolean empty) {
                //maintain parent implementation and mouse logic
                super.updateItem(item, empty);
                if(empty){
                    //set text to null if an empty cel
                    setText(null);
                    return;
                }
                setText(item.name());
            }
        });
        //fill with enum collection
        decryptionMethodComboBox.setItems(FXCollections.observableArrayList(DecryptionMethod.Password, DecryptionMethod.Key));
        //set pref size and make all others adhere to pref width and size requirements
        decryptionMethodComboBox.setPrefWidth(130);
        decryptionMethodComboBox.setPrefHeight(25);
        decryptionMethodComboBox.setMinSize(decryptionMethodComboBox.getPrefWidth(),decryptionMethodComboBox.getPrefHeight());
        decryptionMethodComboBox.setMaxSize(decryptionMethodComboBox.getPrefWidth(),decryptionMethodComboBox.getPrefHeight());
        //add blue background from Main.css
        decryptionMethodComboBox.getStyleClass().add("Dnc-background");
        //select first item from collection as default selection
        decryptionMethodComboBox.getSelectionModel().selectFirst();
        //center x and center the y on the baseline of the pane
        decryptionMethodComboBox.setLayoutX(pane.getPrefWidth()/2.0 - decryptionMethodComboBox.getPrefWidth()/2.0);
        decryptionMethodComboBox.setLayoutY(pane.getPrefHeight()/2.0 - decryptionMethodComboBox.getPrefHeight()-10);
        //create progress indication
        indicator = new ProgressIndicator();
        //set pref size and base all others off of it
        indicator.setPrefSize(100,80);
        indicator.setMinSize(indicator.getPrefWidth(),indicator.getPrefHeight());
        indicator.setMaxSize(indicator.getPrefWidth(),indicator.getPrefHeight());
        //add blue spinner style class from Main.css
        indicator.getStyleClass().add("BlueSpinner");
        //center indicator in the center of the pane
        indicator.setLayoutY(pane.getPrefHeight()/2.0 - indicator.getPrefHeight()/2.0);
        indicator.setLayoutX(pane.getPrefWidth()/2.0 - indicator.getPrefWidth()/2.0);
        //set navigation buttons to change scenes
        nextButton.setOnAction(e-> changeScene(++SceneId));
        backButton.setOnAction(e -> changeScene(--SceneId));
        //init choose file button to interact with native file chooser
        initChooseFileButton();

        //drag drop event occurred in the main menu, advance to second menu step
        preDrawl = false;
        if(encryptedFile!=null){
            fileLabel.setText(encryptedFile.getFileName()+encryptedFile.getFileType());
            Tooltip tooltip = new Tooltip(encryptedFile.getFile().getPath());
            fileLabel.setTooltip(tooltip);
            nextButton.setDisable(false);
            nextButton.fire();
        }
    }

    private void handleEncryptionMethod(){
        DecryptionMethod decryptionMethod = decryptionMethodComboBox.getSelectionModel().getSelectedItem();
        handleFileCreation(decryptionMethod);
    }
    private void initChooseFileButton(){
        //on click action
        chooseFileButton.setOnAction(e -> {
            //create a native file chooser and ask for a file
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose file for decryption");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Encrypt File (*.enc)","*.enc"));
            File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
            //returns null if user did not select a file
            if(file!=null){
                try {
                    //try to read file if fail prompt an error dialog
                    encryptedFile = EncryptedFile.initRead(file);
                    fileLabel.setText(file.getName());
                    Tooltip tooltip = new Tooltip(encryptedFile.getFile().getPath());
                    fileLabel.setTooltip(tooltip);
                    //allow for user to advance to next menu item
                    nextButton.setDisable(false);
                } catch (UnsupportedFileException ex) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Warning File is not supported");
                    alert.setHeaderText("File given does contain Proper File header");
                    MainController.createErrorDialog(ex, alert);
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("IoError occurred");
                    alert.setHeaderText("an error occurred while processing your file");
                    MainController.createErrorDialog(ex, alert);
                }
            }
        });
    }

    /**
     * This method
     * @param method enum that dedicates weather to use Password based decryption or Key based decryption
     */
    private void handleFileCreation(DecryptionMethod method){
        //secret is need in both password and key based encryption
        String secret = "";
        final boolean passOrKey = method == DecryptionMethod.Password;
        switch (method){
            //if key ask for key file, and if password ask for password through password dialog
            case Key -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Choose Key");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Key file (*.key)","*.key"));
                File file = fileChooser.showOpenDialog(pane.getScene().getWindow());
                if(file!=null){
                    try {
                        secret = Files.readString(file.toPath());
                    }catch (IOException e){
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Failed to read key");
                        alert.setHeaderText("An IOError occurred while reading the key file given");
                        MainController.createErrorDialog(e,alert);
                        return;
                    }
                }
            }
            case Password -> {
                Optional<String> stringOptional = createPasswordDialog().showAndWait();
                if(stringOptional.isEmpty() || stringOptional.get().trim().isEmpty()){
                    return;
                }
                secret = stringOptional.get();
            }
        }
        String finalSecret = secret;
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                Encryptor encryptor;
                if (passOrKey) {
                    switch (encryptedFile.getEncryptionType()) {
                        case AES -> encryptor = AESEncryptor.init_password(finalSecret, encryptedFile.getIV());
                        case XOR -> throw new UnsupportedOperationException("Xor encryption not implemented");
                        case BLOWFISH -> throw new UnsupportedOperationException("Blowfish encryption not implemented");
                        default -> throw new IllegalStateException("Not possible state");
                    }
                } else {
                    switch (encryptedFile.getEncryptionType()) {
                        case AES -> encryptor = AESEncryptor.init_key(finalSecret, encryptedFile.getIV());
                        case XOR -> throw new UnsupportedOperationException("Xor encryption not implemented");
                        case BLOWFISH -> throw new UnsupportedOperationException("Blowfish encryption not implemented");
                        default -> throw new IllegalStateException("Not possible state");
                    }
                }
                return encryptor.decrypt(encryptedFile);
            }
        };
        changeScene(++SceneId);
        task.setOnFailed(e -> Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Unable to decrypt File");
                alert.setHeaderText("Decryption precess failed");
                MainController.createErrorDialog(task.getException()==null?new Exception("Something went wrong!"):(Exception)task.getException(),alert);
                changeScene(0);
        }));
        task.setOnSucceeded(e -> Platform.runLater(() -> handleFileSaving(task.getValue())));
        Thread thread = new Thread(task);
        thread.start();
    }
    private void handleFileSaving(Path path){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(encryptedFile.getFileName()+encryptedFile.getFileType());
        File file = fileChooser.showSaveDialog(pane.getScene().getWindow());
        //if user selects file save file into destination
        if(file!=null){
            try {
                //user is warned beforehand that file is going to be overwritten, so we should assume they would like this operation to occur
                Files.copy(path, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                //once file has been copied delete the temp resource
                Files.deleteIfExists(path);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Saved file");
                alert.setHeaderText("Saved: " + file.getName() + " into directory " + file.getParent());
                alert.show();
                //render home menu
                changeScene(0);
                return;
            }catch (IOException e){
                //if error occurs during io write or delete display error dialog
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Failed to cary out File operation");
                alert.setHeaderText("Failed during file write/delete operation");
                MainController.createErrorDialog(e,alert);
                changeScene(0);
                return;
            }
        }
        try {
            Files.deleteIfExists(path);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Files not saved");
            alert.setHeaderText("Deleted Allocated temp file");
            alert.setContentText("returning to home screen");
            alert.show();
        }catch (IOException e){
            Alert alert =new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Failed to delete resource");
            alert.setHeaderText("Failed to delete allocated temp file");
            alert.setContentText("returning to home screen");
            MainController.createErrorDialog(e,alert);
        }finally {
            changeScene(0);
        }

    }

    /**
     * Creates a password dialog, using Custom created javaFX dialog
     * @return password dialog that houses a string optional
     */
    @SuppressWarnings("Duplicates")
    private Dialog<String> createPasswordDialog(){
        //custom graphic
        FontIcon icon = new FontIcon();
        icon.setIconLiteral("bxs-key");
        icon.setIconSize(35);
        icon.setIconColor(Paint.valueOf("#F7CA18"));
        //creating dialog
        Dialog<String> dialog = new Dialog<>();
        dialog.getDialogPane().setMinSize(200,150);
        dialog.setTitle("Decryption Password");
        dialog.setHeaderText("Enter password used during encryption");
        //set graphic
        dialog.setGraphic(icon);
        //add cancel and ok button
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        //create password field and label
        PasswordField field = new PasswordField();
        //Hbox used to house content
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setSpacing(8);
        hBox.getChildren().addAll(new Label("Password:"), field);
        dialog.getDialogPane().setContent(hBox);
        dialog.setResultConverter(button -> {
            if(button == ButtonType.OK){
                return field.getText();
            }
            return null;
        });
        return  dialog;
    }

    private void changeScene(int sceneId){
        sceneCollection.get(sceneId).drawScene();
    }

    /**
     * Used before screen has been rendered to show that drag drop was used to select file
     * If screen has been rendered this operation is ignored
     * @param file the file selected by the user
     * @throws IOException if a general io error occurs
     * @throws UnsupportedFileException if the given file does not follow the standardizes file header
     */
    protected void setEncryptedFile(File file) throws IOException, UnsupportedFileException {
        if(preDrawl){
            encryptedFile = EncryptedFile.initRead(file);
        }
    }
}
