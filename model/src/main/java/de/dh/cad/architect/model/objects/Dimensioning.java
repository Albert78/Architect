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

import java.util.List;
import java.util.Optional;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.utils.IdGenerator;

public class Dimensioning extends BaseAnchoredObject {
    public static final String AP_POSITION_1 = "End-1";
    public static final String AP_POSITION_2 = "End-2";

    protected double mLabelDistancePt = 0.0;
    protected String mLabel = null;

    public Dimensioning() {
        // For JAXB
    }

    public Dimensioning(String id, String name, double viewDistance) {
        super(id, name);
        mLabelDistancePt = viewDistance;
    }

    public static Dimensioning create(String name, Position2D pos1, Position2D pos2, double viewDistance, IObjectsContainer ownerContainer, List<IModelChange> changeTrace) {
        Dimensioning result = new Dimensioning(IdGenerator.generateUniqueId(Dimensioning.class), name, viewDistance);
        ownerContainer.addOwnedChild_Internal(result, changeTrace);
        result.createAnchor(AP_POSITION_1, pos1, changeTrace);
        result.createAnchor(AP_POSITION_2, pos2, changeTrace);
        return result;
    }

    public Anchor getAnchor1() {
        return mAnchors.get(0);
    }

    public Anchor getAnchor2() {
        return mAnchors.get(1);
    }

    @Override
    public boolean isHandle(Anchor anchor) {
        return true;
    }

    /**
     * Gets the distance of the dimensioning label / length text to the line between the points to be measured.
     * The label distance stands orthogonal to that line.
     */
    @XmlTransient
    public double getLabelDistance() {
        return mLabelDistancePt;
    }

    public void setLabelDistance(double value, List<IModelChange> changeTrace) {
        double oldLabelDistancePt = mLabelDistancePt;
        mLabelDistancePt = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public Optional<IModelChange> tryMerge(IModelChange oldChange) {
                if (getClass().equals(oldChange.getClass())) {
                    return Optional.of(oldChange);
                }
                return Optional.empty();
            }

            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setLabelDistance(oldLabelDistancePt, undoChangeTrace);
            }
        });
    }

    /**
     * Gets the user defined label to be displayed in the UI for this dimensioning instead of the pure distance value.
     * For embedding the distance value in the displayed string, use {@code "{0}"}.
     */
    @XmlTransient
    public String getLabel() {
        return mLabel;
    }

    public void setLabel(String value, List<IModelChange> changeTrace) {
        String oldLabel = mLabel;
        mLabel = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setLabel(oldLabel, undoChangeTrace);
            }
        });
    }

    @Override
    public boolean isPossibleDimensioningDockTargetAnchor(Anchor anchor) {
        return false;
    }

    @XmlElement(name = "Label")
    public String getLabel_JAXB() {
        return mLabel;
    }

    public void setLabel_JAXB(String value) {
        mLabel = value;
    }

    @XmlElement(name = "LabelDistance")
    public double getLabelDistance_JAXB() {
        return mLabelDistancePt;
    }

    public void setLabelDistance_JAXB(double value) {
        mLabelDistancePt = value;
    }
}
