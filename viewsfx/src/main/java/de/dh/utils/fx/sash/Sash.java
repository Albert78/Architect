/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021 - 2023  Daniel Höh
 *
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
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>
 *******************************************************************************/
package de.dh.utils.fx.sash;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * SplitPane-like class for a fixed number of two children with improved accessibility of internal functionality and members.
 */
public class Sash extends Pane {
    /**
     * Behavior to make a {@link Sash} act in horizontal or vertical direction. The behavior can be installed and uninstalled.
     */
    public abstract class BaseSashBehavior {
        protected static class DragContext {
            protected double mInitialPos;
            protected double mInitialMousePos;

            public void setInitialValues(double initialPos, double initialMousePos) {
                mInitialPos = initialPos;
                mInitialMousePos = initialMousePos;
            }

            public double getInitialPos() {
                return mInitialPos;
            }

            public double getInitialMousePos() {
                return mInitialMousePos;
            }
        }

        protected final ChangeListener<? super Bounds> LAYOUT_BOUNDS_CHANGE_HANDLER = new ChangeListener<>() {
            private double getAbsDividerDelta(Bounds bounds, double dividerPosition, DividerPositionPolicy dividerPositionPolicy) {
                double layoutSize = mBehavior.getSize(bounds);
                return layoutSize == 0.0 ? -1 : switch (dividerPositionPolicy) {
                case Start -> layoutSize * dividerPosition;
                case End -> layoutSize * (1 - dividerPosition);
                default -> -1;
                };
            }

            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
              double oldAbsDividerDelta = getAbsDividerDelta(oldValue, getDividerPosition(), getDividerPositionPolicy());
              if (oldAbsDividerDelta == -1) {
                  return;
              }
              switch (getDividerPositionPolicy()) {
              case Start:
                  setDividerPosition(oldAbsDividerDelta / getLayoutSize(Sash.this));
                  break;
              case End:
                  double size = getLayoutSize(Sash.this);
                  setDividerPosition((size - oldAbsDividerDelta) / size);
                  break;
              default:
                  break;
              }
            }
        };

        protected static final int DIVIDER_WIDTH = 8;

        protected final Pane mDividerPane;
        protected final Pane mFirstPane;
        protected final Pane mLastPane;

        protected BaseSashBehavior(Pane dividerPane, Pane firstPane, Pane lastPane) {
            mDividerPane = dividerPane;
            mFirstPane = firstPane;
            mLastPane = lastPane;
        }

        public double getWidth(Bounds bounds) {
            return bounds.getWidth();
        }

        public double getHeight(Bounds bounds) {
            return bounds.getHeight();
        }

        public double getMinWidth(Region r) {
            return r.minWidth(-1);
        }

        public double getMinHeight(Region r) {
            return r.minHeight(-1);
        }

        public double getMaxWidth(Region r) {
            return r.maxWidth(-1);
        }

        public double getMaxHeight(Region r) {
            return r.maxHeight(-1);
        }

        /**
         * Sets the size of our child nodes to the given divider position.
         */
        public abstract void layoutForPosition(double dividerPosition);

        /**
         * Gets the layout size of the given region in the direction of this sash layout.
         */
        protected abstract double getSize(Bounds bounds);

        /**
         * Gets the layout size of the given region in the direction of this sash layout.
         */
        protected double getLayoutSize(Region r) {
            return getSize(r.getLayoutBounds());
        }

        /**
         * Gets the min size of the given region in the direction of this sash layout.
         */
        protected abstract double getMinSize(Region r);

        /**
         * Gets the max size of the given region in the direction of this sash layout.
         */
        protected abstract double getMaxSize(Region r);

        /**
         * Gets the scene position of the given event in the direction of this sash layout.
         */
        protected abstract double getPosInScene(MouseEvent e);

        protected abstract void configureDivider();
        protected abstract void unconfigureDivider();

        public void install() {
            DragContext context = new DragContext();

            mDividerPane.setOnMousePressed(e -> {
                if (e.getButton() != MouseButton.PRIMARY) {
                    return;
                }
                context.setInitialValues(getLayoutSize(Sash.this) * getDividerPosition(), getPosInScene(e));
                e.consume();
            });

            mDividerPane.setOnMouseDragged(e -> {
                double delta = getPosInScene(e) - context.getInitialMousePos();
                dividerDraggedTo(context.getInitialPos() + delta);
                e.consume();
            });

            configureDivider();

            layoutBoundsProperty().addListener(LAYOUT_BOUNDS_CHANGE_HANDLER);
        }

        public void uninstall() {
            mDividerPane.setOnMousePressed(null);
            mDividerPane.setOnMouseMoved(null);
            unconfigureDivider();

            layoutBoundsProperty().removeListener(LAYOUT_BOUNDS_CHANGE_HANDLER);
        }

        protected double checkDividerPosition(double position) {
            double size = getLayoutSize(Sash.this);
            double minFirst = getMinSize(mFirstPane);
            double maxFirst = getMaxSize(mFirstPane);
            double minLast = getMinSize(mLastPane);
            double maxLast = getMaxSize(mLastPane);
            double dividerWidthHalf = DIVIDER_WIDTH / 2;

            double minPos = size == 0.0 ? 0 : Math.max(
                (minFirst + dividerWidthHalf) / size,
                (size - maxLast - dividerWidthHalf) / size);
            double maxPos = size == 0.0 ? 1 : Math.min(
                (maxFirst + dividerWidthHalf) / size,
                (size - minLast - dividerWidthHalf) / size);

            if (position < minPos || position < 0) {
                position = Math.max(minPos, 0);
            }
            if (position > maxPos || position > 1) {
                position = maxPos;
            }
            return position;
        }
    }

    public class HorizontalSashBehavior extends BaseSashBehavior {
        protected static final String[] H_DIVIDER_STYLE_CLASSES = {"horizontal-split-pane-divider", "split-pane-divider"};

        public HorizontalSashBehavior(Pane dividerPane, Pane firstPane, Pane lastPane) {
            super(dividerPane, firstPane, lastPane);
        }

        @Override
        public void layoutForPosition(double dividerPosition) {
            double width = getWidth(Sash.this.getLayoutBounds());
            double height = getHeight(Sash.this.getLayoutBounds());
            if (width == 0.0 || height == 0.0) {
                return;
            }
            double dividerStart = (width * dividerPosition) - DIVIDER_WIDTH / 2.0;
            Region.layoutInArea(mFirstPane,
                    0, 0,
                    dividerStart, height,
                    0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, mFirstPane.isSnapToPixel());

            Region.layoutInArea(mDividerPane,
                    dividerStart, 0,
                    DIVIDER_WIDTH, height,
                    0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, mDividerPane.isSnapToPixel());

            Region.layoutInArea(mLastPane,
                    dividerStart + DIVIDER_WIDTH, 0,
                    width - dividerStart - DIVIDER_WIDTH, height,
                    0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, mLastPane.isSnapToPixel());
        }

        @Override
        protected double getSize(Bounds bounds) {
            return getWidth(bounds);
        }

        @Override
        protected double getMinSize(Region r) {
            return getMinWidth(r);
        }

        @Override
        protected double getMaxSize(Region r) {
            return getMaxWidth(r);
        }

        @Override
        protected double getPosInScene(MouseEvent e) {
            return e.getSceneX();
        }

        @Override
        protected void configureDivider() {
            mDividerPane.setCursor(Cursor.H_RESIZE);
            mDividerPane.getStyleClass().setAll(H_DIVIDER_STYLE_CLASSES);
        }

        @Override
        protected void unconfigureDivider() {
            mDividerPane.setCursor(null);
            mDividerPane.getStyleClass().removeAll(H_DIVIDER_STYLE_CLASSES);
        }
    }

    public class VerticalSashBehavior extends BaseSashBehavior {
        protected static final String[] V_DIVIDER_STYLE_CLASSES = {"vertical-split-pane-divider", "split-pane-divider"};

        public VerticalSashBehavior(Pane dividerPane, Pane firstPane, Pane lastPane) {
            super(dividerPane, firstPane, lastPane);
        }

        @Override
        public void layoutForPosition(double dividerPosition) {
            double width = getWidth(Sash.this.getLayoutBounds());
            double height = getHeight(Sash.this.getLayoutBounds());
            if (width == 0.0 || height == 0.0) {
                return;
            }
            double dividerStart = (height * dividerPosition) - DIVIDER_WIDTH / 2.0;
            Region.layoutInArea(mFirstPane,
                    0, 0,
                    width, dividerStart,
                    0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, mFirstPane.isSnapToPixel());

            Region.layoutInArea(mDividerPane,
                    0, dividerStart,
                    width, DIVIDER_WIDTH,
                    0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, mDividerPane.isSnapToPixel());

            Region.layoutInArea(mLastPane,
                    0, dividerStart + DIVIDER_WIDTH,
                    width, height - dividerStart - DIVIDER_WIDTH,
                    0, Insets.EMPTY, true, true, HPos.CENTER, VPos.CENTER, mLastPane.isSnapToPixel());
        }

        @Override
        protected double getSize(Bounds bounds) {
            return getHeight(bounds);
        }

        @Override
        protected double getMinSize(Region r) {
            return getMinHeight(r);
        }

        @Override
        protected double getMaxSize(Region r) {
            return getMaxHeight(r);
        }

        @Override
        protected double getPosInScene(MouseEvent e) {
            return e.getSceneY();
        }

        @Override
        protected void configureDivider() {
            mDividerPane.setCursor(Cursor.V_RESIZE);
            mDividerPane.getStyleClass().setAll(V_DIVIDER_STYLE_CLASSES);
        }

        @Override
        protected void unconfigureDivider() {
            mDividerPane.setCursor(null);
            mDividerPane.getStyleClass().removeAll(V_DIVIDER_STYLE_CLASSES);
        }
    }

    protected final StackPane mDividerPane;
    protected final StackPane mFirstPane;
    protected final StackPane mLastPane;
    protected final ObjectProperty<Orientation> mOrientationProperty = new SimpleObjectProperty<>(Orientation.HORIZONTAL);
    protected final DoubleProperty mDividerPositionProperty = new SimpleDoubleProperty(0.5);
    protected final ObjectProperty<DividerPositionPolicy> mDividerPositionPolicyProperty = new SimpleObjectProperty<>(DividerPositionPolicy.Relative);
    protected BaseSashBehavior mBehavior;

    public Sash() {
        getStylesheets().add(Sash.class.getResource("sash.css").toExternalForm());
        mDividerPane = new StackPane();
        mFirstPane = new StackPane();
        mLastPane = new StackPane();
        getChildren().addAll(mFirstPane, mDividerPane, mLastPane);
        mOrientationProperty.addListener((obs, oldV, newV) -> {
            if (newV.equals(oldV)) {
                return;
            }
            resetBehavior();
        });
        mDividerPositionProperty.addListener((obs, oldV, newV) -> {
            if (oldV.equals(newV)) {
                return;
            }
            layoutChildren();
        });
        mDividerPositionPolicyProperty.addListener((obs, oldV, newV) -> {
            if (oldV.equals(newV)) {
                return;
            }
        });
        resetBehavior();
    }

    public Sash(Node firstItem, Node lastItem) {
        this();
        getFirstPane().getChildren().setAll(firstItem);
        getLastPane().getChildren().setAll(lastItem);
    }

    public ObjectProperty<Orientation> orientationProperty() {
        return mOrientationProperty;
    }

    /**
     * Gets the orientation of this sash. A horizontal sash will lay out its children left-to-right,
     * a vertical sash will lay out its children top-to-bottom.
     */
    public Orientation getOrientation() {
        return mOrientationProperty.get();
    }

    public void setOrientation(Orientation value) {
        mOrientationProperty.set(value);
    }

    public DoubleProperty dividerPositionProperty() {
        return mDividerPositionProperty;
    }

    /**
     * Gets the divider's position as a fraction of the total size, {@code 0.0 < value < 1.0}.
     */
    public double getDividerPosition() {
        return mDividerPositionProperty.get();
    }

    public void setDividerPosition(double value) {
        double pos = mBehavior.checkDividerPosition(value);
        mDividerPositionProperty.set(pos);
    }

    public ObjectProperty<DividerPositionPolicy> dividerPositionPolicyProperty() {
        return mDividerPositionPolicyProperty;
    }

    /**
     * Gets the divider's position policy which determines how the divider will
     * move when the size of the sash changes.
     */
    public DividerPositionPolicy getDividerPositionPolicy() {
        return mDividerPositionPolicyProperty.get();
    }

    public void setDividerPositionPolicy(DividerPositionPolicy value) {
        mDividerPositionPolicyProperty.set(value);
    }

    /**
     * Configures the divider to stick to the start border when the size changes and
     * sets the divider's coordinate distance to the given fixed distance from the start side.
     * The start border is the left or top border, depending on the orientation.
     */
    public void setDividerStartDistance(double distance) {
        setDividerPositionPolicy(DividerPositionPolicy.Start);
        setDividerPosition(distance / mBehavior.getLayoutSize(this));
    }

    public void setDividerPositionFromStart(double position) {
        setDividerPositionPolicy(DividerPositionPolicy.Start);
        setDividerPosition(position);
    }

    /**
     * Configures the divider to stick to the end border when the size changes and
     * sets the divider's coordinate distance to the given fixed distance from the end side.
     * The end border is the right or bottom border, depending on the orientation.
     */
    public void setDividerEndDistance(double distance) {
        setDividerPositionPolicy(DividerPositionPolicy.End);
        double size = mBehavior.getLayoutSize(this);
        setDividerPosition((size - distance) / size);
    }

    public void setDividerPositionFromEnd(double position) {
        setDividerPositionPolicy(DividerPositionPolicy.End);
        setDividerPosition(1 - position);
    }

    /**
     * Gets the left or top pane, depending on the orientation.
     */
    public StackPane getFirstPane() {
        return mFirstPane;
    }

    /**
     * Gets the right or bottom pane, depending on the orientation.
     */
    public StackPane getLastPane() {
        return mLastPane;
    }

    public Node getFirstChild() {
        ObservableList<Node> children = mFirstPane.getChildren();
        return children.isEmpty() ? null : children.get(0);
    }

    public void setFirstChild(Node item) {
        ObservableList<Node> children = mFirstPane.getChildren();
        if (item == null) {
            children.clear();
        } else {
            children.setAll(item);
        }
    }

    public Node getLastChild() {
        ObservableList<Node> children = mLastPane.getChildren();
        return children.isEmpty() ? null : children.get(0);
    }

    public void setLastChild(Node item) {
        ObservableList<Node> children = mLastPane.getChildren();
        if (item == null) {
            children.clear();
        } else {
            children.setAll(item);
        }
    }

    // ATTENTION: Called during constructor, be careful when overriding
    protected void resetBehavior() {
        if (mBehavior != null) {
            mBehavior.uninstall();
        }
        mBehavior = createBehavior();
        mBehavior.install();
        layoutChildren();
    }

    // ATTENTION: Called during constructor, be careful when overriding
    protected BaseSashBehavior createBehavior() {
        Orientation orientation = getOrientation();
        switch (orientation) {
        case HORIZONTAL:
            return new HorizontalSashBehavior(mDividerPane, mFirstPane, mLastPane);
        case VERTICAL:
            return new VerticalSashBehavior(mDividerPane, mFirstPane, mLastPane);
        }
        throw new RuntimeException("Orientation not defined");
    }

    protected void dividerDraggedTo(double absPosition) {
        double size = mBehavior.getLayoutSize(this);
        setDividerPosition(absPosition / size);
    }

    @Override
    protected void layoutChildren() {
        mBehavior.layoutForPosition(getDividerPosition());
    }
}
