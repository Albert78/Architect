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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import de.dh.cad.architect.ui.objects.ReferenceAngleConstructionAncillary;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.RelatedPositionsModel.RelatedPosition;

/**
 * Manages and draws the visual feedbacks for a set of {@link RelatedPosition related positions}.
 */
public class RelatedPositionsFeedback {
    protected final ConstructionView mParentView;
    protected final Map<String, ReferenceAngleConstructionAncillary> mRelatedPositionFeedbacks = new TreeMap<>();

    public RelatedPositionsFeedback(ConstructionView parentView) {
        mParentView = parentView;
    }

    public void update(Map<String, RelatedPosition> relatedPositions) {
        for (String positionId : new ArrayList<>(mRelatedPositionFeedbacks.keySet())) {
            if (!relatedPositions.containsKey(positionId)) {
                mRelatedPositionFeedbacks.remove(positionId).removeFromView();
            }
        }
        for (Entry<String, RelatedPosition> entry : relatedPositions.entrySet()) {
            String positionId = entry.getKey();
            RelatedPosition relatedPosition = entry.getValue();
            ReferenceAngleConstructionAncillary feedback = mRelatedPositionFeedbacks.computeIfAbsent(positionId, id -> {
                ReferenceAngleConstructionAncillary result = new ReferenceAngleConstructionAncillary(mParentView);
                mParentView.addAncillaryObject(result);
                return result;
            });
            feedback.update(relatedPosition.getPosition(), relatedPosition.getAngle());
        }
    }

    public void install() {
        // Nothing to do: Number of related positions is dynamic
    }

    public void uninstall() {
        for (String positionId : new ArrayList<>(mRelatedPositionFeedbacks.keySet())) {
            mRelatedPositionFeedbacks.get(positionId).removeFromView();
        }
    }
}
