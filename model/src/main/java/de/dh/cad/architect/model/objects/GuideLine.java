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
package de.dh.cad.architect.model.objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.jaxb.LengthJavaTypeAdapter;

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

    public GuideLine(String id, String name, GuideLineDirection direction, Length position, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        super(id, name);
        ownerContainer.addOwnedChild_Internal(this, changeSet);
        mDirection = direction;
        mPosition = position;
    }

    @XmlElement(name = "Direction")
    public GuideLineDirection getDirection() {
        return mDirection;
    }

    public void setDirection(GuideLineDirection value) {
        mDirection = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "Position")
    public Length getPosition() {
        return mPosition;
    }

    public void setPosition(Length value) {
        mPosition = value;
    }
}
