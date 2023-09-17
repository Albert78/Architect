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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;

public class ConstructionNullBehavior extends AbstractConstructionBehavior {
    public ConstructionNullBehavior() {
        super(null);
    }

    @Override
    public String getTitle() {
        return "-";
    }

    @Override
    public void setDefaultUserHint() {
        // Do nothing
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        // No actions for this behavior yet
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        // Do nothing
    }

    @Override
    public void uninstall() {
        // Do nothing
    }

    @Override
    public void onObjectFocusChanged(Abstract2DRepresentation repr, boolean focused) {
        // DO nothing
    }

    @Override
    protected void onObjectSpotChanged(Abstract2DRepresentation repr, boolean isSpotted) {
        // DO nothing
    }

    @Override
    public void onObjectsSelectionChanged(Collection<Abstract2DRepresentation> removedSelectionReprs,
        Collection<Abstract2DRepresentation> addedSelectionReprs) {
        // DO nothing
    }

    @Override
    public void onObjectsAdded(Collection<Abstract2DRepresentation> reprs) {
        // Do nothing
    }

    @Override
    public void onObjectsRemoved(Collection<Abstract2DRepresentation> reprs) {
        // Do nothing
    }

    @Override
    public void onObjectsChanged(Collection<Abstract2DRepresentation> reprs) {
        // Do nothing
    }

    @Override
    protected void configureDefaultObjectHandlers(Abstract2DRepresentation repr) {
        // Do nothing
    }

    @Override
    protected void unconfigureDefaultObjectHandlers(Abstract2DRepresentation repr) {
        // Do nothing
    }
}
