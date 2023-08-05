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
package de.dh.cad.architect.ui.view.threed;

import java.util.Collection;

import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.threed.behaviors.Abstract3DViewBehavior;
import de.dh.cad.architect.ui.view.threed.behaviors.PainterBehavior;

public class PainterMode extends AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> {
    public PainterMode(UiController uiController) {
        super(uiController);
    }

    @Override
    public Abstract3DViewBehavior getBehaviorForSelectedReprs(Collection<Abstract3DRepresentation> selectedReprs) {
        return new PainterBehavior(this);
    }
}
