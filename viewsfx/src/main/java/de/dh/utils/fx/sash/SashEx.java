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

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SashEx extends Sash {
    protected static final Image LEFT_ARROW = new Image(HorizontalSashBehaviorEx.class.getResource("left-arrow.png").toExternalForm());
    protected static final Image RIGHT_ARROW = new Image(HorizontalSashBehaviorEx.class.getResource("right-arrow.png").toExternalForm());
    protected static final Image TOP_ARROW = new Image(HorizontalSashBehaviorEx.class.getResource("top-arrow.png").toExternalForm());
    protected static final Image DOWN_ARROW = new Image(HorizontalSashBehaviorEx.class.getResource("down-arrow.png").toExternalForm());

    protected static final String STYLE_CLASS_MAXIMIZE_RIGHT_PANE = "sash-maximize-right-pane";
    protected static final String STYLE_CLASS_MAXIMIZE_LEFT_PANE = "sash-maximize-left-pane";
    protected static final String STYLE_CLASS_MAXIMIZE_UPPER_PANE = "sash-maximize-upper-pane";
    protected static final String STYLE_CLASS_MAXIMIZE_LOWER_PANE = "sash-maximize-lower-pane";

    protected static final double BUTTON_IMAGE_SMALL_SIDE = 4;
    protected static final double BUTTON_IMAGE_WIDE_SIDE = 12;

    protected class HorizontalSashBehaviorEx extends HorizontalSashBehavior {
        protected final VBox mOuterPane;

        public HorizontalSashBehaviorEx(Pane dividerPane, Pane firstPane, Pane lastPane) {
            super(dividerPane, firstPane, lastPane);
            Pane bLeft = createImagePane(LEFT_ARROW, BUTTON_IMAGE_SMALL_SIDE, BUTTON_IMAGE_WIDE_SIDE);
            bLeft.setOnMouseClicked(event -> {
                maximizeLastPane();
            });
            bLeft.getStyleClass().add(STYLE_CLASS_MAXIMIZE_RIGHT_PANE);
            Pane bRight = createImagePane(RIGHT_ARROW, BUTTON_IMAGE_SMALL_SIDE, BUTTON_IMAGE_WIDE_SIDE);
            bRight.setOnMouseClicked(event -> {
                maximizeFirstPane();
            });
            bRight.getStyleClass().add(STYLE_CLASS_MAXIMIZE_LEFT_PANE);
            VBox buttons = new VBox(bLeft, bRight);
            buttons.setAlignment(Pos.CENTER);
            buttons.setPadding(new Insets(2, 0, 2, 0));
            buttons.setPrefSize(0, 0);
            buttons.setFillWidth(true);
            buttons.setCursor(Cursor.DEFAULT);
            // Flowpane necessary to prevent buttons pane fill the whole space - would cause mouse cursor to become DEFAULT in draggable area
            mOuterPane = new VBox(buttons);
            mOuterPane.setAlignment(Pos.TOP_CENTER);
            mOuterPane.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    restoreDividerState();
                }
            });
        }

        @Override
        protected void configureDivider() {
            mDividerPane.getChildren().add(mOuterPane);
            super.configureDivider();
        }

        @Override
        protected void unconfigureDivider() {
            super.unconfigureDivider();
            mDividerPane.getChildren().remove(mOuterPane);
        }
    }

    protected class VerticalSashBehaviorEx extends VerticalSashBehavior {
        protected final HBox mOuterPane;

        public VerticalSashBehaviorEx(Pane dividerPane, Pane firstPane, Pane lastPane) {
            super(dividerPane, firstPane, lastPane);
            Pane bTop = createImagePane(TOP_ARROW, BUTTON_IMAGE_WIDE_SIDE, BUTTON_IMAGE_SMALL_SIDE);
            bTop.setOnMouseClicked(event -> {
                maximizeLastPane();
            });
            bTop.getStyleClass().add(STYLE_CLASS_MAXIMIZE_LOWER_PANE);
            Pane bDown = createImagePane(DOWN_ARROW, BUTTON_IMAGE_WIDE_SIDE, BUTTON_IMAGE_SMALL_SIDE);
            bDown.setOnMouseClicked(event -> {
                maximizeFirstPane();
            });
            bDown.getStyleClass().add(STYLE_CLASS_MAXIMIZE_UPPER_PANE);
            HBox buttons = new HBox(bTop, bDown);
            buttons.setAlignment(Pos.CENTER);
            buttons.setPadding(new Insets(0, 2, 0, 2));
            buttons.setPrefSize(0, 0);
            buttons.setFillHeight(true);
            buttons.setCursor(Cursor.DEFAULT);
            // Flowpane necessary to prevent buttons pane fill the whole space - would cause mouse cursor to become DEFAULT in draggable area
            mOuterPane = new HBox(buttons);
            mOuterPane.setAlignment(Pos.CENTER_LEFT);
            mOuterPane.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    restoreDividerState();
                }
            });
        }

        @Override
        protected void configureDivider() {
            mDividerPane.getChildren().add(mOuterPane);
            super.configureDivider();
        }

        @Override
        protected void unconfigureDivider() {
            super.unconfigureDivider();
            mDividerPane.getChildren().remove(mOuterPane);
        }
    }

    public enum PaneSide {
        First, Last;

        private Pane getPane(Sash sash) {
            return this == First ? sash.getFirstPane() : sash.getLastPane();
        }

        public PaneSide opposite() {
            return this == First ? Last : First;
        }
    }

    public class StateOverride {
        protected final double mDividerPosition;
        protected final DividerPositionPolicy mDividerPositionPolicy;
        protected final PaneSide mMinimizedPane;
        protected final boolean mVisible;
        protected final double mMinWidth;
        protected final double mMinHeight;

        public StateOverride(PaneSide minimizedPane) {
            mDividerPosition = SashEx.this.getDividerPosition();
            mDividerPositionPolicy = SashEx.this.getDividerPositionPolicy();
            mMinimizedPane = minimizedPane;
            Pane pane = minimizedPane.getPane(SashEx.this);
            mVisible = pane.isVisible();
            mMinWidth = pane.getMinWidth();
            mMinHeight = pane.getMinHeight();

            pane.setVisible(false);
            pane.setMinSize(0, 0);
        }

        public PaneSide getMinimizedPane() {
            return mMinimizedPane;
        }

        public double getDividerPosition() {
            return mDividerPosition;
        }

        public DividerPositionPolicy getDividerPositionPolicy() {
            return mDividerPositionPolicy;
        }

        public void restore(boolean restorePosition) {
            if (restorePosition) {
                setDividerPosition(mDividerPosition);
            }
            setDividerPositionPolicy(mDividerPositionPolicy);
            Pane pane = mMinimizedPane.getPane(SashEx.this);
            pane.setVisible(mVisible);
            pane.setMinWidth(mMinWidth);
            pane.setMinHeight(mMinHeight);
        }
    }

    protected Optional<StateOverride> mStateOverride = Optional.empty();
    protected final BooleanProperty mShowMaximizeButtonsProperty = new SimpleBooleanProperty(true) {
        @Override
        protected void invalidated() {
            resetBehavior();
        }
    };

    public SashEx() {
        super();
    }

    public SashEx(Node firstItem, Node lastItem) {
        super(firstItem, lastItem);
    }

    public Optional<PaneSide> getMaximizedPane() {
        return mStateOverride.map(so -> so.getMinimizedPane().opposite());
    }

    public Optional<StateOverride> getStateOverride() {
        return mStateOverride;
    }

    public boolean isShowMaximizeButtons() {
        return mShowMaximizeButtonsProperty.get();
    }

    public void setShowMaximizeButtons(boolean value) {
        mShowMaximizeButtonsProperty.set(value);
    }

    public BooleanProperty showMaximizeButtonsProperty() {
        return mShowMaximizeButtonsProperty;
    }

    public void maximizeFirstPane() {
        if (mStateOverride.isPresent()) {
            switch (mStateOverride.get().getMinimizedPane()) {
            case Last:
                // Last already minimized, nothing to do
                return;
            case First:
                // If we have maximized the last pane, restore the state
                restoreDividerState();
                return;
            }
        }
        mStateOverride = Optional.of(disablePane(PaneSide.Last));
        setDividerPositionFromEnd(0);
    }

    public void maximizeLastPane() {
        if (mStateOverride.isPresent()) {
            switch (mStateOverride.get().getMinimizedPane()) {
            case First:
                // First already minimized, nothing to do
                return;
            case Last:
                // If we have maximized the first pane, restore the state
                restoreDividerState();
                return;
            }
        }
        mStateOverride = Optional.of(disablePane(PaneSide.First));
        setDividerPositionFromStart(0);
    }

    protected static Pane createImagePane(Image image, double width, double height) {
        ImageView iv = new ImageView(image);
        iv.setFitWidth(width);
        iv.setFitHeight(height);
        return new StackPane(iv);
    }

    @Override
    protected void dividerDraggedTo(double absPosition) {
        super.dividerDraggedTo(absPosition);
        if (mStateOverride.isPresent()) {
            mStateOverride.get().restore(false);
            mStateOverride = Optional.empty();
        }
    }

    protected StateOverride disablePane(PaneSide minimizedPane) {
        return new StateOverride(minimizedPane);
    }

    protected boolean restoreDividerState() {
        if (mStateOverride.isEmpty()) {
            return false;
        }
        mStateOverride.get().restore(true);
        mStateOverride = Optional.empty();
        return true;
    }

    // ATTENTION: Called during super constructor, so don't access fields of this class
    @Override
    protected BaseSashBehavior createBehavior() {
        if (mShowMaximizeButtonsProperty == null || isShowMaximizeButtons()) {
            Orientation orientation = getOrientation();
            switch (orientation) {
            case HORIZONTAL:
                return new HorizontalSashBehaviorEx(mDividerPane, mFirstPane, mLastPane);
            case VERTICAL:
                return new VerticalSashBehaviorEx(mDividerPane, mFirstPane, mLastPane);
            }
            throw new RuntimeException("Orientation not defined");
        } else {
            return super.createBehavior();
        }
    }
}
