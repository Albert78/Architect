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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.util.Optional;

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.view.AbstractUiMode;

/**
 * Second part of the two-part behavior to create walls. First part is {@link GroundPlanStartWallBehavior}.
 */
public abstract class AbstractConstructionSelectSecondAnchorOrPositionBehavior extends AbstractConstructionSelectAnchorOrPositionBehavior {
    // One of those two is set
    protected final Optional<Anchor> mOStartDockAnchor;
    protected final Optional<Position2D> mOStartPosition;

    public AbstractConstructionSelectSecondAnchorOrPositionBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode, Optional<Anchor> oStartDockAnchor, Optional<Position2D> oStartPosition) {
        super(parentMode);
        mOStartDockAnchor = oStartDockAnchor;
        mOStartPosition = oStartPosition;
    }

    protected Position2D getStartPosition() {
        return mOStartDockAnchor.map(startDockAnchor -> startDockAnchor.getPosition().projectionXY()).orElse(mOStartPosition.orElse(Position2D.zero()));
    }

    @Override
    protected void secondaryButtonClicked() {
        mParentMode.resetBehavior();
    }
}
