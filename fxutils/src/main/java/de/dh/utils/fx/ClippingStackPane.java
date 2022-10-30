package de.dh.utils.fx;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class ClippingStackPane extends StackPane {
    public ClippingStackPane() {
        installClippingPane();
    }

    public ClippingStackPane(Node... children) {
        super(children);
        installClippingPane();
    }

    protected void installClippingPane() {
        Rectangle clipRectangle = new Rectangle();
        setClip(clipRectangle);
        layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
           clipRectangle.setWidth(newValue.getWidth());
           clipRectangle.setHeight(newValue.getHeight());
        });
    }
}
