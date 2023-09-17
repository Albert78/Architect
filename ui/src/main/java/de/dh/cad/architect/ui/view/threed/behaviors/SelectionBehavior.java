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
package de.dh.cad.architect.ui.view.threed.behaviors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.threed.ThreeDUIElementFilter;

public class SelectionBehavior extends Abstract3DViewBehavior {
    public SelectionBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode) {
        super(parentMode);
        setUIElementFilter(new ThreeDUIElementFilter());
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public void setDefaultUserHint() {
        setUserHint(Strings.THREE_D_SELECTION_BEHAVIOR_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.THREE_D_SELECTION_BEHAVIOR_TITLE;
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        actions.add(createCameraPositionsMenuAction());

        mActionsList.setAll(actions);
    }

    @Override
    protected void installDefaultViewHandlers() {
        super.installDefaultViewHandlers();
        installDefaultDeleteObjectsKeyHandler();
    }

    @Override
    protected void uninstallDefaultViewHandlers() {
        uninstallDefaultDeleteObjectsKeyHandler();
        super.uninstallDefaultViewHandlers();
    }
}