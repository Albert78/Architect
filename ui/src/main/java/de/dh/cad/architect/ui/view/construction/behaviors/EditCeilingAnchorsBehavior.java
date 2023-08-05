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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.CeilingConstructionRepresentation;
import de.dh.cad.architect.ui.objects.CeilingConstructionRepresentation.AnchorDragMode;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.construction.GroundPlanUIElementFilter;

/**
 * Behavior to edit the ceiling anchors of a ceiling.
 */
public class EditCeilingAnchorsBehavior extends AbstractConstructionBehavior {
    protected class EditCeilingAnchorsUIElementFilter extends GroundPlanUIElementFilter {
        @Override
        public boolean isUIElementVisible(Abstract2DRepresentation repr) {
            if (super.isUIElementVisible(repr)) {
                return true;
            }
            if (repr instanceof AnchorConstructionRepresentation anchorRepr) {
                return Objects.equals(anchorRepr.getAnchorOwnerRepresentation(), mEditCeilingRepr) && Ceiling.isCeilingHandleAnchor(anchorRepr.getAnchor());
            }
            return false;
        }

        @Override
        public void configure(Abstract2DRepresentation repr) {
            super.configure(repr);
            if (Objects.equals(repr, mEditCeilingRepr)) {
                mEditCeilingRepr.setAnchorDragMode(AnchorDragMode.CeilingHandleAnchors);
            }
        }

        @Override
        public void unconfigure(Abstract2DRepresentation repr) {
            if (Objects.equals(repr, mEditCeilingRepr)) {
                mEditCeilingRepr.setAnchorDragMode(AnchorDragMode.None);
            }
        }
    }

    protected final CeilingConstructionRepresentation mEditCeilingRepr;

    public EditCeilingAnchorsBehavior(CeilingConstructionRepresentation editCeilingRepr, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        mEditCeilingRepr = editCeilingRepr;
        setUIElementFilter(new EditCeilingAnchorsUIElementFilter());
    }

    public static EditCeilingAnchorsBehavior create(CeilingConstructionRepresentation editCeilingRepr, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        return new EditCeilingAnchorsBehavior(editCeilingRepr, parentMode);
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        // No actions yet

        mActionsList.setAll(actions);
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.EDIT_CEILING_ANCHORS_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.EDIT_CEILING_ANCHORS_BEHAVIOR_TITLE;
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return false;
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
    }

    @Override
    public void uninstall() {
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        super.uninstall();
    }
}