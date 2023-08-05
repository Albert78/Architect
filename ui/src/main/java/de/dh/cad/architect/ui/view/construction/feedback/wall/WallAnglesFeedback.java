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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import de.dh.cad.architect.model.coords.Angle;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.ui.objects.WallAngleConstructionAncillary;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.WallsAlignmentModel.BendPointAngle;
import de.dh.cad.architect.utils.CircularLinkedList;
import de.dh.cad.architect.utils.CircularLinkedList.Node;
import de.dh.cad.architect.utils.SortedPair;

/**
 * Manages the visual feedbacks for all angles between adjacent walls.
 */
public class WallAnglesFeedback {
    protected static class AnchorWithAngle {
        protected final IWallAnchor mAnchor;
        protected final Angle mAngle;

        public AnchorWithAngle(IWallAnchor anchor, Angle angle) {
            mAnchor = anchor;
            mAngle = angle;
        }

        public IWallAnchor getAnchor() {
            return mAnchor;
        }

        public Angle getAngle() {
            return mAngle;
        }
    }

    protected static class AngleResult {
        protected final Position2D mPosition;
        protected final Angle mAngle1;
        protected final Angle mAngle2;
        protected final boolean mTurnToSmallerAngleSide;

        public AngleResult(Position2D position, Angle angle1, Angle angle2, boolean turnToSmallerAngleSide) {
            mPosition = position;
            mAngle1 = angle1;
            mAngle2 = angle2;
            mTurnToSmallerAngleSide = turnToSmallerAngleSide;
        }

        public Position2D getPosition() {
            return mPosition;
        }

        public Angle getAngle1() {
            return mAngle1;
        }

        public Angle getAngle2() {
            return mAngle2;
        }

        public boolean isTurnToSmallerAngleSide() {
            return mTurnToSmallerAngleSide;
        }
    }

    protected final ConstructionView mParentView;
    protected final Map<Pair<IWallAnchor, IWallAnchor>, WallAngleConstructionAncillary> mNeighbourWallFeedbacks = new TreeMap<>();

    public WallAnglesFeedback(ConstructionView parentView) {
        mParentView = parentView;
    }

    public void update(Map<SortedPair<IWallAnchor>, BendPointAngle> neighborWallAngles) {
        // Avoid drawing angle visuals on top of other angle visuals:

        // Cluster by bendpoint anchor dock root
        Map<IWallAnchor, List<BendPointAngle>> bendPointsByDock = new TreeMap<>();
        for (BendPointAngle bendPointAngle : neighborWallAngles.values()) {
            IWallAnchor anchor = bendPointAngle.getThisAnchor();
            Iterator<? extends IWallAnchor> i = anchor.getAllDockedAnchors().iterator();
            IWallAnchor dockMaster = i.hasNext() ? i.next() : anchor;
            bendPointsByDock
                .computeIfAbsent(dockMaster, a -> new ArrayList<>())
                .add(bendPointAngle);
        }

        // Thin out and align angles of this dock; retain only those which directly connect neighbor walls.
        // We do this by arranging all direct neighbor anchors in a circular list, then only using combinations
        // of neighbor anchors from that list as remaining angles.
        Map<Pair<IWallAnchor, IWallAnchor>, AngleResult> remainingAngles = new TreeMap<>();
        for (List<BendPointAngle> bendPointAngles : bendPointsByDock.values()) {
            Angle refAngle = bendPointAngles.get(0).getThisWallAngle(); // Take arbitrary angle as reference
            CircularLinkedList<IWallAnchor> neighborAnchors = new CircularLinkedList<>();
            bendPointAngles
                .stream()
                .flatMap(bpa -> Stream.of(
                    new AnchorWithAngle(bpa.getThisAnchor(), bpa.getThisWallAngle()),
                    new AnchorWithAngle(bpa.getConnectedAnchor(), bpa.getNeighbourAngle())))
                .sorted(Comparator.comparing(awa -> Angle.normalizeAngle(awa.getAngle().getAngleDeg() - refAngle.getAngleDeg())))
                .map(awa -> awa.getAnchor())
                .distinct()
                .forEach(neighborAnchors::addNode);
            for (BendPointAngle bpa : bendPointAngles) {
                IWallAnchor thisAnchor = bpa.getThisAnchor();
                Node<IWallAnchor> anchorNode = neighborAnchors.findNode(thisAnchor).get(); // Must find a result, we just added all anchors above
                IWallAnchor anchorBefore = anchorNode.getPrevNode().getValue();
                IWallAnchor anchorAfter = anchorNode.getNextNode().getValue();
                IWallAnchor connectedAnchor = bpa.getConnectedAnchor();

                // Align angle visuals to the side between the adjacent walls (if more than one angle is to be drawn)
                if (connectedAnchor.equals(anchorAfter)) {
                    // Correct order
                    remainingAngles.put(new ImmutablePair<>(thisAnchor, connectedAnchor),
                        new AngleResult(bpa.getThisAnchor().getPosition(), bpa.getThisWallAngle(), bpa.getNeighbourAngle(), bendPointAngles.size() == 1));
                } else if (connectedAnchor.equals(anchorBefore)) {
                    // Turn anchor direction
                    remainingAngles.put(new ImmutablePair<>(connectedAnchor, thisAnchor),
                        new AngleResult(bpa.getThisAnchor().getPosition(), bpa.getNeighbourAngle(), bpa.getThisWallAngle(), bendPointAngles.size() == 1));
                } // Else: Anchor pair is not adjacent, ignore it
            }
        }

        for (Entry<Pair<IWallAnchor, IWallAnchor>, AngleResult> entry : remainingAngles.entrySet()) {
            Pair<IWallAnchor, IWallAnchor> anchors = entry.getKey();
            AngleResult angles = entry.getValue();
            WallAngleConstructionAncillary feedback = mNeighbourWallFeedbacks.computeIfAbsent(anchors, key -> {
                WallAngleConstructionAncillary result = new WallAngleConstructionAncillary(mParentView);
                mParentView.addAncillaryObject(result);
                return result;
            });
            feedback.update(
                angles.getPosition(),
                angles.getAngle1(), angles.getAngle2(), angles.isTurnToSmallerAngleSide());
        }
        for (Pair<IWallAnchor, IWallAnchor> bendPoint : new ArrayList<>(mNeighbourWallFeedbacks.keySet())) {
            if (!remainingAngles.containsKey(bendPoint)) {
                mNeighbourWallFeedbacks.remove(bendPoint).removeFromView();
            }
        }
    }

    public void install() {
        // Nothing to do
    }

    public void uninstall() {
        for (Pair<IWallAnchor, IWallAnchor> bendPoint : new ArrayList<>(mNeighbourWallFeedbacks.keySet())) {
            mNeighbourWallFeedbacks.remove(bendPoint).removeFromView();
        }
    }
}
