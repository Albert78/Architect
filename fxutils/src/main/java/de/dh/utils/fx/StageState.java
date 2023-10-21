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
// Copyright (c) 2017-2018 Flexpoint Tech Ltd. All rights reserved.
package de.dh.utils.fx;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

// Taken from https://flexpoint.tech/2017/12/20/restoring-window-sizes-in-javafx/
// Removed silly code, refactored class
public class StageState {
    private static Logger log = LoggerFactory.getLogger(StageState.class);

    protected static double MINIMUM_VISIBLE_WIDTH = 100;
    protected static double MINIMUM_VISIBLE_HEIGHT = 50;
    protected static double MARGIN = 50;
    protected static double BORDER_SIZE = 10;
    protected static double DEFAULT_WIDTH = 800;
    protected static double DEFAULT_HEIGHT = 600;

    protected final boolean mMaximized;
    protected final boolean mHidden;
    protected final double mX;
    protected final double mY;
    protected final double mWidth;
    protected final double mHeight;

    public StageState(double x, double y, double width, double height, boolean maximized, boolean hidden) {
        mX = x;
        mY = y;
        mWidth = width;
        mHeight = height;
        mMaximized = maximized;
        mHidden = hidden;
    }

    public static StageState fromSerializedState(String serializedState) {
        if (StringUtils.isEmpty(serializedState)) {
            return null;
        }
        double x = MARGIN;
        double y = MARGIN;
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        boolean maximized = false;
        boolean hidden = false;
        String[] kvps = serializedState.split(";");
        for (String kvpa : kvps) {
            String[] kvp = kvpa.split("=");
            if (kvp.length != 2) {
                log.warn("Cannot parse stage size property from '" + kvpa + "'");
                continue;
            }
            String k = kvp[0];
            String v = kvp[1];
            switch (k) {
            case "X":
                x = Double.parseDouble(v);
                break;
            case "Y":
                y = Double.parseDouble(v);
                break;
            case "Width":
                width = Double.parseDouble(v);
                break;
            case "Height":
                height = Double.parseDouble(v);
                break;
            case "Maximized":
                maximized = Boolean.parseBoolean(v);
                break;
            case "Hidden":
                hidden = Boolean.parseBoolean(v);
                break;
            default:
                log.warn("Unknown stage size property '" + kvpa + "'");
            }
        }
        return new StageState(x, y, width, height, maximized, hidden);
    }

    public String serializeState() {
        return "X=" + mX + ";Y=" + mY + ";Width="+ mWidth + ";Height=" + mHeight + ";Maximized=" + mMaximized + ";Hidden=" + mHidden;
    }

    /**
     * Reads the current state of the given stage and returns it.
     * Use this method to restore the correct size of the stage if it is in maximized state. See the comment in
     * {@link #fromStage(Stage)}.
     * @param oFormerStageState State of the stage from a former serialization. Only needed if the stage is currently in
     * maximized state to preserve the original width and height.
     */
    public static StageState fromStage(Stage stage, Optional<StageState> oFormerStageState) {
        return oFormerStageState
                        .map(formerStageState -> fromStage(stage,
                            formerStageState.getX(), formerStageState.getY(),
                            formerStageState.getWidth(), formerStageState.getHeight()))
                        .orElse(fromStage(stage));
    }

    /**
     * Reads the current state of the given stage and returns it.
     * Use this method to restore the correct size of the stage if it is in maximized state. See the comment in
     * {@link #fromStage(Stage)}.
     * The params denote the original values of the stage in normal (i.e. not maximized) state. Only needed if the stage is maximized.
     */
    public static StageState fromStage(Stage stage, double normalX, double normalY, double normalWidth, double normalHeight) {
        StageState state = fromStage(stage);
        if (state.isMaximized()) {
            return state.withNormalValues(normalX, normalY, normalWidth, normalHeight);
        }
        return state;
    }

    /**
     * Reads the current state of the given stage and returns it. Note that this method
     * is not able to read the "unmaximized" width and height of the stage if the stage is currently
     * maximized. That will result in a wrong X, Y, width and height in that case, i.e. if the stage is
     * reset to the returned stage state in that case, switching from maximized to normal stage
     * will change the state but will leave the stage size in the maximized size instead of returning
     * to the size the stage had before maximizing. To solve that problem, the caller should maintain
     * the unmaximized size of the stage, if possible, and call {@link #fromStage(Stage, double, double, double, double)}.
     */
    public static StageState fromStage(Stage stage) {
        return new StageState(
            stage.getX(),
            stage.getY(),
            stage.getWidth(),
            stage.getHeight(),
            stage.isMaximized(),
            !stage.isShowing());
    }

    /**
     * Convenience method to build an optional stage state from a nullable config string.
     */
    public static Optional<StageState> buildStageState(String savedState) {
        return Optional.ofNullable(savedState).map(stateStr -> StageState.fromSerializedState(stateStr));
    }

    public void applyToStage(Stage stage) {
        // First, restore the size and position of the stage.
        resizeAndPosition(stage);
        Platform.runLater(() -> {
            checkStageVisible(stage);
        });
    }

    public StageState withNormalValues(double x, double y, double width, double height) {
        return new StageState(x, y, width, height, mMaximized, mHidden);
    }

    public boolean isMaximized() {
        return mMaximized;
    }

    public boolean isHidden() {
        return mHidden;
    }

    public double getX() {
        return mX;
    }

    public double getY() {
        return mY;
    }

    public double getWidth() {
        return mWidth;
    }

    public double getHeight() {
        return mHeight;
    }

    protected void resizeAndPosition(Stage stage) {
        // This sequence works under Windows 11 with JavaFX 20
        // Without calling stage.hide(), stage.setMaximized() does not maximize the stage.
        stage.hide();
        stage.setX(getX());
        stage.setY(getY());
        stage.setWidth(getWidth());
        stage.setHeight(getHeight());
        stage.setMaximized(isMaximized());
        if (!isHidden()) {
            stage.show();
        }
    }

    protected void checkStageVisible(Stage stage) {
        // If the stage is not visible in any of the current screens, relocate it the primary screen
        if (isWindowIsOutOfBounds(stage)) {
            moveToPrimaryScreen(stage);
        }
    }

    protected boolean isWindowIsOutOfBounds(Stage stage) {
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            double stageX = stage.getX();
            double stageY = stage.getY();
            if (stageX + stage.getWidth() - MINIMUM_VISIBLE_WIDTH >= bounds.getMinX() &&
                    stageX + MINIMUM_VISIBLE_WIDTH <= bounds.getMaxX() &&
                    bounds.getMinY() <= stageY + BORDER_SIZE && // We want the title bar to always be visible
                    stageY + MINIMUM_VISIBLE_HEIGHT <= bounds.getMaxY()) {
                return false;
            }
        }
        return true;
    }

    protected void moveToPrimaryScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + MARGIN);
        stage.setY(bounds.getMinY() + MARGIN);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(mHeight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (mHidden ? 1231 : 1237);
        result = prime * result + (mMaximized ? 1231 : 1237);
        temp = Double.doubleToLongBits(mWidth);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mX);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mY);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StageState other = (StageState) obj;
        if (Double.doubleToLongBits(mHeight) != Double.doubleToLongBits(other.mHeight))
            return false;
        if (mHidden != other.mHidden)
            return false;
        if (mMaximized != other.mMaximized)
            return false;
        if (Double.doubleToLongBits(mWidth) != Double.doubleToLongBits(other.mWidth))
            return false;
        if (Double.doubleToLongBits(mX) != Double.doubleToLongBits(other.mX))
            return false;
        if (Double.doubleToLongBits(mY) != Double.doubleToLongBits(other.mY))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("StageState [X = %s, Y = %s, Width = %s, Height = %s, Maximized = %s, Hidden = %s]",
            mX, mY, mWidth, mHeight, mMaximized, mHidden);
    }
}