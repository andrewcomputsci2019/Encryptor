module com.andrew.UD {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.ikonli.javafx;

    opens com.andrew.ud3 to javafx.fxml;
    exports com.andrew.ud3;
    exports com.andrew.ud3.EncryptorService;
}