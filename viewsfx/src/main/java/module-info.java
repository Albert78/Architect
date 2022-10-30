module de.dh.utils.fx.viewsfx {
    requires transitive javafx.base;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires org.slf4j;
    requires java.xml.bind;

    exports de.dh.utils.fx.viewsfx;
    exports de.dh.utils.fx.viewsfx.io;
    opens de.dh.utils.fx.viewsfx.io;
}