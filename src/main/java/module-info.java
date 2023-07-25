module com.andrew.Encryptor {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;

    opens com.andrew.Encryptor to javafx.fxml;
    exports com.andrew.Encryptor;
    exports com.andrew.Encryptor.EncryptorService;
}