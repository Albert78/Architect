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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.wallmodel.IWallAnchor;
import de.dh.cad.architect.model.wallmodel.WallAnchorPositions;
import de.dh.cad.architect.model.wallmodel.WallBevelType;
import de.dh.cad.architect.utils.IdGenerator;

/**
 * Container holding the state of temporary wall states (in form of instances of {@link AncillaryWall} and
 * {@link AncillaryWallAnchor}) to be displayed during a create or edit wall operation until the new wall state is
 * committed into the plan.
 */
public class AncillaryWallsModel {
    protected final Collection<AncillaryWall> mWallsForDimensioningsFeedback = new ArrayList<>();
    protected final Collection<AncillaryWall> mWallsForAlignentsFeedback = new ArrayList<>();
    protected Optional<AncillaryWallAnchor> mOWallAnchorForMovingHandleFeedback = Optional.empty();
    protected final Map<String, AncillaryWall> mWallsById = new TreeMap<>();
    protected final Map<String, AncillaryWallAnchor> mAnchorsById = new TreeMap<>();

    public AncillaryWall createNewWall() {
        return AncillaryWall.create(IdGenerator.generateUniqueId(), IdGenerator.generateUniqueId(), IdGenerator.generateUniqueId(),
            mWallsById, mAnchorsById);
    }

    /**
     * Configures the given virtual handle; sets its position, wall bevel and connects it to docked walls.
     * @param anchorWallHandle Wall handle which is already present in this wall model and whose situation should be updated to the "real world".
     * @param position The position to set in the given handle. The position will not be propagated to possible docked anchors.
     * @param wallBevel Connection wall bevel for the potential dock of the given handle.
     * @param oDockAnchor Anchor in the "real world" whose dock participants will be mirrored into this model.
     */
    public void updateVirtualHandle(AncillaryWallAnchor anchorWallHandle, Position2D position, WallBevelType wallBevel, Optional<Anchor> oDockAnchor) {
        oDockAnchor.ifPresent(dockAnchor -> {
            List<IWallAnchor> dockedAnchors = addDockParticipants(dockAnchor);
            dockedAnchors.add(anchorWallHandle);
            anchorWallHandle.setDockedAnchors(dockedAnchors);
        });

        anchorWallHandle.setPosition(position);

        List<IModelChange> unusedChangeTrace = new ArrayList<>();
        WallAnchorPositions.setWallBevelTypeOfAnchorDock(anchorWallHandle, wallBevel, unusedChangeTrace);
    }

    public List<IWallAnchor> addDockParticipants(Anchor dockAnchor) {
        List<IWallAnchor> result = new ArrayList<>();
        for (Anchor anchor : new ArrayList<>(dockAnchor.getAllDockedAnchors())) {
            if (Wall.isWallHandleAnchor(anchor)) {
                BaseAnchoredObject anchorOwner = anchor.getAnchorOwner();
                AncillaryWall wall = mWallsById.get(anchorOwner.getId());
                if (wall == null) {
                    // If wall was not added yet, add it
                    wall = addWall((Wall) anchorOwner);
                }
                AncillaryWallAnchor participant = mAnchorsById.get(anchor.getId());
                result.add(participant);
                participant.setDockedAnchors(result);
            }
        }
        return result;
    }

    public AncillaryWall addWall(Wall wall) {
        return AncillaryWall.fromWall(wall, mWallsById, mAnchorsById);
    }

    public Map<String, AncillaryWall> getWallsById() {
        return mWallsById;
    }

    public Map<String, AncillaryWallAnchor> getAnchorsById() {
        return mAnchorsById;
    }

    public void addWallsForFeedback(boolean dimensionings, boolean alignments, AncillaryWall... walls) {
        Collection<AncillaryWall> wallCollection = Arrays.asList(walls);
        addWallsForFeedback(dimensionings, alignments, wallCollection);
    }

    public void addWallsForFeedback(boolean dimensionings, boolean alignments, Collection<AncillaryWall> walls) {
        if (dimensionings) {
            mWallsForDimensioningsFeedback.addAll(walls);
        }
        if (alignments) {
            mWallsForAlignentsFeedback.addAll(walls);
        }
    }

    public Collection<AncillaryWall> getWallsForDimensioningsFeedback() {
        return mWallsForDimensioningsFeedback;
    }

    public Collection<AncillaryWall> getWallsForAlignentsFeedback() {
        return mWallsForAlignentsFeedback;
    }

    public Optional<AncillaryWallAnchor> getWallAnchorForMovingHandleFeedback() {
        return mOWallAnchorForMovingHandleFeedback;
    }

    public void setWallAnchorForMovingHandleFeedback(AncillaryWallAnchor value) {
        mOWallAnchorForMovingHandleFeedback = Optional.of(value);
    }
}
