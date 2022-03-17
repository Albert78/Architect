/*******************************************************************************
 *     Architect - A free 2D/3D home and interior designer
 *     Copyright (C) 2021, 2022  Daniel Höh
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.coords.MathUtils;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.SupportObject2DAncillary;
import de.dh.cad.architect.ui.objects.SupportObjectTemplate;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.SupportObjectsUIElementFilter;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Behavior which lets the user choose the position for a new support object to be added to the plan.
 */
public class SupportObjectsAddSupportObjectBehavior extends AbstractConstructionSelectAnchorOrPositionBehavior {
    protected final List<SupportObject2DAncillary> mSOAncillaryObjetcs = new ArrayList<>();
    protected final List<SupportObjectTemplate> mTemplates;

    public SupportObjectsAddSupportObjectBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode, Collection<SupportObjectTemplate> templates) {
        super(parentMode);
        mTemplates = new ArrayList<>(templates);
    }

    public static SupportObjectsAddSupportObjectBehavior newSupportObject(SupportObjectDescriptor descriptor, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        return new SupportObjectsAddSupportObjectBehavior(parentMode, Arrays.asList(SupportObjectTemplate.createNew(descriptor)));
    }

    public static SupportObjectsAddSupportObjectBehavior copySupportObjects(Collection<SupportObject> originalSupportObjets,
        AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        AssetManager assetManager = parentMode.getUiController().getAssetManager();
        Collection<SupportObjectTemplate> templates = new ArrayList<>();
        List<Position2D> positions = originalSupportObjets
                .stream()
                .map(so -> so.getCenterPoint())
                .collect(Collectors.toList());
        Position2D centroid = MathUtils.calculateCentroid(positions);
        for (SupportObject origSO : originalSupportObjets) {
            try {
                templates.add(SupportObjectTemplate.copyOf(origSO, centroid, assetManager));
            } catch (IOException e) {
                showMissingSupportObjectModelAlert();
            }
        }
        return new SupportObjectsAddSupportObjectBehavior(parentMode, templates);
    }

    @Override
    protected void initializeActions() {
        mActionsList.add(createCancelBehaviorAction(Strings.ACTION_GOUND_PLAN_CANCEL_ADD_SUPPORT_OBJECT_TITLE));
    }

    @Override
    protected void initializeUiElementFilter() {
        setUIElementFilter(new SupportObjectsUIElementFilter());
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.ADD_SUPPORT_OBJECT_BEHAVIOR_DEFAULT_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.ADD_SUPPORT_OBJECT_BEHAVIOR_TITLE;
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        // No actions for this behavior yet
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return false;
    }

    @Override
    protected void showPositionFeedback(Position2D pos) {
        for (int i = 0; i < mTemplates.size(); i++) {
            SupportObjectTemplate template = mTemplates.get(i);
            SupportObject2DAncillary obj = mSOAncillaryObjetcs.get(i);
            obj.updatePosition(pos.plus(template.getPositionOffset()), template.getRotationDeg());
        }
    }

    @Override
    protected void showAnchorFeedback(AnchorConstructionRepresentation anchorRepr) {
        showPositionFeedback(anchorRepr.getAnchor().getPosition().projectionXY());

        // TODO: Necessary?
        setDefaultUserHint();
    }

    protected static void showMissingSupportObjectModelAlert() {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(Strings.ADD_SUPPORT_OBJECT_BEHAVIOR_ERROR_SUPPORT_OBJECT_BROKEN_TITLE);
        alert.setHeaderText(Strings.ADD_SUPPORT_OBJECT_BEHAVIOR_ERROR_SUPPORT_OBJECT_BROKEN_HEADER);
        alert.setContentText(Strings.ADD_SUPPORT_OBJECT_BEHAVIOR_ERROR_SUPPORT_OBJECT_BROKEN_CONTENT);

        alert.showAndWait();
    }

    @Override
    protected void triggerPoint(Position2D pos) {
        createNewSupportObject(pos);
        mParentMode.resetBehavior();
    }

    @Override
    protected void triggerAnchor(AnchorConstructionRepresentation anchorRepr) {
        triggerPoint(anchorRepr.getAnchor().getPosition().projectionXY());
    }

    public void createNewSupportObject(Position2D pos) {
        UiController uiController = getUiController();
        try {
            Collection<String> newObjectIds = new ArrayList<>();
            for (SupportObjectTemplate template : mTemplates) {
                SupportObject so = template.createSupportObject(pos, uiController);
                newObjectIds.add(so.getId());
            }
            uiController.setSelectedObjectIds(newObjectIds);
        } catch (IOException e) {
            showMissingSupportObjectModelAlert();
        }
    }

    @Override
    protected boolean isPossibleDockTargetAnchor(Anchor anchor) {
        return false;
    }

    @Override
    protected AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr) {
        return null;
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);
        ConstructionView constructionView = getView();
        mSOAncillaryObjetcs.clear();
        for (SupportObjectTemplate template : mTemplates) {
            SupportObject2DAncillary obj = new SupportObject2DAncillary(constructionView);
            obj.updateSupportObject(template.getSupportObjectDescriptor(), template.getDimensions(), constructionView.getAssetLoader());
            constructionView.addAncillaryObject(obj);
            mSOAncillaryObjetcs.add(obj);
        }
    }

    @Override
    public void uninstall() {
        for (SupportObject2DAncillary obj : mSOAncillaryObjetcs) {
            obj.removeFromView();
        }
        mSOAncillaryObjetcs.clear();
        super.uninstall();
    }
}
