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
package de.dh.cad.architect.ui.view.construction.feedback;

import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;

public class AncillaryPosition {
    protected final Position2D mPosition;
    protected final Optional<Anchor> mOAnchor;

    public AncillaryPosition(Position2D position, Optional<Anchor> oAnchor) {
        mPosition = position;
        mOAnchor = oAnchor;
    }

    public static AncillaryPosition wrap(Anchor anchor) {
        return anchor == null ? null : new AncillaryPosition(anchor.getPosition().projectionXY(), Optional.of(anchor));
    }

    public static AncillaryPosition wrap(Position2D position) {
        return new AncillaryPosition(position, Optional.empty());
    }

    public Position2D getPosition() {
        return mPosition;
    }

    public Optional<Anchor> getOAnchor() {
        return mOAnchor;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mPosition == null) ? 0 : mPosition.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AncillaryPosition other = (AncillaryPosition) obj;
        if (!mPosition.equals(other.mPosition))
            return false;
        return true;
    }
}
