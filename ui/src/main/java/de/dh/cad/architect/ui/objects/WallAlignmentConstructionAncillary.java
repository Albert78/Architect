/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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
package de.dh.cad.architect.ui.objects;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.utils.MathUtils;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.utils.fx.Vector2D;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;

public class WallAlignmentConstructionAncillary extends Abstract2DAncillaryObject {
    public static enum WallAlignment {
        Horizontal,
        Vertical,
        Parallel,
        Orthogonal;

        public static Optional<WallAlignment> computeHVWallAlignment(boolean horizontal, boolean vertical) {
            if (horizontal) {
                return Optional.of(WallAlignment.Horizontal);
            } else if (vertical) {
                return Optional.of(WallAlignment.Vertical);
            }
            return Optional.empty();
        }

        public static Optional<WallAlignment> computePOWallAlignment(boolean parallel, boolean orthogonal) {
            if (parallel) {
                return Optional.of(WallAlignment.Parallel);
            } else if (orthogonal) {
                return Optional.of(WallAlignment.Orthogonal);
            }
            return Optional.empty();
        }
    }

    public static class WallAlignmentData {
        protected final WallAlignmentConstructionAncillary mAncillaryObject;
        protected final WallAlignment mAlignment;

        public WallAlignmentData(WallAlignmentConstructionAncillary ancillaryObject, WallAlignment alignment) {
            mAncillaryObject = ancillaryObject;
            mAlignment = alignment;
        }

        public WallAlignmentConstructionAncillary getAncillaryObject() {
            return mAncillaryObject;
        }

        public WallAlignment getAlignment() {
            return mAlignment;
        }
    }

    protected static class WallData {
        protected final Length mWallThickness;
        protected final Position2D mHandleA;
        protected final Position2D mHandleB;

        public WallData(Length wallThickness, Position2D handleA, Position2D handleB) {
            mWallThickness = wallThickness;
            mHandleA = handleA;
            mHandleB = handleB;
        }

        public static WallData fromWall(IWall wall) {
            Position2D handleA = wall.getAnchorWallHandleA().getPosition();
            Position2D handleB = wall.getAnchorWallHandleB().getPosition();
            Length thickness = wall.getThickness();
            return new WallData(thickness, handleA, handleB);
        }

        public Length getWallThickness() {
            return mWallThickness;
        }

        public Position2D getHandleA() {
            return mHandleA;
        }

        public Position2D getHandleB() {
            return mHandleB;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mWallThickness, mHandleA, mHandleB);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            WallData other = (WallData) obj;
            if (!mWallThickness.equals(other.mWallThickness))
                return false;
            if (!mHandleA.equals(other.mHandleA))
                return false;
            if (!mHandleB.equals(other.mHandleB))
                return false;
            return true;
        }
    }

    protected static final double WALL_ALIGNMENT_FEEDBACK_DIST = 6;
    protected static final double WALL_ALIGNMENT_FEEDBACK_SIZE = 25;
    protected static final double DISTANCE = 10;

    protected WallAlignment mAlignment = null;
    protected WallData mWallData = null;
    protected double mOffset = 0;

    protected Node mVisual = null;
    protected Scale mVisualsScaleCorrection = null;

    public WallAlignmentConstructionAncillary(ConstructionView parentView) {
        super(parentView);
        setMouseTransparent(true);
    }

    public WallAlignment getAlignment() {
        return mAlignment;
    }

    protected void remove() {
        if (mVisual == null) {
            return;
        }
        remove(mVisual);
        mVisual = null;
    }

    @Override
    public void updateScale(double scaleCompensation) {
        remove(); // Optimization, avoid updating scale when visual is removed anyway by updateAlignment()
        super.updateScale(scaleCompensation);
        updateAlignment();
    }

    public void updateAlignment(WallAlignment alignment, IWall wall, double offset) {
        WallData wallData = WallData.fromWall(wall);
        if (Objects.equals(mAlignment, alignment) && Objects.equals(mWallData, wallData) && MathUtils.almostEqual(mOffset, offset, MathUtils.EPSILON_DOUBLE_EQUALITY)) {
            return;
        }
        mAlignment = alignment;
        mWallData = wallData;
        mOffset = offset;
        updateAlignment();
    }

    protected void updateAlignment() {
        remove();
        if (mAlignment == null || mWallData == null) {
            return;
        }

        double wallThickness2 = CoordinateUtils.lengthToCoords(mWallData.getWallThickness()) / 2;
        Position2D handlePosA = mWallData.getHandleA();
        Vector2D handleA = new Vector2D(CoordinateUtils.lengthToCoords(handlePosA.getX()), CoordinateUtils.lengthToCoords(handlePosA.getY()));
        Position2D handlePosB = mWallData.getHandleB();
        Vector2D handleB = new Vector2D(CoordinateUtils.lengthToCoords(handlePosB.getX()), CoordinateUtils.lengthToCoords(handlePosB.getY()));
        Vector2D middle = Vector2D.center(handleA, handleB);
        Vector2D vectorBA = handleA.minus(handleB);
        Vector2D vSide = vectorBA.getNormalCCW();
        Vector2D vOffset = vectorBA.scaleToLength(mOffset * getScaleCompensation());

        double scaleCompensation = getScaleCompensation();

        double dist = // Starting at middle of wall...
                        wallThickness2 // ... to the side
                        + WALL_ALIGNMENT_FEEDBACK_DIST * Math.sqrt(scaleCompensation) // ... 8 pixels further but compensate border stroke thickness a bit, looks good
                        + (WALL_ALIGNMENT_FEEDBACK_SIZE / 2) * scaleCompensation; // ... + half of shape width
        vSide = vSide.scaleToLength(dist);
        double shapeMiddleX = middle.getX() + vSide.getX() + vOffset.getX();
        double shapeMiddleY = middle.getY() + vSide.getY() + vOffset.getY();

        switch (mAlignment) {
        case Horizontal: {
            mVisual = createHorizontalLine(shapeMiddleX, shapeMiddleY);
            break;
        }
        case Vertical: {
            mVisual = createVerticalLine(shapeMiddleX, shapeMiddleY);
            break;
        }
        case Parallel: {
            Group g = new Group();
            mVisual = g;
            ObservableList<Node> children = g.getChildren();
            children.add(createVerticalLine(shapeMiddleX - 5, shapeMiddleY));
            children.add(createVerticalLine(shapeMiddleX + 5, shapeMiddleY));
            break;
        }
        case Orthogonal: {
            mVisual = createOrthogonalSymbol(shapeMiddleX, shapeMiddleY);
        }
        }
        mVisualsScaleCorrection = addUnscaled(mVisual);
        mVisualsScaleCorrection.setPivotX(shapeMiddleX);
        mVisualsScaleCorrection.setPivotY(shapeMiddleY);
    }

    protected static void configureShape(Shape shape) {
        shape.setStroke(ANCILLARY_FEEDBACK_OBJECTS_COLOR);
        shape.setStrokeWidth(3);
        shape.setFill(null);
    }

    protected static Node createOrthogonalSymbol(double shapeMiddleX, double shapeMiddleY) {
        Group g = new Group();
        ObservableList<Node> children = g.getChildren();

        double bottomY = shapeMiddleY + WALL_ALIGNMENT_FEEDBACK_SIZE / 2;

        Line vLine = new Line();
        vLine.setStartX(shapeMiddleX);
        vLine.setStartY(shapeMiddleY - WALL_ALIGNMENT_FEEDBACK_SIZE / 2);
        vLine.setEndX(shapeMiddleX);
        vLine.setEndY(bottomY);
        configureShape(vLine);

        Line hLine = new Line();
        hLine.setStartX(shapeMiddleX - WALL_ALIGNMENT_FEEDBACK_SIZE * 0.7 / 2);
        hLine.setStartY(bottomY);
        hLine.setEndX(shapeMiddleX + WALL_ALIGNMENT_FEEDBACK_SIZE * 0.7 / 2);
        hLine.setEndY(bottomY);
        configureShape(hLine);

        children.addAll(vLine, hLine);
        return g;
    }

    protected static Line createVerticalLine(double shapeMiddleX, double shapeMiddleY) {
        Line result = new Line();
        result.setStartX(shapeMiddleX);
        result.setStartY(shapeMiddleY - WALL_ALIGNMENT_FEEDBACK_SIZE / 2);
        result.setEndX(shapeMiddleX);
        result.setEndY(shapeMiddleY + WALL_ALIGNMENT_FEEDBACK_SIZE / 2);
        configureShape(result);
        return result;
    }

    protected static Line createHorizontalLine(double shapeMiddleX, double shapeMiddleY) {
        Line result = new Line();
        result.setStartX(shapeMiddleX - WALL_ALIGNMENT_FEEDBACK_SIZE / 2);
        result.setStartY(shapeMiddleY);
        result.setEndX(shapeMiddleX + WALL_ALIGNMENT_FEEDBACK_SIZE / 2);
        result.setEndY(shapeMiddleY);
        configureShape(result);
        return result;
    }

    /**
     * Arranges the given alignments on the given wall.
     */
    public static void arrange(IWall wall, List<WallAlignmentData> alignments) {
        double overallSize = (alignments.size() - 1) * (WALL_ALIGNMENT_FEEDBACK_SIZE + DISTANCE) + WALL_ALIGNMENT_FEEDBACK_SIZE;
        double offset = -overallSize / 2 + WALL_ALIGNMENT_FEEDBACK_SIZE / 2;
        for (WallAlignmentData alignment : alignments) {
            alignment.getAncillaryObject().updateAlignment(alignment.getAlignment(), wall, offset);
            offset += WALL_ALIGNMENT_FEEDBACK_SIZE + DISTANCE;
        }
    }
}
