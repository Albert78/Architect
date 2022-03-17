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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.wallmodel.IWall;
import de.dh.cad.architect.ui.objects.WallAlignmentConstructionAncillary;
import de.dh.cad.architect.ui.objects.WallAlignmentConstructionAncillary.WallAlignment;
import de.dh.cad.architect.ui.objects.WallAlignmentConstructionAncillary.WallAlignmentData;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.utils.Context;

/**
 * Manages the visual feedbacks for a given wall of interest.
 */
public class WallAlignmentsFeedback {
    protected final ConstructionView mParentView;
    protected final Map<WallAlignment, WallAlignmentConstructionAncillary> mWallAlignmentFeedbacks = new TreeMap<>();
    protected final IWall mWall;
    protected Position2D mPosA;
    protected Position2D mPosB;

    public WallAlignmentsFeedback(ConstructionView parentView, IWall wall) {
        mWall = wall;
        mParentView = parentView;
    }

    public void update(Set<WallAlignment> wallAlignments) {
        Position2D posA = mWall.getAnchorWallHandleA().getPosition();
        Position2D posB = mWall.getAnchorWallHandleB().getPosition();

        Context<Boolean> changed = Context.of(!Objects.equals(mPosA, posA) || !Objects.equals(mPosB, posB));
        mPosA = posA;
        mPosB = posB;

        for (WallAlignment formerAlignment : new ArrayList<>(mWallAlignmentFeedbacks.keySet())) {
            if (!wallAlignments.contains(formerAlignment)) {
                mWallAlignmentFeedbacks.remove(formerAlignment).removeFromView();
                changed.set(true);
            }
        }

        List<WallAlignmentData> alignments = new ArrayList<>();
        for (WallAlignment wallAlignment : wallAlignments) {
            WallAlignmentConstructionAncillary feedback = mWallAlignmentFeedbacks.computeIfAbsent(wallAlignment, wa -> {
                WallAlignmentConstructionAncillary result = new WallAlignmentConstructionAncillary(mParentView);
                mParentView.addAncillaryObject(result);
                changed.set(true);
                return result;
            });
            alignments.add(new WallAlignmentData(feedback, wallAlignment));
        }

        if (changed.get()) {
            WallAlignmentConstructionAncillary.arrange(mWall, alignments);
        }
    }

    public void install() {
        // Nothing to do
    }

    public void uninstall() {
        for (WallAlignmentConstructionAncillary feedback : new ArrayList<>(mWallAlignmentFeedbacks.values())) {
            feedback.removeFromView();
        }
        mWallAlignmentFeedbacks.clear();
    }
}
