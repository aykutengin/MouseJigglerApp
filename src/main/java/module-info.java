module org.engin {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.logging;

    opens org.engin to javafx.fxml;
    exports org.engin;
}