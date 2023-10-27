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
package de.dh.cad.architect.ui.view;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.IObjectContextMenuProvider;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.IModelBasedObject;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

public abstract class AbstractUiMode<TRepr extends IModelBasedObject, TAnc extends Node> {
    // Default event handlers
    protected final IObjectContextMenuProvider mContextMenuProvider_SetObjectVisibility = new IObjectContextMenuProvider() {
        @Override
        public String getMenuName() {
            return Strings.CONSTRUCTION_BEHAVIOR_CONTEXT_MENU_NAME;
        }

        @Override
        public Optional<Collection<MenuItem>> getMenuItems(Collection<String> objectIdsInContext) {
            if (objectIdsInContext.isEmpty()) {
                return Optional.empty();
            }
            MenuItem turnVisibleItem = new MenuItem(objectIdsInContext.size() == 1 ? Strings.CONSTRUCTION_BEHAVIOR_TURN_VISIBLE_1 : Strings.CONSTRUCTION_BEHAVIOR_TURN_VISIBLE_N);
            turnVisibleItem.setOnAction(actionEvent -> {
                mUiController.setObjectsVisibilityByIds(objectIdsInContext, false);
            });
            MenuItem turnInvisibleItem = new MenuItem(objectIdsInContext.size() == 1 ? Strings.CONSTRUCTION_BEHAVIOR_TURN_INVISIBLE_1 : Strings.CONSTRUCTION_BEHAVIOR_TURN_INVISIBLE_N);
            turnInvisibleItem.setOnAction(actionEvent -> {
                mUiController.setObjectsVisibilityByIds(objectIdsInContext, true);
            });
            return Optional.of(Arrays.asList(turnVisibleItem, turnInvisibleItem));
        }
    };

    protected final UiController mUiController;
    protected AbstractPlanView<TRepr, TAnc> mView = null;

    public AbstractUiMode(UiController uiController) {
        mUiController = uiController;
    }

    public UiController getUiController() {
        return mUiController;
    }

    public AbstractPlanView<TRepr, TAnc> getView() {
        return mView;
    }

    /**
     * Installs this mode in the given view.
     * This method configures all views and other UI elements for the user to match this mode.
     */
    public void install(AbstractPlanView<TRepr, TAnc> view) {
        mView = view;
        resetBehavior();
    }

    /**
     * Removes this mode from the given view.
     * This method resets the view and initialized UI elements to their original state.
     */
    public void uninstall() {
        setBehavior(null);
        mView = null;
    }

    public abstract AbstractViewBehavior<TRepr, TAnc> getBehaviorForSelectedReprs(Collection<TRepr> selectedReprs);

    /**
     * Switches to the default behavior for this mode.
     */
    public void resetBehavior() {
        Collection<TRepr> selectedReprs = mView.getRepresentationsByIds(mUiController.selectedObjectIds());
        AbstractViewBehavior<TRepr, TAnc> targetBehavior = getBehaviorForSelectedReprs(selectedReprs);
        setBehavior(targetBehavior);
    }

    public AbstractViewBehavior<TRepr, TAnc> getBehavior() {
        return mView.getBehavior();
    }

    /**
     * Switches to the given behavior.
     */
    public void setBehavior(AbstractViewBehavior<TRepr, TAnc> behavior) {
        mView.setBehavior(behavior);
    }
}
