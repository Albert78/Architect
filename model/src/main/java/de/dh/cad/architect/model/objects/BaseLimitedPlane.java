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
package de.dh.cad.architect.model.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;
import de.dh.cad.architect.model.coords.IPosition;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Position3D;
import de.dh.cad.architect.utils.IdGenerator;

/**
 * Base class for all plane-like objects whose extends are defined in 2D by two-dimensional anchors and whose actual
 * plane is given by a custom function. The plane's normal can not be orthogonal to the Z axis.
 */
public abstract class BaseLimitedPlane extends BaseSolidObject {
    public static class PlaneEdge {
        protected final Anchor mEdgeHandleAnchor;
        protected final Anchor mEdgePositionAnchor;

        public PlaneEdge(Anchor edgeHandleAnchor, Anchor edgePositionAnchor) {
            mEdgeHandleAnchor = edgeHandleAnchor;
            mEdgePositionAnchor = edgePositionAnchor;
        }

        public Anchor getEdgeHandleAnchor() {
            return mEdgeHandleAnchor;
        }

        public Anchor getEdgePositionAnchor() {
            return mEdgePositionAnchor;
        }
    }

    public static final String PREFIX_EDGE_ANCHOR_HANDLE = "Edge-Handle-";
    public static final String PREFIX_EDGE_ANCHOR_POSITION = "Edge-Position-";

    protected List<String> mEdgeAnchorTypes = new ArrayList<>();

    public BaseLimitedPlane() {
        // For JAXB
    }

    protected BaseLimitedPlane(String id, String name) {
        super(id, name);
    }

    public abstract Length getHeightAtPosition(Position2D xyPosition);

    public Position3D get3DPosition(Position2D xyPosition) {
        return xyPosition.upscale(getHeightAtPosition(xyPosition));
    }

    protected String getEdgeHandleAnchorTypeId(Anchor edgeHandleAnchor) {
        if (edgeHandleAnchor.getAnchorOwner() != this) {
            throw new IllegalArgumentException("The given anchor <" + edgeHandleAnchor + "> must be an anchor of <" + this + ">");
        }
        String anchorType = edgeHandleAnchor.getAnchorType();
        if (StringUtils.isEmpty(anchorType) || !anchorType.startsWith(PREFIX_EDGE_ANCHOR_HANDLE)) {
            throw new IllegalArgumentException("The given anchor <" + edgeHandleAnchor + "> is not an edge handle anchor");
        }
        String result = anchorType.substring(PREFIX_EDGE_ANCHOR_HANDLE.length());
        if (!mEdgeAnchorTypes.contains(result)) {
            // Should never happen, in this case, the model is broken/inconsistent
            throw new RuntimeException("The given anchor <" + edgeHandleAnchor + "> doesn't belong to <" + this + ">");
        }
        return result;
    }

    protected String getEdgePositionAnchorTypeId(Anchor edgePositionAnchor) {
        if (edgePositionAnchor.getAnchorOwner() != this) {
            throw new IllegalArgumentException("The given anchor <" + edgePositionAnchor + "> must be an anchor of <" + toString() + ">");
        }
        String anchorType = edgePositionAnchor.getAnchorType();
        if (StringUtils.isEmpty(anchorType) || !anchorType.startsWith(PREFIX_EDGE_ANCHOR_POSITION)) {
            throw new IllegalArgumentException("The given anchor <" + edgePositionAnchor + "> is not an edge position anchor");
        }
        return anchorType.substring(PREFIX_EDGE_ANCHOR_POSITION.length());
    }

    protected void addEdgeAnchorType_Internal(int indexAfter, String edgeAnchorTypeId, List<IModelChange> changeTrace) {
        mEdgeAnchorTypes.add(indexAfter, edgeAnchorTypeId);
        changeTrace.add(
            new ObjectModificationChange(this) {
                @Override
                public void undo(List<IModelChange> undoChangeTrace) {
                    removeEdgeAnchorType_Internal(edgeAnchorTypeId, undoChangeTrace);
                }
            });
    }

    protected void removeEdgeAnchorType_Internal(String edgeAnchorTypeId, List<IModelChange> changeTrace) {
        int index = mEdgeAnchorTypes.indexOf(edgeAnchorTypeId);
        mEdgeAnchorTypes.remove(edgeAnchorTypeId);
        changeTrace.add(
            new ObjectModificationChange(this) {
                @Override
                public void undo(List<IModelChange> undoChangeTrace) {
                    addEdgeAnchorType_Internal(index, edgeAnchorTypeId, changeTrace);
                }
            });
    }

    /**
     * Creates and adds a new edge anchor before the anchor at the given position.
     * @param edgeHandleAnchorAfter Edge handle anchor which specifies the insert position, the new edge anchor will be added before that
     * edge handle anchor. If the given anchor is {@code null}, the new anchor will be added as last anchor in the edge anchor's list.
     * @param position Position of the anchor which will be created.
     * @param changeTrace Tracked model changes.
     * @return Created edge handle and position anchors.
     */
    public PlaneEdge createEdgeAnchor(Anchor edgeHandleAnchorAfter, Position2D handlePosition, List<IModelChange> changeTrace) {
        int indexAfter = mEdgeAnchorTypes.size();
        if (edgeHandleAnchorAfter != null) {
            String edgeAnchorTypeIdAfter = getEdgeHandleAnchorTypeId(edgeHandleAnchorAfter);
            indexAfter = mEdgeAnchorTypes.indexOf(edgeAnchorTypeIdAfter);
        }
        Position3D position3D = get3DPosition(handlePosition);
        String edgeAnchorTypeId = IdGenerator.generateUniqueId();
        String edgeHandleType = PREFIX_EDGE_ANCHOR_HANDLE + edgeAnchorTypeId;
        String edgePositionType = PREFIX_EDGE_ANCHOR_POSITION + edgeAnchorTypeId;
        Anchor handleAnchor = createAnchor(edgeHandleType, handlePosition, changeTrace);

        Anchor positionAnchor = createAnchor(edgePositionType, position3D, changeTrace);
        addEdgeAnchorType_Internal(indexAfter, edgeAnchorTypeId, changeTrace);
        return new PlaneEdge(handleAnchor, positionAnchor);
    }

    /**
     * Removes the given edge.
     * Edge anchor and connected position anchor must not be docked, the anchors must be removed from all docks before.
     */
    public void removeEdge(Anchor edgeHandleAnchor, List<IModelChange> changeTrace) {
        if (!isEdgeHandleAnchor(edgeHandleAnchor)) {
            throw new IllegalArgumentException("The given anchor is not an edge handle anchor of <" + this + ">");
        }
        String edgeAnchorTypeId = getEdgeHandleAnchorTypeId(edgeHandleAnchor);
        Anchor edgePositionAnchor = getEdgePositionAnchorForConnectedEdgeHandleAnchor(edgeHandleAnchor);
        removeEdgeAnchorType_Internal(edgeAnchorTypeId, changeTrace);

        edgeHandleAnchor.delete(changeTrace);
        edgePositionAnchor.delete(changeTrace);
    }

    public Anchor getEdgePositionAnchorForConnectedEdgeHandleAnchor(Anchor edgeHandleAnchor) {
        String edgeAnchorTypeId = getEdgeHandleAnchorTypeId(edgeHandleAnchor);
        return getAnchorByAnchorType(PREFIX_EDGE_ANCHOR_POSITION + edgeAnchorTypeId);
    }

    public Anchor getEdgeHandleAnchorForConnectedEdgePositionAnchor(Anchor edgeHandleAnchor) {
        String edgeAnchorTypeId = getEdgePositionAnchorTypeId(edgeHandleAnchor);
        return getAnchorByAnchorType(PREFIX_EDGE_ANCHOR_HANDLE + edgeAnchorTypeId);
    }

    public static boolean isEdgeHandleAnchor(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return anchorType != null && anchorType.startsWith(PREFIX_EDGE_ANCHOR_HANDLE);
    }

    public static boolean isEdgePositionAnchor(Anchor anchor) {
        String anchorType = anchor.getAnchorType();
        return anchorType != null && anchorType.startsWith(PREFIX_EDGE_ANCHOR_POSITION);
    }

    @Override
    public boolean isHandle(Anchor anchor) {
        return isEdgeHandleAnchor(anchor);
    }

    public int getNumEdges() {
        return mEdgeAnchorTypes.size();
    }

    @XmlElementWrapper(name = "EdgeAnchors")
    @XmlElement(name = "Edge")
    public List<String> getEdgeAnchorTypes() {
        return mEdgeAnchorTypes;
    }

    @XmlTransient
    public List<Anchor> getEdgePositionAnchors() {
        List<Anchor> result = new ArrayList<>();
        for (String edgeAnchorTypeId : getEdgeAnchorTypes()) {
            Anchor anchor = getAnchorByAnchorType(PREFIX_EDGE_ANCHOR_POSITION + edgeAnchorTypeId);
            result.add(anchor);
        }
        return result;
    }

    @XmlTransient
    public List<Anchor> getEdgeHandleAnchors() {
        List<Anchor> result = new ArrayList<>();
        for (String edgeAnchorTypeId : getEdgeAnchorTypes()) {
            Anchor anchor = getAnchorByAnchorType(PREFIX_EDGE_ANCHOR_HANDLE + edgeAnchorTypeId);
            result.add(anchor);
        }
        return result;
    }

    @Override
    public ReconcileResult reconcileAfterHandleChange(List<IModelChange> changeTrace) throws IllegalStateException {
        Collection<Anchor> changedDependentAnchors = new ArrayList<>();
        for (Anchor edgeHandleAnchor : getEdgeHandleAnchors()) {
            Anchor edgePositionAnchor = getEdgePositionAnchorForConnectedEdgeHandleAnchor(edgeHandleAnchor);
            IPosition position = edgeHandleAnchor.getPosition();
            edgePositionAnchor.setPosition(get3DPosition(position.projectionXY()), changeTrace);
            changedDependentAnchors.add(edgePositionAnchor);
        }
        return new ReconcileResult(changedDependentAnchors);
    }

    @Override
    public boolean isPossibleDimensioningDockTargetAnchor(Anchor anchor) {
        return isEdgeHandleAnchor(anchor);
    }
}
