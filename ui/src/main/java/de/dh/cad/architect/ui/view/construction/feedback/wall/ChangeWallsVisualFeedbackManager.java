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
package de.dh.cad.architect.ui.view.construction.feedback.wall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.wallmodel.AdaptedModelAnchor;
import de.dh.cad.architect.model.wallmodel.AdaptedModelWall;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.ui.objects.WallAlignmentConstructionAncillary.WallAlignment;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.WallsAlignmentModel.BendPointAngle;
import de.dh.cad.architect.ui.view.construction.feedback.wall.WallsAlignmentModel.POWallState;
import de.dh.cad.architect.ui.view.construction.feedback.wall.WallsAlignmentModel.WallStates;
import de.dh.cad.architect.utils.SortedPair;

public class ChangeWallsVisualFeedbackManager {
    protected final ConstructionView mView;
    protected final WallsFeedback mWallsFeedback;
    protected final Map<IWall, WallDimensioningsFeedback> mWallDimensioningsFeedbacks;
    protected final Map<IWall, WallAlignmentsFeedback> mWallAlignmentsFeedback;
    protected final WallAnglesFeedback mWallAnglesFeedback;
    protected final RelatedPositionsFeedback mRelatedPositionsFeedback;

    protected Map<String, ? extends IWall> mVirtualWalls = null;
    protected Collection<? extends IWall> mWallsForDimensioningsFeedback = null;
    protected Collection<? extends IWall> mWallsForAlignmentsFeedback = null;
    protected Optional<? extends IWallAnchor> mWallAnchorOfMovingHandle = Optional.empty();

    protected WallsSnappingModel mSnappingModel = null;
    protected WallsAlignmentModel mAlignmentModel = null;
    protected RelatedPositionsModel mRelatedPositionsModel = null;

    public ChangeWallsVisualFeedbackManager(ConstructionView view) {
        mView = view;
        mWallsFeedback = new WallsFeedback(view);
        mWallDimensioningsFeedbacks = new HashMap<>();
        mWallAlignmentsFeedback = new HashMap<>();
        mWallAnglesFeedback = new WallAnglesFeedback(view);
        mRelatedPositionsFeedback = new RelatedPositionsFeedback(view);
    }

    public void initialize(Map<String, ? extends IWall> virtualWalls,
        Collection<? extends IWall> wallsForDimensioningsFeedback, Collection<? extends IWall> wallsForAlignmentsFeedback, Optional<? extends IWallAnchor> oWallAnchorOfMovingHandle) {
        mVirtualWalls = virtualWalls;
        mWallsForDimensioningsFeedback = wallsForDimensioningsFeedback;
        mWallsForAlignmentsFeedback = wallsForAlignmentsFeedback;
        mWallAnchorOfMovingHandle = oWallAnchorOfMovingHandle;

        removeVisualObjects();
        Plan plan = mView.getPlan();
        Collection<? extends IWallAnchor> wallAnchorsOfMovingHandles;
        if (oWallAnchorOfMovingHandle.isPresent()) {
            wallAnchorsOfMovingHandles = Collections.singleton(oWallAnchorOfMovingHandle.get());
        } else {
            wallAnchorsOfMovingHandles = Collections.emptySet();
        }
        Collection<IWall> modelWalls = AdaptedModelWall.wrapWalls(plan.getWalls().values());
        Collection<IWall> modelAndVirtualWalls = CollectionUtils.union(modelWalls, virtualWalls.values());
        mSnappingModel = WallsSnappingModel.create(oWallAnchorOfMovingHandle, modelAndVirtualWalls, plan.getGuideLines().values());
        Collection<VirtualLinesCenter> wallSnapData = mSnappingModel.getWallSnapData();
        mAlignmentModel = WallsAlignmentModel.create(mWallsForAlignmentsFeedback, modelWalls, wallSnapData);
        // Create related positions model even if we don't have anchors of interest - in that case, the model is empty
        mRelatedPositionsModel = new RelatedPositionsModel(wallAnchorsOfMovingHandles, wallSnapData);

        for (IWall wall : mWallsForDimensioningsFeedback) {
            WallDimensioningsFeedback dimensioningsFeedback = new WallDimensioningsFeedback(mView, wall);
            dimensioningsFeedback.install();
            mWallDimensioningsFeedbacks.put(wall, dimensioningsFeedback);
        }
        updateVisualObjects();
    }

    public void initializeForAddWall(PrincipalWallAncillaryWallsModel ancillaryWallsModel) {
        initialize(ancillaryWallsModel.getWallsById(),
            ancillaryWallsModel.getWallsForDimensioningsFeedback(), ancillaryWallsModel.getWallsForAlignentsFeedback(),
            ancillaryWallsModel.getWallAnchorForMovingHandleFeedback());
    }

    public void initializeForDragWallHandle(Anchor wallHandleAnchor) {
        IWallAnchor wrappedHandleAnchor = new AdaptedModelAnchor(wallHandleAnchor);
        List<IWall> dockedWalls = wrappedHandleAnchor
            .getAllDockedAnchors()
            .stream()
            .map(a -> a.getOwner())
            .toList();
        initialize(Collections.emptyMap(), dockedWalls, dockedWalls, Optional.of(wrappedHandleAnchor));
    }

    public void uninstall() {
        removeVisualObjects();
    }

    public Optional<Position2D> correctDragPosition(Position2D targetPosition) {
        return mSnappingModel.snapAnchorPosition(targetPosition, mView.getScale());
    }

    public void updateVisualObjects() {
        // Ghost walls
        mWallsFeedback.update(mVirtualWalls);

        mAlignmentModel.updateWallStates();
        Map<String, WallStates> wallStatesMap = mAlignmentModel.getWallStates();

        for (IWall wall : mWallsForDimensioningsFeedback) {
            WallDimensioningsFeedback feedbacks = mWallDimensioningsFeedbacks.get(wall);
            feedbacks.update();
        }

        Map<SortedPair<IWallAnchor>, BendPointAngle> neighbourWallAngles = new TreeMap<>();
        Map<IWall, Set<WallAlignment>> alignmentsByWall = new TreeMap<>();

        for (IWall wall : mWallsForAlignmentsFeedback) {
            String wallId = wall.getId();
            WallStates wallStates = wallStatesMap.get(wallId);
            if (wallStates == null) {
                continue;
            }
            // Collect all neighbour angles (pairs of neighbour angles might occur multiple times if
            // the alignment model was calculated over docked walls, putting them into a map will remove duplicates)
            neighbourWallAngles.putAll(new TreeMap<>(wallStates.getNeighbourWallAngles()));

            // Cluster all wall alignment symbols by wall
            Optional<WallAlignment> oThisWallHVAlignment = WallAlignment.computeHVWallAlignment(wallStates.isHorizontal(), wallStates.isVertical());
            oThisWallHVAlignment.ifPresent(wallHVAlignment -> {
                alignmentsByWall.computeIfAbsent(wall, w -> new TreeSet<>())
                    .add(wallHVAlignment);
            });
            for (POWallState wallState : wallStates.getPOWallStates().values()) {
                Optional<WallAlignment> oOtherWallPOAlignment = WallAlignment.computePOWallAlignment(wallState.isParallel(), wallState.isOrthogonal());
                oOtherWallPOAlignment.ifPresent(wallPOAlignment -> {
                    alignmentsByWall.computeIfAbsent(wallState.getOtherWall(), otherWall -> new TreeSet<>())
                        .add(wallPOAlignment);
                });
            }
        }
        for (IWall wall : new ArrayList<>(mWallAlignmentsFeedback.keySet())) {
            if (!alignmentsByWall.containsKey(wall)) {
                mWallAlignmentsFeedback.remove(wall).uninstall();
            }
        }
        for (Entry<IWall, Set<WallAlignment>> entry : alignmentsByWall.entrySet()) {
            IWall wall = entry.getKey();
            Set<WallAlignment> wallAlignments = entry.getValue();
            WallAlignmentsFeedback feedback = mWallAlignmentsFeedback.computeIfAbsent(wall, w -> {
                WallAlignmentsFeedback result = new WallAlignmentsFeedback(mView, wall);
                result.install();
                return result;
            });

            feedback.update(wallAlignments);
        }
        mWallAnglesFeedback.update(neighbourWallAngles);
        mRelatedPositionsModel.update();
        mRelatedPositionsFeedback.update(mRelatedPositionsModel.getRelatedPositions());
    }

    public void removeVisualObjects() {
        mWallsFeedback.uninstall();
        for (WallDimensioningsFeedback feedbacks : mWallDimensioningsFeedbacks.values()) {
            feedbacks.uninstall();
        }
        for (WallAlignmentsFeedback feedbacks : mWallAlignmentsFeedback.values()) {
            feedbacks.uninstall();
        }
        mWallAnglesFeedback.uninstall();
        mRelatedPositionsFeedback.uninstall();
    }
}
