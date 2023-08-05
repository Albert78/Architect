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
package de.dh.cad.architect.ui.view.construction;

import java.util.Optional;

import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.CeilingConstructionRepresentation;
import de.dh.cad.architect.ui.objects.DimensioningConstructionRepresentation;
import de.dh.cad.architect.ui.objects.FloorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.SupportObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.WallConstructionRepresentation;
import de.dh.cad.architect.ui.objects.WallHoleConstructionRepresentation;

public class SupportObjectsUIElementFilter extends ConstructionUIElementFilter {
    @Override
    protected Optional<Double> getUIElementOpacity(Abstract2DRepresentation repr) {
        if (repr instanceof CeilingConstructionRepresentation) {
            return Optional.of(0.5);
        }
        return super.getUIElementOpacity(repr);
    }

    @Override
    public boolean isUIElementMouseTransparent(Abstract2DRepresentation repr) {
        return false;
    }

    @Override
    public boolean isUIElementVisible(Abstract2DRepresentation repr) {
        return !repr.getModelObject().isHidden() && (
                repr instanceof WallConstructionRepresentation
                || repr instanceof WallHoleConstructionRepresentation
                || repr instanceof FloorConstructionRepresentation
                || repr instanceof DimensioningConstructionRepresentation
                || repr instanceof SupportObjectConstructionRepresentation);
    }
}