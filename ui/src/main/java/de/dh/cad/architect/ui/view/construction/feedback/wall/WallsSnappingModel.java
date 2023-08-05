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
package de.dh.cad.architect.ui.view.construction.feedback.wall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.MathUtils;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.GuideLine.GuideLineDirection;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.construction.feedback.wall.VirtualLinesCenter.Distance;

/**
 * Calculates snapping positions for wall change feedback.
 * Contains the static snapping positions and all data to calculate dynamic snapping positions for a wall anchor in a given wall
 * dock situation based on virtual {@link IWall walls} and {@link IWallAnchor anchors} and calculates snapping positions.
 *
 * When adding a wall or dragging an existing wall anchor, the user expects "haptic" feedback which guides him to likely wall end positions
 * to locate walls orthogonal to the coordinate axes or orthogonal to docked walls.
 * Furthermore, we provide snapping to the view's guide lines.
 */
public class WallsSnappingModel {
    protected static class SnapLine {
        protected final Position2D mA;
        protected final Vector2D mV;

        public SnapLine(Position2D a, Vector2D v) {
            mA = a;
            mV = v;
        }

        public Position2D getA() {
            return mA;
        }

        public Vector2D getV() {
            return mV;
        }

        @Override
        public String toString() {
            return "[" + mA.coordsToString() + "] + n * [" + mV.coordsToString() + "]";
        }

        public static Collection<SnapLine> fromWallSnapData(Collection<VirtualLinesCenter> wallSnapData) {
            Collection<SnapLine> result = new ArrayList<>();
            for (VirtualLinesCenter wsd : wallSnapData) {
                for (Angle snapAngle : wsd.getAngles()) {
                    result.add(new SnapLine(wsd.getCenterPosition(), Vector2D.ofAngle(snapAngle)));
                }
            }
            return result;
        }

        public static Collection<SnapLine> fromGuideLines(Collection<GuideLine> guideLines) {
            Collection<SnapLine> result = new ArrayList<>();
            for (GuideLine guideLine : guideLines) {
                if (guideLine.getDirection() == GuideLineDirection.Horizontal) {
                    result.add(new SnapLine(new Position2D(Length.ZERO, guideLine.getPosition()), new Vector2D(Length.ofM(1), Length.ZERO)));
                } else {
                    result.add(new SnapLine(new Position2D(guideLine.getPosition(), Length.ZERO), new Vector2D(Length.ZERO, Length.ofM(1))));
                }
            }
            return result;
        }
    }

   /**
     * Maximum, scale-corrected distance to snap to a snap point.
     */
    protected static final double MAX_SNAP_POSITION_DIFF = 20;

    /**
     * Snap points of a snaller distance won't be treated as separate points.
     */
    protected static final Length MAX_POINT_DISTANCE_FOR_EQUAL = Length.ofMM(1);

    protected static final double EPSILON = 0.01;

    protected final Collection<VirtualLinesCenter> mWallSnapData;
    protected final Collection<GuideLine> mGuideLines;
    protected final Collection<Position2D> mStaticSnapPositions;

    public WallsSnappingModel(Collection<VirtualLinesCenter> wallSnapData, Collection<GuideLine> guideLines, Collection<Position2D> staticSnapPositions) {
        mWallSnapData = wallSnapData;
        mGuideLines = guideLines;
        mStaticSnapPositions = staticSnapPositions;
    }

    public static WallsSnappingModel create(Optional<? extends IWallAnchor> oSnapWallHandle,
            Collection<IWall> allWalls, Collection<GuideLine> guideLines) {
        Collection<SnapLine> snapLines = new ArrayList<>();
        Map<String, VirtualLinesCenter> wallSnapData = new TreeMap<>();
        Set<IWallAnchor> allWallAnchors = allWalls
            .stream()
            .flatMap(wall -> Stream.of(wall.getAnchorWallHandleA(), wall.getAnchorWallHandleB()))
            .flatMap(anchor -> anchor.getAllDockedAnchors().stream())
            .collect(Collectors.toSet());

        Collection<IWall> snapHandleDockedWalls = new TreeSet<>();
        List<IWallAnchor> oppositeHandles = new ArrayList<>();
        Collection<? extends IWallAnchor> snapHandleDockedAnchors = oSnapWallHandle
                .map(snapWallHandle -> new TreeSet<>(snapWallHandle.getAllDockedAnchors()))
                .orElse(new TreeSet<>());
        for (IWallAnchor dockedAnchor : snapHandleDockedAnchors) {
            IWall dockedWall = dockedAnchor.getOwner();
            snapHandleDockedWalls.add(dockedWall);
            if (dockedWall.representsRealWall()) { // Omit virtual add-wall-corner-walls...
                oppositeHandles.add(getOppositeHandleAnchor(dockedAnchor));
            }
        }
        for (IWallAnchor anchor : allWallAnchors) {
            if (!anchor.isReferenceAnchor()) {
                // Don't snap to uninteresting anchors: Virtual walls, ... (sometimes anchors are explicitly configured as reference points, for example a new wall's start anchor)
                continue;
            }

            Collection<Angle> snapAngles = new ArrayList<>();

            if (anchor.getOwner().representsRealWall()) {
                snapAngles.addAll(calculateSnapAnglesFromWallSide(anchor, true));
            }
            if (snapHandleDockedAnchors.contains(anchor)) {
                // Don't snap our handle to itself or another handle in the same dock
                continue;
            }

            // Horizontal
            snapAngles.add(new Angle(0.0));
            // Vertical
            snapAngles.add(new Angle(90.0));

            Position2D rotationCenter = anchor.getPosition();
            String anchorId = anchor.getId();
            VirtualLinesCenter wsd = new VirtualLinesCenter(anchorId, rotationCenter);
            wsd.getAngles().addAll(snapAngles);
            wallSnapData.put(anchorId, wsd);
        }

        // Calculate wall snap data angles for direct connections of moved walls itself -
        // if the snap wall handle is a dock of two (or more) walls, build snap lines on the direct connections
        // of the opposite side handles
        int numOppositeHandles = oppositeHandles.size();
        for (int i = 0; i < numOppositeHandles; i++) {
            for (int j = i + 1; j < numOppositeHandles; j++) {
                IWallAnchor a = oppositeHandles.get(i);
                IWallAnchor b = oppositeHandles.get(j);
                String aId = a.getId();
                VirtualLinesCenter wsd = wallSnapData.computeIfAbsent(aId, id -> new VirtualLinesCenter(aId, a.getPosition()));
                wsd.getAngles().add(Angle.ofVector(b.getPosition().minus(a.getPosition())));

                String bId = b.getId();
                wsd = wallSnapData.computeIfAbsent(bId, id -> new VirtualLinesCenter(bId, b.getPosition()));
                wsd.getAngles().add(Angle.ofVector(a.getPosition().minus(b.getPosition())));
            }
        }

        if (numOppositeHandles == 1) {
            IWallAnchor oppositeHandle = oppositeHandles.get(0);
            String oppositeHandleId = oppositeHandle.getId();
            if (!wallSnapData.containsKey(oppositeHandleId)) {
                // This is the situation where the start wall anchor (oppositeHandle) is not docked
                // and thus there is no wall snap data for the verical and horizontal snap line starting at the
                // oppositeHandle -> create it
                VirtualLinesCenter wsd = new VirtualLinesCenter(oppositeHandleId, oppositeHandle.getPosition());
                Collection<Angle> angles = wsd.getAngles();
                angles.add(new Angle(0.0));
                angles.add(new Angle(90.0));
                wallSnapData.put(oppositeHandleId, wsd);
            }
        }

        snapLines.addAll(SnapLine.fromGuideLines(guideLines));
        snapLines.addAll(SnapLine.fromWallSnapData(wallSnapData.values()));

        return new WallsSnappingModel(wallSnapData.values(), guideLines, calculateCrossingPoints(snapLines));
    }

    /**
     * Calculates the best snapping position for the given {@code startPos} based on this snapping data for the given view's scale.
     * @param startPos Current wall handle anchor position which should maybe snapped, if there is a good snapping position.
     * @param scale The current view's scale to make the maximum snapping distance equal in physical view coordinates.
     */
    public Optional<Position2D> snapAnchorPosition(Position2D startPos, double scale) {
        // Snap priority 1: Prefer static snap positions
        double maxSnapDiff = MAX_SNAP_POSITION_DIFF / scale;
        Optional<Position2D> oNearestSnapPosition = findNearestPoint(startPos, mStaticSnapPositions, maxSnapDiff);
        if (oNearestSnapPosition.isPresent()) {
            return oNearestSnapPosition;
        }

        // Snap priority 2: Fallback to dynamic snap positions along snap lines
        Collection<Position2D> snapPositions = new ArrayList<>();

        // TODO: Snap to positions where each pair of the walls docked to the snap wall handle is orthogonal.
        // Those positions are located on an arc going from each opposite handle to another.
        // Snapping to those arcs should be separated into two priorities:
        // - First need to snap to each intersection point of the arcs and all other wall snap lines (goes to snap priority 2a)
        // - Then we need to snap to the arc lines itself (goes to snap priority 2b)

        // Snap to wall angle lines
        snapPositions.addAll(calculateSnapPositionsFromWalls(startPos));

        // Snap to guidelines
        snapPositions.addAll(calculateSnapPositionsFromGuideLines(startPos));

        oNearestSnapPosition = findNearestPoint(startPos, snapPositions, maxSnapDiff);
        if (oNearestSnapPosition.isPresent()) {
            return oNearestSnapPosition;
        }
        return Optional.empty();
    }

    public Collection<VirtualLinesCenter> getWallSnapData() {
        return mWallSnapData;
    }

    public Collection<GuideLine> getGuideLines() {
        return mGuideLines;
    }

    public Collection<Position2D> getStaticSnapPositions() {
        return mStaticSnapPositions;
    }

    protected Collection<Position2D> calculateSnapPositionsFromGuideLines(Position2D currentPoint) {
        Collection<Position2D> result = new ArrayList<>();
        for (GuideLine guideLine : mGuideLines) {
            switch (guideLine.getDirection()) {
            case Vertical:
                result.add(currentPoint.withX(guideLine.getPosition()));
                break;
            case Horizontal:
                result.add(currentPoint.withY(guideLine.getPosition()));
                break;
            default:
                throw new RuntimeException("Unknown guideline direction " + guideLine.getDirection());
            }
        }
        return result;
    }

    protected Position2D calculateSnapPositionForAngle(Position2D centerPosition, Angle snapAngle,
        Position2D startPos) {
        Vector2D currentVector = Vector2D.between(centerPosition, startPos);
        Length length = currentVector.getLength();
        double angleRad = snapAngle.getAngleRad();
        return new Position2D(
            centerPosition.getX().plus(length.times(Math.cos(angleRad))),
            centerPosition.getY().plus(length.times(Math.sin(angleRad))));
    }

    protected Collection<Position2D> calculateSnapPositionsFromWalls(Position2D startPos) {
        Collection<Position2D> result = new ArrayList<>();
        for (VirtualLinesCenter wsd : mWallSnapData) {
            Distance distance = wsd.calculateSmallestLineDistance(startPos);
            result.add(calculateSnapPositionForAngle(wsd.getCenterPosition(), distance.getAngleToDistance(), startPos));
        }
        return result;
    }

    /**
     * Gets likely angles for a wall starting at the given anchor based on the current dock situation.
     * This contains all docked wall's original angles together with all angles in a rotation of 90, 180 and 270 degrees.
     */
    protected static Collection<Angle> calculateSnapAnglesFromWallSide(IWallAnchor anchor, boolean excludeVirtual) {
        Collection<Angle> result = new ArrayList<>();
        IWall dockedWall = anchor.getOwner();
        Vector2D ab = Vector2D.between(dockedWall.getAnchorWallHandleA().getPosition(), dockedWall.getAnchorWallHandleB().getPosition());
        Angle angle = Angle.ofVector(ab);
        result.add(angle);
        result.add(angle.plusDeg(90));
        return result;
    }

    protected static IWallAnchor getOppositeHandleAnchor(IWallAnchor handle) {
        IWall ownerWall = handle.getOwner();
        return ownerWall.getAnchorWallHandle(handle.getHandleAnchorDockEnd().get().opposite());
    }

    protected static Optional<Position2D> findNearestPoint(Position2D startPos, Collection<Position2D> positions, double maxDistance) {
        double nearestDistance = Double.MAX_VALUE;
        Position2D nearestPos = null;
        for (Position2D position : positions) {
            double distance = CoordinateUtils.lengthToCoords(position.distance(startPos));
            if (distance < Math.min(nearestDistance, maxDistance)) {
                nearestDistance = distance;
                nearestPos = position;
            }
        }
        return Optional.ofNullable(nearestPos);
    }

    protected static boolean containsSimilarPoint(Collection<Position2D> collection, Position2D point) {
        for (Position2D pos : collection) {
            if (MathUtils.isAlmostEqual(pos, point, MAX_POINT_DISTANCE_FOR_EQUAL)) {
                return true;
            }
        }
        return false;
    }

    protected static Collection<Position2D> calculateCrossingPoints(Collection<SnapLine> lines) {
        List<SnapLine> lLines = lines instanceof List ? (List<SnapLine>) lines : new ArrayList<>(lines);
        Collection<Position2D> result = new ArrayList<>(lLines.size() * lLines.size());
        for (int i = 0; i < lLines.size(); i++) {
            SnapLine line1 = lLines.get(i);
            for (int j = i + 1; j < lLines.size(); j++) {
                SnapLine line2 = lLines.get(j);
                if (line1.getV().isParallelTo(line2.getV(), EPSILON)) {
                    continue;
                }
                Optional<Position2D> oCrossingPoint = MathUtils.calculateLinesIntersectionPoint(
                    line1.getA(), line1.getA().plus(line1.getV()), line2.getA(), line2.getA().plus(line2.getV()));
                oCrossingPoint.ifPresent(crossingPoint -> {
                    if (!containsSimilarPoint(result, crossingPoint)) {
                        result.add(crossingPoint);
                    }
                });
            }
        }
        return result;
    }
}
