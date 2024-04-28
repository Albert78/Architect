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

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.jaxb.LengthJavaTypeAdapter;
import de.dh.cad.architect.utils.IdGenerator;

// TODO:
// Add children:
// - Holes
public class Floor extends BaseLimitedPlane {
    public static final String SURFACE_TYPE = "Surface";

    protected int mLevel = 0; // German counting; 0 = Base floor where you enter, -1 = cellar, ...

    protected Length mHeight = Length.ZERO;

    public Floor() {
        // For JAXB
    }

    public Floor(String id, int level, Length height, String name) {
        super(id, name);
        mLevel = level;
        mHeight = height;
    }

    @Override
    protected void initializeSurfaces(List<IModelChange> changeTrace) {
        clearSurfaces(changeTrace);
        createSurface(SURFACE_TYPE, changeTrace);
    }

    public static Floor create(int level, Length height, String name, List<Position2D> anchorPositions, IObjectsContainer ownerContainer, List<IModelChange> changeTrace) {
        Floor result = new Floor(IdGenerator.generateUniqueId(Floor.class), level, height, name);
        ownerContainer.addOwnedChild_Internal(result, changeTrace);
        for (Position2D pos : anchorPositions) {
            result.createEdgeAnchor(null, pos, changeTrace);
        }
        result.initializeSurfaces(changeTrace);
        return result;
    }

    @Override
    public Length getHeightAtPosition(Position2D xyPosition) {
        return mHeight;
    }

    public List<Position2D> getEdgePositions() {
        return getEdgeHandleAnchors()
                        .stream()
                        .map(a -> a.projectionXY())
                        .collect(Collectors.toList());
    }

    /**
     * Calculates the area of this floor in m².
     */
    public double calculateAreaM2() {
        List<Position2D> edgePositions = getEdgePositions();
        int n = edgePositions.size();
        if (n < 3) {
            return 0;
        }
        double a = 0;
        for (int i = 0; i < n; i++) {
            Position2D posI = edgePositions.get(i);
            Position2D posI1 = edgePositions.get((i + 1) % n);
            double xi = posI.getX().inM();
            double yi = posI.getY().inM();
            double xi1 = posI1.getX().inM();
            double yi1 = posI1.getY().inM();
            a += xi * yi1 - xi1 * yi;
        }
        return Math.abs(a / 2);
    }

    @XmlTransient
    public int getLevel() {
        return mLevel;
    }

    public void setLevel(int value, List<IModelChange> changeTrace) {
        int oldLevel = mLevel;
        mLevel = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setLevel(oldLevel, undoChangeTrace);
            }
        });
    }

    /**
     * Gets the height of this floor above the {@link #getLevel() level}'s height.
     */
    @XmlTransient
    public Length getHeight() {
        return mHeight;
    }

    public void setHeight(Length value, List<IModelChange> changeTrace) {
        Length oldHeight = mHeight;
        mHeight = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setHeight(oldHeight, undoChangeTrace);
            }
        });
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() + ", Height=" + mHeight + ", Level=" + mLevel;
    }

    public String getAreaString() {
        double area = calculateAreaM2();
        return MessageFormat.format("{0,number,#.##} m²", area);
    }

    @XmlAttribute(name = "level")
    public int getLevel_JAXB() {
        return mLevel;
    }

    public void setLevel_JAXB(int value) {
        mLevel = value;
    }

    @XmlElement(name = "Height")
    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    public Length getHeight_JAXB() {
        return mHeight;
    }

    public void setHeight_JAXB(Length value) {
        mHeight = value;
    }
}
