module com.proj.ddos {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires transitive org.apache.logging.log4j;
    requires transitive org.apache.logging.log4j.core;

    opens com.proj.ddos to javafx.fxml;
    exports com.proj.ddos;
}
