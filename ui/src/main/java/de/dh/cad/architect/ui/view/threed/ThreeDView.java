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
package de.dh.cad.architect.ui.view.threed;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.persistence.CameraPosition;
import de.dh.cad.architect.ui.persistence.ThreeDViewState;
import de.dh.cad.architect.ui.persistence.ViewState;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.NullMode;
import de.dh.cad.architect.ui.view.threed.behaviors.Abstract3DViewBehavior;
import de.dh.utils.fx.ImageUtils;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.transform.Translate;

public class ThreeDView extends Abstract3DView {
    private static final Logger log = LoggerFactory.getLogger(ThreeDView.class);

    protected static final String ICON_SELECTION_MODE_RESOURCE = "SelectionMode.png";
    protected static final String ICON_PAINTER_MODE_RESOURCE = "PainterMode.png";

    protected static final int TOOL_BAR_ICON_SIZE = 32;

    protected ThreeDViewState mSavedViewState;
    protected AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> mThreeDMode = new NullMode<>();

    protected final SelectionMode mSelectionMode;
    protected final PainterMode mPainterMode;

    protected ToggleButton mSelectionModeButton = null;
    protected ToggleButton mPainterModeButton = null;

    public ThreeDView(UiController uiController) {
        super(uiController);
        initializeViewState();

        mSelectionMode = new SelectionMode(mUiController);
        mPainterMode = new PainterMode(mUiController);
    }

    @Override
    protected void initializeFromPlan() {
        Plan plan = getPlan();

        addUIRepresentations(plan.getDimensionings().values());
        addUIRepresentations(plan.getAnchors().values());
        addUIRepresentations(plan.getFloors().values());
        addUIRepresentations(plan.getWalls().values());
        addUIRepresentations(plan.getCeilings().values());
        addUIRepresentations(plan.getCoverings().values());
        addUIRepresentations(plan.getSupportObjects().values());
        for (Abstract3DRepresentation repr : mRepresentationsById.values()) {
            repr.updateToModel();
        }
    }

    protected void initializeViewState() {
        CameraPosition currentCameraPosition = new CameraPosition();

        currentCameraPosition.setAngleX(-60);
        currentCameraPosition.setCameraTranslateZ(-1500);
        currentCameraPosition.setFieldOfView(60);
        currentCameraPosition.setCameraNearClip(mMinNearClip);
        mSavedViewState = new ThreeDViewState();
        mSavedViewState.setCurrentCameraPosition(currentCameraPosition);
    }

    @Override
    protected void initialize() {
        mSelectionModeButton = new ToggleButton();
        mSelectionModeButton.setGraphic(ImageUtils.loadSquareIcon(ThreeDView.class, ICON_SELECTION_MODE_RESOURCE, TOOL_BAR_ICON_SIZE));
        mSelectionModeButton.setTooltip(new Tooltip(Strings.THREE_D_SELECTION_MODE_ACTION_TOOLTIP));
        mSelectionModeButton.setOnAction(action -> {
            setThreeDMode(mSelectionMode);
        });

        mPainterModeButton = new ToggleButton();
        mPainterModeButton.setGraphic(ImageUtils.loadSquareIcon(ThreeDView.class, ICON_PAINTER_MODE_RESOURCE, TOOL_BAR_ICON_SIZE));
        mPainterModeButton.setTooltip(new Tooltip(Strings.PAINTER_MODE_ACTION_TOOLTIP));
        mPainterModeButton.setOnAction(action -> {
            setThreeDMode(mPainterMode);
        });

        super.initialize();
        setToolBarContributionItems(mSelectionModeButton, mPainterModeButton);

        updateViewToViewState();

        setThreeDMode(mSelectionMode);
    }

    @Override
    protected void uninitialize() {
        setBehavior(null);

        takeViewStateFromView();

        mSelectionModeButton = null;
        mPainterModeButton = null;

        super.uninitialize();
    }

    public AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> getThreeDMode() {
        return mThreeDMode;
    }

    public void setThreeDMode(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> value) {
        AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> currentMode = mThreeDMode;
        if (currentMode != null) {
            currentMode.uninstall();
        }
        if (value != null) {
            value.install(this);
        }
        mThreeDMode = value;

        mSelectionModeButton.setSelected(false);
        mPainterModeButton.setSelected(false);
        if (value instanceof SelectionMode) {
            mSelectionModeButton.setSelected(true);
        } else if (value instanceof PainterMode) {
            mPainterModeButton.setSelected(true);
        }
    }

    @Override
    public Class<? extends ViewState> getViewStateClass() {
        return ThreeDViewState.class;
    }

    @Override
    public Optional<ThreeDViewState> getViewState() {
        takeViewStateFromView();
        mSavedViewState.getCurrentCameraPosition().cleanup();
        for (CameraPosition position : mSavedViewState.getNamedCameraPositions().values()) {
            position.cleanup();
        }
        return Optional.of(mSavedViewState);
    }

    @Override
    public void setViewState(ViewState viewState) {
        mSavedViewState = (ThreeDViewState) viewState;
        updateViewToViewState();
    }

    protected void takeViewStateFromView() {
        if (!isAlive()) {
            return;
        }
        CameraPosition currentCameraPosition = getCurrentCameraPosition();
        mSavedViewState.setCurrentCameraPosition(currentCameraPosition);
    }

    protected void updateViewToViewState() {
        if (!isAlive()) {
            return;
        }
        CameraPosition currentCameraPosition = mSavedViewState.getCurrentCameraPosition();
        setCameraPosition(currentCameraPosition);
    }

    public CameraPosition getCurrentCameraPosition() {
        CameraPosition result = new CameraPosition();
        result.setCameraTranslateZ(mCamera.getTranslateZ());
        result.setCameraNearClip(mCamera.getNearClip());
        result.setFieldOfView(mCamera.getFieldOfView());
        result.setAngleX(mTransformedRoot.getXRotate().getAngle());
        result.setAngleZ(mTransformedRoot.getZRotate().getAngle());
        Translate translate = mTransformedRoot.getTranslate();
        result.setX(translate.getX());
        result.setY(translate.getY());
        result.setZ(translate.getZ());
        return result;
    }

    public void setCameraPosition(CameraPosition cameraPosition) {
        mCamera.setTranslateZ(cameraPosition.getCameraTranslateZ());
        mCamera.setNearClip(Math.max(cameraPosition.getCameraNearClip(), mMinNearClip));
        mCamera.setFieldOfView(cameraPosition.getFieldOfView());
        mTransformedRoot.getXRotate().setAngle(cameraPosition.getAngleX());
        mTransformedRoot.getZRotate().setAngle(cameraPosition.getAngleZ());
        Translate translate = mTransformedRoot.getTranslate();
        translate.setX(cameraPosition.getX());
        translate.setY(cameraPosition.getY());
        translate.setZ(cameraPosition.getZ());
    }

    public void setNamedCameraPositions(Map<String, CameraPosition> newPositions) {
        mSavedViewState.setNamedCameraPositions(newPositions);
    }

    public void saveCameraPosition(String positionName) {
        mSavedViewState.getNamedCameraPositions().put(positionName, getCurrentCameraPosition());
    }

    public void loadCameraPosition(String positionName) {
        CameraPosition cameraPosition = mSavedViewState.getNamedCameraPositions().get(positionName);
        if (cameraPosition == null) {
            log.warn("Requested camera position with name '" + positionName + "' does not exist");
            return;
        }
        setCameraPosition(cameraPosition);
    }

    public Map<String, CameraPosition> getNamedCameraPositions() {
        return mSavedViewState.getNamedCameraPositions();
    }

    @Override
    public String getTitle() {
        return Strings.THREE_D_PLAN_VIEW_TITLE;
    }

    @Override
    public Abstract3DViewBehavior getBehavior() {
        return (Abstract3DViewBehavior) super.getBehavior();
    }
}
