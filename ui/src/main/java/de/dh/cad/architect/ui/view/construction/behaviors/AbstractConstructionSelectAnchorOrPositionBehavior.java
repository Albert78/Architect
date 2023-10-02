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

import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.AbstractAnchoredObjectConstructionRepresentation;
import de.dh.cad.architect.ui.objects.AnchorConstructionRepresentation;
import de.dh.cad.architect.ui.utils.Cursors;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.UiPlanPosition;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/**
 * First part of the two-part behavior to create walls. Second part is {@link GroundPlanAddWallBehavior}.
 */
public abstract class AbstractConstructionSelectAnchorOrPositionBehavior extends AbstractConstructionBehavior {
    protected final EventHandler<? super MouseEvent> ROOT_MOUSE_CLICK_HANDLER = event -> {
        MouseButton button = event.getButton();
        if (MouseButton.PRIMARY.equals(button)) {
            if (event.isSecondaryButtonDown()
                    || event.isMiddleButtonDown()) {
                // We only react to primary button clicks without other buttons
                return;
            }
            event.consume();
            UiPlanPosition uiPos = getView().getPlanPositionFromScene(event.getSceneX(), event.getSceneY());
            Position2D pos = tryCorrectPosition(uiPos.getModelPosition(), event.isShiftDown(), event.isAltDown(), event.isControlDown());
            triggerPoint(pos);
        } else if (MouseButton.SECONDARY.equals(button)) {
            event.consume();
            secondaryButtonClicked();
        } else if (MouseButton.MIDDLE.equals(button)) {
            event.consume();
            middleButtonClicked();
        }
    };
    protected final EventHandler<? super MouseEvent> ROOT_MOUSE_MOVE_HANDLER = event -> {
        UiPlanPosition uiPos = getView().getPlanPositionFromScene(event.getSceneX(), event.getSceneY());
        Position2D pos = tryCorrectPosition(uiPos.getModelPosition(), event.isShiftDown(), event.isAltDown(), event.isControlDown());
        showPositionFeedback(pos);
    };
    protected final EventHandler<? super MouseEvent> ROOT_MOUSE_EXITED_HANDLER = event -> {
        setDefaultUserHint();
    };

    public AbstractConstructionSelectAnchorOrPositionBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
        initializeActions();
        initializeUiElementFilter();
        setDefaultUserHint();
    }

    protected abstract void initializeActions();
    protected abstract void initializeUiElementFilter();

    @Override
    public abstract String getTitle();

    protected abstract void showPositionFeedback(Position2D pos);
    protected abstract void showAnchorFeedback(AnchorConstructionRepresentation anchorRepr);

    protected abstract void triggerPoint(Position2D pos);
    protected abstract void triggerAnchor(AnchorConstructionRepresentation anchorRepr);

    protected Position2D tryCorrectPosition(Position2D pos, boolean isShiftDown, boolean isAltDown, boolean isControlDown) {
        return isControlDown ? pos : calculateCorrectedPosition(pos);
    }

    protected Position2D calculateCorrectedPosition(Position2D pos) {
        ConstructionView constructionView = (ConstructionView) getParentMode().getView();
        pos = constructionView.snapToGuideLines(pos);
        return pos;
    }

    protected abstract boolean isPossibleDockTargetAnchor(Anchor anchor);
    protected abstract AbstractAnchoredObjectConstructionRepresentation getDockTargetElementIfSupported(Abstract2DRepresentation repr);

    protected void secondaryButtonClicked() {
        // Can be overridden, if needed
    }

    protected void middleButtonClicked() {
        // Can be overridden, if needed
    }

    @Override
    protected void configureObject(Abstract2DRepresentation repr) {
        // Skip default event handlers like click event and default spot event; we have our own click handlers

        AbstractAnchoredObjectConstructionRepresentation aaocr = getDockTargetElementIfSupported(repr);
        if (aaocr != null) {
            aaocr.enableMouseOverSpot();

            aaocr.installAnchorChoiceFeature(anchor -> {
                return isPossibleDockTargetAnchor(anchor);
            }, this::showAnchorFeedback, this::triggerAnchor);
        }

        if (mUIElementFilter != null) {
            mUIElementFilter.configure(repr);
        }
    }

    @Override
    protected void unconfigureObject(Abstract2DRepresentation repr, boolean objectRemoved) {
        AbstractAnchoredObjectConstructionRepresentation aaocr = getDockTargetElementIfSupported(repr);
        if (aaocr != null) {
            aaocr.uninstallAnchorChoiceFeature();
        }
        repr.disableMouseOverSpot();

        if (mUIElementFilter != null) {
            mUIElementFilter.unconfigure(repr);
        }
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);
        ConstructionView constructionView = getView();
        constructionView.enableRulerCursorMarker();
        constructionView.setCursor(Cursors.createCursorCrossHair());
        Pane rootLayer = constructionView.getCenterPane();
        rootLayer.setOnMouseClicked(ROOT_MOUSE_CLICK_HANDLER);
        rootLayer.setOnMouseMoved(ROOT_MOUSE_MOVE_HANDLER);
        rootLayer.setOnMouseExited(ROOT_MOUSE_EXITED_HANDLER);
        installDefaultEscapeBehaviorKeyHandler();
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
    }

    @Override
    public void uninstall() {
        ConstructionView constructionView = getView();
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        uninstallDefaultEscapeBehaviorKeyHandler();
        Pane rootLayer = constructionView.getCenterPane();
        rootLayer.setOnMouseExited(null);
        rootLayer.setOnMouseMoved(null);
        rootLayer.setOnMouseClicked(null);
        constructionView.setCursor(null);
        constructionView.disableRulerCursorMarker();
        super.uninstall();
    }
}
