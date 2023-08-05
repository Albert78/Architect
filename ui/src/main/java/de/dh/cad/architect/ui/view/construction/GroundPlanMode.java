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

import java.util.Collection;

import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.construction.behaviors.AbstractConstructionBehavior;
import de.dh.cad.architect.ui.view.construction.behaviors.EditSelectedAnchorBehavior;
import de.dh.cad.architect.ui.view.construction.behaviors.EditSelectedObjectBehavior;
import de.dh.cad.architect.ui.view.construction.behaviors.GroundPlanDefaultBehavior;

public class GroundPlanMode extends AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> {
    public GroundPlanMode(UiController uiController) {
        super(uiController);
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);
        mUiController.addContextMenuProvider(mContextMenuProvider_SetObjectVisibility);
    }

    @Override
    public void uninstall() {
        mUiController.removeContextMenuProvider(mContextMenuProvider_SetObjectVisibility);
    }

    @Override
    public AbstractConstructionBehavior getBehaviorForSelectedReprs(Collection<Abstract2DRepresentation> selectedReprs) {
        Abstract2DRepresentation singleSelectedRepr = selectedReprs.size() == 1 ? selectedReprs.iterator().next() : null;

        if (singleSelectedRepr != null) {
            // Edit single objects
            if (EditSelectedAnchorBehavior.canEditObject(singleSelectedRepr)) {
                return new EditSelectedAnchorBehavior((AnchorConstructionRepresentation) singleSelectedRepr, this);
            } else if (EditSelectedObjectBehavior.canEditObject(singleSelectedRepr)) {
                return new EditSelectedObjectBehavior(singleSelectedRepr, this);
            }
        }
        return new GroundPlanDefaultBehavior(this);
    }
}
