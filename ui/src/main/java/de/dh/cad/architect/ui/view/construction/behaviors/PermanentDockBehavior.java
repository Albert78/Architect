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

import java.text.MessageFormat;
import java.util.List;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.view.AbstractUiMode;

public class PermanentDockBehavior extends AbstractDockBehavior {
    public PermanentDockBehavior(AnchorConstructionRepresentation handleAnchorRepresentation, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(handleAnchorRepresentation, parentMode);
    }

    @Override
    protected String getDockToSelectedAnchorTitle(Anchor anchor) {
        return MessageFormat.format(Strings.ACTION_PERMANENT_DOCK_TO_SELECTED_ANCHOR_TITLE, anchor.getId());
    }

    @Override
    protected void executeDockToSelectedAnchorAction(Anchor anchor) {
        mView.getUiController().dock(mHandleAnchor, anchor, DockConflictStrategy.Exception);
    }

    @Override
    protected String getCancelDockActionTitle() {
        return Strings.ACTION_CANCEL_PERMANENT_DOCK;
    }

    @Override
    protected boolean canDockToAnchor(Anchor anchor) {
        return getUiController().checkDockConflicts(mHandleAnchor, anchor).isEmpty();
    }

    @Override
    protected String getInteractionsTabTitle() {
        return Strings.INTERACTIONS_TAB_PERMANENT_DOCK_OPERATION_TITLE;
    }

    @Override
    protected String getDockToTargetButtonTitle() {
        return Strings.DOCK_OPERATION_PERMANENT_DOCK_TO_TARGET_ANCHOR;
    }

    @Override
    public String getTitle() {
        return Strings.PERMANENT_DOCK_BEHAVIOR_TITLE;
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        // No actions for this behavior yet
    }

    @Override
    protected String getDefaultUserHint() {
        return MessageFormat.format(Strings.PERMANENT_DOCK_BEHAVIOR_USER_HINT, BaseObjectUIRepresentation.getShortName(mHandleAnchor));
    }
}