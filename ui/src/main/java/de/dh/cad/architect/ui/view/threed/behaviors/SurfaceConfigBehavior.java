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
import de.dh.cad.architect.ui.controls.SurfaceConfigControl;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.objects.SurfaceData;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.InteractionsControl;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Shape3D;

public class SurfaceConfigBehavior extends BaseSurfaceConfigBehavior {
    protected final SurfaceData<? extends Shape3D> mSurface;
    protected final SurfaceConfigControl mSurfaceConfigControl;

    public SurfaceConfigBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode, SurfaceData<? extends Shape3D> surface) {
        super(parentMode);
        mSurface = surface;

        mSurfaceConfigControl = new SurfaceConfigControl(surface, getUiController());
    }

    public SurfaceData<? extends Shape3D> getSurface() {
        return mSurface;
    }

    @Override
    protected boolean updateBehavior(Collection<Abstract3DRepresentation> selectedReprs) {
        if (selectedReprs.size() == 1 && selectedReprs.iterator().next() == mSurface.getOwnerRepr()) {
            // Suppress behavior change on selection of the parent object of our surface because
            // the selection change happens after this behavior was started due to the current call structure
            // in BaseSurfaceConfigBehavior.switchToConfigSurfaceBehavior()
            return false;
        }
        return super.updateBehavior(selectedReprs);
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        actions.add(createCancelBehaviorAction(Strings.THREE_D_SURFACE_CONFIG__BEHAVIOR_CANCEL_BEHAVIOR_ACTION_TITLE));

        mActionsList.setAll(actions);
    }

    @Override
    public String getTitle() {
        return Strings.THREE_D_SURFACE_CONFIG_BEHAVIOR_TITLE;
    }

    @Override
    public void setDefaultUserHint() {
        setUserHint(Strings.THREE_D_SURFACE_CONFIG_BEHAVIOR_USER_HINT);
    }

    protected void createInteractionsPane() {
        setInteractionsControl(new InteractionsControl(new ScrollPane(mSurfaceConfigControl), Strings.THREE_D_SURFACE_CONFIG_BEHAVIOR_INTERACTIONS_TAB_TITLE, true));
    }

    @Override
    public void install(AbstractPlanView<Abstract3DRepresentation, Abstract3DAncillaryObject> view) {
        super.install(view);
        createInteractionsPane();
    }

    @Override
    public void uninstall() {
        super.uninstall();
    }
}
