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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.jaxb.LengthJavaTypeAdapter;
import de.dh.cad.architect.utils.IdGenerator;

public class GuideLine extends BaseObject {
    public enum GuideLineDirection {
        /**
         * Horizontal guide line whose position is interpreted as the Y coordinate.
         */
        Horizontal,

        /**
         * Vertical guide line whose position is interpreted as the X coordinate.
         */
        Vertical;
    }

    protected Length mPosition;
    protected GuideLineDirection mDirection;

    public GuideLine() {
        // For JAXB
    }

    public GuideLine(String id, String name, GuideLineDirection direction, Length position) {
        super(id, name);
        mDirection = direction;
        mPosition = position;
    }

    public static GuideLine create(String name, GuideLineDirection direction, Length position, IObjectsContainer ownerContainer, List<IModelChange> changeTrace) {
        GuideLine result = new GuideLine(IdGenerator.generateUniqueId(GuideLine.class), name, direction, position);
        ownerContainer.addOwnedChild_Internal(result, changeTrace);
        return result;
    }

    @XmlTransient
    public GuideLineDirection getDirection() {
        return mDirection;
    }

    public void setDirection(GuideLineDirection value, List<IModelChange> changeTrace) {
        GuideLineDirection oldDirection = mDirection;
        mDirection = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setDirection(oldDirection, undoChangeTrace);
            }
        });
    }

    @XmlTransient
    public Length getPosition() {
        return mPosition;
    }

    public void setPosition(Length value, List<IModelChange> changeTrace) {
        Length oldPosition = mPosition;
        mPosition = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setPosition(oldPosition, undoChangeTrace);
            }
        });
    }

    @XmlElement(name = "Direction")
    public GuideLineDirection getDirection_JAXB() {
        return mDirection;
    }

    public void setDirection_JAXB(GuideLineDirection value) {
        mDirection = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "Position")
    public Length getPosition_JAXB() {
        return mPosition;
    }

    public void setPosition_JAXB(Length value) {
        mPosition = value;
    }
}
