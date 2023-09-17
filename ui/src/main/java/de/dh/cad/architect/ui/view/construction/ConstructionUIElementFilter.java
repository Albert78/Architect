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
import de.dh.cad.architect.ui.view.AbstractUIElementFilter;

public class ConstructionUIElementFilter extends AbstractUIElementFilter<Abstract2DRepresentation> {
    /**
     * Gets the opacity of the given UI element, if it's different from default.
     */
    @Override
    protected Optional<Double> getUIElementOpacity(Abstract2DRepresentation repr) {
        return Optional.empty();
    }

    /**
     * Gets the information if the given UI element is visible.
     */
    @Override
    public boolean isUIElementVisible(Abstract2DRepresentation repr) {
        return true;
    }

    @Override
    public boolean isUIElementMouseTransparent(Abstract2DRepresentation repr) {
        return false;
    }

    @Override
    public void configure(Abstract2DRepresentation repr) {
        getUIElementOpacity(repr).ifPresent(value -> {
            repr.setOpacity(value);
        });
        repr.setVisible(isUIElementVisible(repr) && !repr.getModelObject().isHidden());
        repr.setMouseTransparent(isUIElementMouseTransparent(repr));
    }

    @Override
    public void unconfigure(Abstract2DRepresentation repr) {
        repr.setOpacity(Abstract2DRepresentation.OPACITY_DEFAULT);
        repr.setVisible(true);
    }
}
