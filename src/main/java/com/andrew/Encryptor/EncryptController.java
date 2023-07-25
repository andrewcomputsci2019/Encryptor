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
//javafx and Encryptor services import
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
//inbuilt java imports
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
//static imports
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class EncryptController implements Initializable {
    private boolean preDrawl = true;

    private enum EncryptionMethod {Password, Key}

    @FXML
    private Button chooseFileButton;
    @FXML
    private Button backButton;
    @FXML
    private Button nextButton;
    @FXML
    private Pane pane;
    @FXML
    private Label fileLabel;
    private ArrayList<DrawScene> sceneCollection;
    private EncryptedFile encryptedFile;
    private ComboBox<EncryptedFile.EncryptionType> encryptionType;
    private ComboBox<EncryptionMethod> encryptionMethod;
    private ProgressIndicator indicator;

    private FontIcon icon;
    //by default, we are on scene 1 out of 4
    private int SceneId = 1;

    public EncryptController() {

    }

    //for drag and drop behavior
    private final DrawScene scene0 = () -> {
        try {
            AnchorPane anchorPane = FXMLLoader.load(EncryptController.class.getResource("Main.fxml"));
            pane.getScene().setRoot(anchorPane);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Failed to create Scene");
            alert.setHeaderText("Failed to create Main Menu Scene");
            MainController.createErrorDialog(e, alert);
        }
    };
    private final DrawScene scene1 = () -> {
        nextButton.setOnAction(e-> changeScene(++SceneId));
        pane.getChildren().setAll(backButton, nextButton, chooseFileButton, fileLabel);
    };
    private final DrawScene scene2 = () -> {
        //add code to add FontIcon back onto next button
        nextButton.setText(null);
        nextButton.setGraphic(icon);
        nextButton.setOnAction(e -> handleEncryptionType());
        pane.getChildren().setAll(backButton, nextButton, encryptionType);

    };
    private final DrawScene scene3 = () -> {
        //add code to remove next button graphic and put text encrypt
        pane.getChildren().setAll(backButton, nextButton, encryptionMethod);
        //add code here for next to handle changing screens as well handling task creation
        nextButton.setGraphic(null);
        nextButton.setTextFill(Paint.valueOf("#A5DE37"));
        nextButton.setTextAlignment(TextAlignment.CENTER);
        nextButton.setText("Encrypt");
        nextButton.setOnAction(event -> handleEncryptionMethod());
    };
    private final DrawScene scene4 = () -> pane.getChildren().setAll(indicator);

    @SuppressWarnings("Duplicates")
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //set up next button font icon
        fileLabel.setAlignment(Pos.CENTER);
        sceneCollection = new ArrayList<>(List.of(scene0, scene1, scene2, scene3, scene4));
        icon = new FontIcon();
        icon.setIconLiteral("anto-arrow-right");
        icon.setIconSize(15);
        icon.setIconColor(Paint.valueOf("#A5DE37"));
        //create encryptionType dropdown
        encryptionType = new ComboBox<>();
        encryptionType.setCellFactory(e -> new ListCell<>() {
            @Override
            protected void updateItem(EncryptedFile.EncryptionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(item.name());
            }
        });
        encryptionType.setItems(FXCollections.observableArrayList(EncryptedFile.EncryptionType.AES));
        encryptionType.setPrefWidth(130);
        encryptionType.setPrefHeight(25);
        encryptionType.setMinSize(encryptionType.getPrefWidth(), encryptionType.getPrefHeight());
        encryptionType.setMaxSize(encryptionType.getPrefWidth(), encryptionType.getPrefHeight());
        encryptionType.getStyleClass().add("Enc-background");
        encryptionType.getSelectionModel().selectFirst();
        //centering equation, parent.width/2  - node.width/2
        encryptionType.setLayoutX(pane.getPrefWidth() / 2.0 - encryptionType.getPrefWidth() / 2.0);
        encryptionType.setLayoutY(pane.getPrefHeight() / 2.0 - encryptionType.getPrefHeight() - 10);
        //create a indeterminate progress indicator
        indicator = new ProgressIndicator();
        indicator.setPrefSize(100, 80);
        indicator.setMinSize(indicator.getPrefWidth(), indicator.getPrefHeight());
        indicator.setMaxSize(indicator.getPrefWidth(), indicator.getPrefHeight());
        indicator.getStyleClass().add("GreenSpinner");
        indicator.setLayoutX(pane.getPrefWidth() / 2.0 - indicator.getPrefWidth() / 2.0);
        indicator.setLayoutY(pane.getPrefHeight() / 2.0 - indicator.getPrefHeight() / 2.0);

        encryptionMethod = new ComboBox<>();
        encryptionMethod.setPrefSize(130, 25);
        encryptionMethod.setMinSize(encryptionMethod.getPrefWidth(), encryptionMethod.getPrefHeight());
        encryptionMethod.setMaxSize(encryptionMethod.getPrefWidth(), encryptionMethod.getPrefHeight());
        encryptionMethod.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(EncryptionMethod item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(item.name());
            }
        });
        encryptionMethod.setItems(FXCollections.observableArrayList(EncryptionMethod.Password, EncryptionMethod.Key));
        encryptionMethod.getSelectionModel().selectFirst();
        encryptionMethod.getStyleClass().add("Enc-background");
        //centered on center baseline
        encryptionMethod.setLayoutY(pane.getPrefHeight() / 2.0 - encryptionMethod.getPrefHeight() - 10);
        encryptionMethod.setLayoutX(pane.getPrefWidth() / 2.0 - encryptionMethod.getPrefWidth() / 2.0);
        backButton.setOnAction(e -> changeScene(--SceneId));
        nextButton.setOnAction(e -> changeScene(++SceneId));
        initChooseFileButton();
        preDrawl = false;
        if (encryptedFile != null) {
            fileLabel.setText(encryptedFile.getFileName() + encryptedFile.getFileType());
            Tooltip tooltip = new Tooltip(encryptedFile.getFile().getPath());
            fileLabel.setTooltip(tooltip);
            nextButton.setDisable(false);
            nextButton.fire();
        }
    }


    private void changeScene(int sceneId) {
        sceneCollection.get(sceneId).drawScene();
    }

    private void initChooseFileButton() {
        chooseFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose File for encryption");
            File file = fileChooser.showOpenDialog(chooseFileButton.getScene().getWindow());
            if (file != null) {
                String fileName = Utils.getFileName(file.getName());
                String fileExtension = Utils.getFileExtension(file.getName());
                fileLabel.setText(fileName + fileExtension);
                fileLabel.setTooltip(new Tooltip(file.getPath()));
                encryptedFile = new EncryptedFile(fileName, fileExtension, EncryptedFile.EncryptionType.AES, file);
                nextButton.setDisable(false);
            }
        });
    }

    protected void setEncryptedFile(File file) {
        if (preDrawl) {
            this.encryptedFile = new EncryptedFile(Utils.getFileName(file.getName()), Utils.getFileExtension(file.getName()), EncryptedFile.EncryptionType.AES, file);
        } else {
            throw new IllegalStateException("Encrypted file can no longer be set");
        }
    }

    private void handleEncryptionType() {
        EncryptedFile.EncryptionType type = encryptionType.getSelectionModel().getSelectedItem();
        encryptedFile.setEncryptionType(type);
        changeScene(++SceneId);
    }

    private void handleEncryptionMethod() {
        String password = "";
        if (encryptionMethod.getSelectionModel().getSelectedItem() == EncryptionMethod.Password) {
            Optional<String> stringOptional = createPasswordDialog().showAndWait();
            if (stringOptional.isEmpty() || stringOptional.get().trim().isEmpty()) {
                return;
            }
            password = stringOptional.get();
        }
        String finalPassword = password;

        Task<PathPair<Path, Path>> task = new Task<>() {
            @Override
            protected PathPair<Path, Path> call() throws GeneralSecurityException, IOException {
                Encryptor encryptor;
                switch (encryptionMethod.getSelectionModel().getSelectedItem()) {
                    case Key -> {
                        switch (encryptedFile.getEncryptionType()) {
                            case AES -> encryptor = AESEncryptor.init();
                            case BLOWFISH ->
                                    throw new UnsupportedOperationException("Blowfish encryption not implemented");
                            case XOR -> throw new UnsupportedOperationException("Xor encryption not implemented");
                            default -> throw new IllegalStateException("Default value should not be possible");
                        }
                    }
                    case Password -> {
                        switch (encryptedFile.getEncryptionType()) {
                            case AES -> encryptor = AESEncryptor.init(finalPassword);
                            case BLOWFISH ->
                                    throw new UnsupportedOperationException("Blowfish encryption not implemented");
                            case XOR -> throw new UnsupportedOperationException("Xor encryption not implemented");
                            default -> throw new IllegalStateException("Default value should not be possible");
                        }
                    }
                    default -> throw new IllegalStateException("Default value should not be possible");
                }
                return encryptor.encrypt(encryptedFile);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> HandleFileSaving(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Unable to process file");
            alert.setContentText("failed to either create resource or a runtime error has occurred");
            MainController.createErrorDialog(task.getException()==null?new Exception("Something went wrong!"):(Exception)task.getException(), alert);
            //return to main menu after alert has been closed
            changeScene(0);
        }));
        Thread thread = new Thread(task::run);
        thread.start();
        changeScene(++SceneId);
    }

    @SuppressWarnings("Duplicates")
    private Dialog<String> createPasswordDialog() {
        FontIcon icon = new FontIcon();
        icon.setIconLiteral("bxs-key");
        icon.setIconSize(35);
        icon.setIconColor(Paint.valueOf("#F7CA18"));
        Dialog<String> dialog = new Dialog<>();
        dialog.getDialogPane().setMinSize(200,150);
        dialog.setTitle("Encryption password");
        dialog.setHeaderText("Remember this password!");
        dialog.setGraphic(icon);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        PasswordField passwordField = new PasswordField();
        HBox content = new HBox();
        content.setAlignment(Pos.CENTER_LEFT);
        content.setSpacing(8);
        content.getChildren().addAll(new Label("Password:"), passwordField);
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return passwordField.getText();
            }
            return null;
        });
        return dialog;
    }

    private void HandleFileSaving(PathPair<Path, Path> pathPair) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("save encrypted File");
        fileChooser.setInitialFileName(encryptedFile.getFileName() + ".enc");
        File file = fileChooser.showSaveDialog(pane.getScene().getWindow());
        if (file != null) {
            Path dest = file.toPath();
            Path root = dest.getParent();
            try {
                Files.copy(pathPair.getFile(), dest, REPLACE_EXISTING);
                //if key file present
                if(pathPair.getKey() != null){
                    Files.copy(pathPair.getKey(), root.resolve(Utils.getFileName(file.getName())+".key"), REPLACE_EXISTING);
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Files saved");
                alert.setHeaderText("Files were successfully saved into directory: " + root.toString());
                alert.show();
                pathPair.deleteFiles();
                changeScene(0);
                return;
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Failed to copy files");
                alert.setHeaderText("Failed to copy new encrypted files into destination");
                MainController.createErrorDialog(e, alert);
                pathPair.deleteFiles();
                changeScene(0);
                return;
            }
        }
        pathPair.deleteFiles();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("File not saved");
        alert.setHeaderText("Temp files were deleted");
        alert.setContentText("returning to home screen");
        alert.show();
        changeScene(0);
    }
}
