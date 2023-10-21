package de.dh.utils.fx.shapes;

import de.dh.utils.Vector2D;
import de.dh.utils.fx.shapes.Arrow;
import de.dh.utils.fx.shapes.CurvedArrow;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;

public class CurvedArrowTest extends Application {
    // A draggable anchor displayed around a point.
    protected class DraggableAnchor extends Circle {
        public DraggableAnchor(Color color, ObjectProperty<Vector2D> positionProperty) {
            super(positionProperty.get().getX(), positionProperty.get().getY(), 10);
            setFill(color.deriveColor(1, 1, 1, 0.5));
            setStroke(color);
            setStrokeWidth(2);
            setStrokeType(StrokeType.OUTSIDE);

            centerXProperty().addListener((prop, oldV, newV) -> {
                positionProperty.set(positionProperty.get().withX(getCenterX()));
            });
            centerYProperty().addListener((prop, oldV, newV) -> {
                positionProperty.set(positionProperty.get().withY(getCenterY()));
            });
            enableDrag();
        }

        // Make a node movable by dragging it around with the mouse.
        protected void enableDrag() {
            final var dragDelta = new Object() {
                double x, y;
            };
            setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    // Record a delta distance for the drag and drop operation.
                    dragDelta.x = getCenterX() - mouseEvent.getX();
                    dragDelta.y = getCenterY() - mouseEvent.getY();
                    getScene().setCursor(Cursor.MOVE);
                }
            });
            setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    getScene().setCursor(Cursor.HAND);
                }
            });
            setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    double newX = mouseEvent.getX() + dragDelta.x;
                    if (newX > 0 && newX < getScene().getWidth()) {
                        setCenterX(newX);
                    }
                    double newY = mouseEvent.getY() + dragDelta.y;
                    if (newY > 0 && newY < getScene().getHeight()) {
                        setCenterY(newY);
                    }
                }
            });
            setOnMouseEntered(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    if (!mouseEvent.isPrimaryButtonDown()) {
                        getScene().setCursor(Cursor.HAND);
                    }
                }
            });
            setOnMouseExited(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    if (!mouseEvent.isPrimaryButtonDown()) {
                        getScene().setCursor(Cursor.DEFAULT);
                    }
                }
            });
        }
    }

    public CurvedArrow initDockArrowCurve() {
        CurvedArrow result = new CurvedArrow();
        result.setSourcePosition(new Vector2D(100, 100));
        result.setTargetPosition(new Vector2D(300, 100));

        CubicCurve curve = result.getCurve();
        curve.setStroke(Color.FORESTGREEN);
        curve.setStrokeWidth(4);
        curve.setFill(Color.CORNSILK.deriveColor(0, 1.2, 1, 0.6));

        Arrow arrowTip = result.getArrowTip();
        arrowTip.setFill(Color.web("#ff0900"));
        return result;
    }

    @Override
    public void start(final Stage stage) throws Exception {
        CurvedArrow curvedArrow = initDockArrowCurve();

        DraggableAnchor start = new DraggableAnchor(Color.PALEGREEN, curvedArrow.getSourcePositionProperty());
        DraggableAnchor end = new DraggableAnchor(Color.TOMATO, curvedArrow.getTargetPositionProperty());

        Group root = new Group();
        root.getChildren().addAll(curvedArrow, start, end);

        stage.setTitle("Dock Anchor Behavior - Arrow Test");
        stage.setScene(new Scene(root, 400, 400, Color.ALICEBLUE));
        stage.show();
    }


    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
