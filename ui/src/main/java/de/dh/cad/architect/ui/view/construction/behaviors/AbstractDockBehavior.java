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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.utils.Cursors;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.InteractionsControl;
import de.dh.cad.architect.ui.view.construction.EditedAnchorUIElementFilter;
import de.dh.cad.architect.ui.view.construction.behaviors.DockInteractionsPaneControl.DockTargetObject;
import de.dh.cad.architect.ui.view.construction.behaviors.DockInteractionsPaneControl.IChangeListener;
import de.dh.cad.architect.ui.view.construction.feedback.AncillaryPosition;
import de.dh.cad.architect.ui.view.construction.feedback.dock.DockOperationVisualFeedbackManager;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.Scene;

/**
 * Behavior supporting dock-like operations. Subclasses can implement the concrete operation.
 */
public abstract class AbstractDockBehavior extends AbstractConstructionBehavior {
    /* Dependence of data:
     * <b>Handle anchor to dock</b>
     * Comes as constructor parameter, constant over the lifetime of this behavior
     *  -> Passed to DockInteractionsPane, never changed
     *  - Managed by DockInteractionsPane
     * <b>Target object to dock to</b>
     * Can be choosen by the user via object selection, either by clicking in the construction view or in object explorer, e.g.
     *  -> Passed to DockInteractionsPane
     *  - Managed by DockInteractionsPane
     * <b>Target anchor to dock</b>
     * Can be choosen by the user via object selection, either by clicking in the construction view or in object explorer, e.g.
     *  -> Choosen in DockInteractionsPane or passed to there
     *  - Managed by DockInteractionsPane
     * <b>Ancillary objects in view representing dock source and target anchors</b>
     * Managed by this behavior, derived from DockInteractionsPane
     */
    protected final Cursor mCursorForbidden = Cursors.createCursorForbidden();
    protected final Cursor mCursorCrosshair = Cursors.createCursorCrossHair();

    protected final DockInteractionsPaneControl mDockInteractionsPane = new DockInteractionsPaneControl(this::commitAnchor);

    protected final Anchor mHandleAnchor;
    protected final AnchorConstructionRepresentation mHandleAnchorRepresentation;
    protected final Abstract2DRepresentation mAnchorOwnerRepresentation;

    // Data valid from install() to uninstall()
    protected Collection<? extends Anchor> mDockableAnchors = null;
    protected DockOperationVisualFeedbackManager mFeedbackManager = null;

    public AbstractDockBehavior(AnchorConstructionRepresentation handleAnchorRepresentation, AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        mDockInteractionsPane.addChangeListener(new IChangeListener() {
            @Override
            public void changed(DockInteractionsPaneControl pane) {
                updateAncillaryObjects();
                Anchor selectedAnchor = pane.getSelectedTargetAnchor();
                if (selectedAnchor == null) {
                    return;
                }
                Platform.runLater(() -> {
                    UiController uiController = getUiController();
                    ObservableList<String> selectedObjectIds = uiController.selectedObjectIds();
                    String selectedAnchorId = selectedAnchor.getId();
                    if (selectedObjectIds.size() != 1 || !selectedObjectIds.contains(selectedAnchorId)) {
                        uiController.setSelectedObjectId(selectedAnchorId);
                    }
                });
            }
        });
        mHandleAnchorRepresentation = handleAnchorRepresentation;
        mAnchorOwnerRepresentation = mHandleAnchorRepresentation.getAnchorOwnerRepresentation();
        mHandleAnchor = mHandleAnchorRepresentation.getAnchor();
        setUIElementFilter(new EditedAnchorUIElementFilter(handleAnchorRepresentation) {
            @Override
            public boolean isUIElementVisible(Abstract2DRepresentation repr) {
                if (super.isUIElementVisible(repr)) {
                    return true;
                }
                UiController uiController = getUiController();
                if (uiController.selectedObjectIds().contains(repr.getModelId())) {
                    return true;
                }
                return false;
            }

            @Override
            protected Optional<Double> getUIElementOpacity(Abstract2DRepresentation repr) {
                if (repr.equals(mHandleAnchorRepresentation)) {
                    return Optional.of(0.4);
                }
                return super.getUIElementOpacity(repr);
            }
        });
    }

    public Anchor getAnchor() {
        return mHandleAnchor;
    }

    protected abstract boolean canDockToAnchor(Anchor anchor);
    protected abstract String getInteractionsTabTitle();
    protected abstract String getDefaultUserHint();
    protected abstract String getDockToSelectedAnchorTitle(Anchor anchor);
    protected abstract void executeDockToSelectedAnchorAction(Anchor anchor);
    protected abstract String getCancelDockActionTitle();
    protected abstract String getDockToTargetButtonTitle();

    // Attention: This method is called during #install() when the member variables are not initialized yet
    protected Collection<Anchor> getDockableAnchors() {
        Collection<Anchor> allAnchors = getUiController().getPlan().getAnchors().values();
        return allAnchors.stream().filter(anchor -> canDockToAnchor(anchor)).collect(Collectors.toList());
    }

    @Override
    public void onObjectsSelectionChanged(Collection<Abstract2DRepresentation> removedSelectionReprs,
        Collection<Abstract2DRepresentation> addedSelectionReprs) {
        configureObjects();
        List<BaseObject> selectedObjects = getSelectedObjects();
        if (selectedObjects.size() == 1) {
            BaseObject selectedObject = selectedObjects.get(0);
            if (selectedObject instanceof Anchor anchor) {
                if (mDockableAnchors.contains(anchor)) {
                    configureDockTargetObject(anchor.getAnchorOwner(), anchor);
                    return;
                }
            } else if (selectedObject instanceof BaseAnchoredObject bao) {
                if (hasDockableAnchors(bao)) {
                    configureDockTargetObject(bao, null);
                    return;
                }
            }
        }
        configureDockTargetObject(null, null);
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public abstract String getTitle();

    protected boolean hasDockableAnchors(BaseAnchoredObject bao) {
        return bao.getAnchors()
            .stream()
            .filter(this::canDockToAnchor)
            .findAny()
            .isPresent();
    }

    /**
     * Shows feedback from mouse-over over a dockable target object.
     */
    protected void showTargetObjectFeedback(AbstractAnchoredObjectConstructionRepresentation ownerObjRepr) {
        if (ownerObjRepr == null) {
           setDefaultUserHint();
        } else {
            BaseAnchoredObject ownerObject = ownerObjRepr.getModelObject();
            setUserHint(MessageFormat.format(Strings.DOCK_OPERATION_CHOOSE_TARGET_OBJECT_0, BaseObjectUIRepresentation.getShortName(ownerObject)));
        }
    }

    /**
     * Shows feedback from mouse-over over a dockable target anchor.
     */
    protected void showAnchorFeedback(AnchorConstructionRepresentation anchorRepr) {
        Scene scene = mView.getScene();
        if (anchorRepr == null) {
            scene.setCursor(Cursor.DEFAULT);
            setUserHint(getDefaultUserHint());
        } else {
            Anchor anchor = anchorRepr.getAnchor();
            if (mDockableAnchors.contains(anchor)) {
                // Dock allowed
                scene.setCursor(mCursorCrosshair);
                setUserHint(MessageFormat.format(Strings.DOCK_OPERATION_TARGET_ANCHOR_0, BaseObjectUIRepresentation.getShortName(anchor)));
            } else {
                scene.setCursor(mCursorForbidden);
            }
        }
    }

    /**
     * Handles the click on a dockable target object.
     * Selects the choosen object which indirectly sets it as target object in the dock interactions pane.
     */
    protected void onTargetObjectClicked(AbstractAnchoredObjectConstructionRepresentation ownerObjRepr) {
        setDefaultUserHint();
        getUiController().setSelectedObjectId(ownerObjRepr.getModelId());
    }

    /**
     * Handles the click on a dockable target anchor.
     * Selects the choosen object which indirectly sets it as target anchor in the dock interactions pane.
     */
    protected void onTargetAnchorClicked(AnchorConstructionRepresentation anchorRepr) {
        getUiController().setSelectedObjectId(anchorRepr.getModelId());
    }

    /**
     * Updates the dock interaction pane to a selected object or anchor.
     * @param dockableObject Object providing dockable anchors to be set as target object in the dock interactions pane.
     * Can be {@code null}, in this case the target object in the dock interactions pane will be cleared.
     * @param anchor Anchor of the {@code dockableObject} to be selected as target anchor. Can be {@code null},
     * in this case the target anchor in the dock interactions pane will be cleared.
     */
    protected void configureDockTargetObject(BaseAnchoredObject dockableObject, Anchor anchor) {
        if (dockableObject == null) {
            mDockInteractionsPane.setDockTargetObject(null, null);
        } else {
            Collection<Anchor> dockableTargetAnchors = CollectionUtils.intersection(mDockableAnchors, dockableObject.getAnchors());
            mDockInteractionsPane.setDockTargetObject(new DockTargetObject(dockableObject, dockableTargetAnchors), anchor);
        }
    }

    /**
     * Finally triggers this operation with the given target anchor.
     */
    protected void commitAnchor(Anchor anchor) {
        executeDockToSelectedAnchorAction(anchor);
        mParentMode.resetBehavior();
    }

    protected void updateAncillaryObjects() {
        AncillaryPosition sourcePosition = AncillaryPosition.wrap(mHandleAnchor);
        Anchor targetAnchor = mDockInteractionsPane.getSelectedTargetAnchor();
        AncillaryPosition targetPosition = AncillaryPosition.wrap(targetAnchor);
        mFeedbackManager.updateVisualObjects(sourcePosition, targetPosition);
    }

    @Override
    protected void configureObject(Abstract2DRepresentation repr) {
        // Skip default event handlers like click event and default spot event; we have our own click handlers

        // Set up mouse-over handlers
        if (repr instanceof AbstractAnchoredObjectConstructionRepresentation aaocr) {
            BaseAnchoredObject modelObject = aaocr.getModelObject();
            if (hasDockableAnchors(modelObject) && !aaocr.equals(mAnchorOwnerRepresentation)) {
                aaocr.enableMouseOverSpot();

                aaocr.installAnchorChoiceFeature(
                    this::canDockToAnchor,
                    this::showTargetObjectFeedback,
                    this::onTargetObjectClicked,
                    this::showAnchorFeedback,
                    this::onTargetAnchorClicked,
                    this::isAnchorVisible,
                    Optional.empty());
            }
        }
        if (repr.equals(mAnchorOwnerRepresentation)) {
            // Don't allow docking to the same object
            repr.setObjectEmphasized(true);
            repr.setMouseTransparent(true);
        }
        if (repr instanceof AnchorConstructionRepresentation && !mDockableAnchors.contains(repr.getModelObject())) {
            // Prevent undockable anchor representations from blocking mouse-over events
            repr.setMouseTransparent(true);
        }

        if (mUIElementFilter != null) {
            mUIElementFilter.configure(repr);
        }
    }

    protected boolean isAnchorVisible(AnchorConstructionRepresentation anchorRepr) {
        return mUIElementFilter.isUIElementVisible(anchorRepr);
    }

    @Override
    protected void unconfigureObject(Abstract2DRepresentation repr, boolean objectRemoved) {
        if (repr instanceof AbstractAnchoredObjectConstructionRepresentation aaocr) {
            aaocr.uninstallAnchorChoiceFeature();
        }
        repr.disableMouseOverSpot();
        if (!objectRemoved) {
            repr.setObjectEmphasized(false);
            repr.setMouseTransparent(false);
        }

        if (mUIElementFilter != null) {
            mUIElementFilter.unconfigure(repr);
        }
    }

    protected void createInteractionsTab() {
        mDockInteractionsPane.setSourceAnchor(mHandleAnchor);
        setInteractionsControl(new InteractionsControl(mDockInteractionsPane, getInteractionsTabTitle(), true));
        showTargetObjectFeedback(null);
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        mDockableAnchors = getDockableAnchors(); // Needed by configureObject()
        super.install(view);
        mFeedbackManager = new DockOperationVisualFeedbackManager(getView());
        updateAncillaryObjects();
        mActionsList.add(createCancelBehaviorAction(getCancelDockActionTitle()));
        mDockInteractionsPane.setDockToAnchorButtonText(getDockToTargetButtonTitle());
        createInteractionsTab();

        installDefaultEscapeBehaviorKeyHandler();
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
    }

    @Override
    public void uninstall() {
        mFeedbackManager.removeVisualObjects();
        mFeedbackManager = null;
        uninstallDefaultEscapeBehaviorKeyHandler();
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        super.uninstall();
    }
}