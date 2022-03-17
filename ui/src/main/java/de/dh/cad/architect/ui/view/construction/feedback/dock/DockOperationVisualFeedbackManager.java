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
package de.dh.cad.architect.ui.view.construction.feedback.dock;

import de.dh.cad.architect.ui.objects.PositionMarkerAncillary;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.AncillaryPosition;

public class DockOperationVisualFeedbackManager {
    protected final ConstructionView mView;
    protected PositionMarkerAncillary mSourceAnchorMarker = null;
    protected PositionMarkerAncillary mTargetAnchorMarker = null;

    public DockOperationVisualFeedbackManager(ConstructionView view) {
        mView = view;
    }

    public void updateVisualObjects(AncillaryPosition sourcePosition, AncillaryPosition targetPosition) {
        if (sourcePosition == null) {
            if (mSourceAnchorMarker != null) {
                mView.removeAncillaryObject(mSourceAnchorMarker.getAncillaryObjectId());
                mSourceAnchorMarker = null;
            }
        } else {
            if (mSourceAnchorMarker == null) {
                mSourceAnchorMarker = new PositionMarkerAncillary(mView);
                mView.addAncillaryObject(mSourceAnchorMarker);
            }
            mSourceAnchorMarker.update(sourcePosition);
        }
        if (sourcePosition == null) {
            if (mTargetAnchorMarker != null) {
                mView.removeAncillaryObject(mTargetAnchorMarker.getAncillaryObjectId());
                mTargetAnchorMarker = null;
            }
        } else {
            if (mTargetAnchorMarker == null) {
                mTargetAnchorMarker = new PositionMarkerAncillary(mView);
                mView.addAncillaryObject(mTargetAnchorMarker);
            }
            mTargetAnchorMarker.update(targetPosition);
        }
    }

    public void removeVisualObjects() {
        updateVisualObjects(null, null);
    }
}
