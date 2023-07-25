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

import com.andrew.Encryptor.EncryptorService.Utils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private Button encryptButton;
    @FXML
    private Button decryptButton;
    @FXML
    private AnchorPane pane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //init encrypt button functionality.
            encryptButton.setOnAction(e -> {
                try {
                    //load Encrypt View and attach controller
                    FXMLLoader loader = new FXMLLoader(MainController.class.getResource("Encrypt.fxml"));
                    loader.setController(new EncryptController());
                    AnchorPane node = loader.load();
                    encryptButton.getScene().setRoot(node);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Action Failed");
                    alert.setHeaderText("Failed to create Encrypt Scene");
                    createErrorDialog(ex, alert);
                }
            });
            //init decrypt Button functionality
            decryptButton.setOnAction(e -> {
                try{
                    //load Decrypt View and attach controller
                    FXMLLoader loader = new FXMLLoader(this.getClass().getResource("Decrypt.fxml"));
                    loader.setController(new DecryptController());
                    AnchorPane node = loader.load();
                    decryptButton.getScene().setRoot(node);
                }catch (Exception ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Action Failed");
                    alert.setHeaderText("Failed to create Decrypt Scene");
                    createErrorDialog(ex, alert);
                }
            });
            pane.setOnDragOver(e -> {
                if(e.getDragboard().hasFiles() && e.getDragboard().getFiles().size()==1) {
                    e.acceptTransferModes(TransferMode.COPY);
                }else{
                    e.acceptTransferModes(TransferMode.NONE);
                }
                e.consume();
            });
            pane.setOnDragDropped(e -> {
                try {
                    if (!Utils.getFileExtension(e.getDragboard().getFiles().get(0).getName()).equals(".enc")) {
                        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("Encrypt.fxml"));
                        EncryptController controller = new EncryptController();
                        controller.setEncryptedFile(e.getDragboard().getFiles().get(0));
                        loader.setController(controller);
                        AnchorPane anchorPane = loader.load();
                        pane.getScene().setRoot(anchorPane);
                    } else {
                        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("Decrypt.fxml"));
                        DecryptController controller = new DecryptController();
                        controller.setEncryptedFile(e.getDragboard().getFiles().get(0));
                        loader.setController(controller);
                        AnchorPane anchorPane = loader.load();
                        pane.getScene().setRoot(anchorPane);
                    }
                    e.setDropCompleted(true);
                    e.consume();
                }catch (Exception exception){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Failed to create Scene");
                    createErrorDialog(exception, alert);
                }
            });

    }

    public static void createErrorDialog(Exception ex, Alert alert) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        Label label = new Label("exception stack trace");
        TextArea textArea  = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea,Priority.ALWAYS);
        GridPane content = new GridPane();
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(label,0,0);
        content.add(textArea,0,1);
        alert.getDialogPane().setExpandableContent(content);
        alert.showAndWait();
    }

}
