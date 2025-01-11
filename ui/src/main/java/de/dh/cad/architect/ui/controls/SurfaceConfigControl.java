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
package de.dh.cad.architect.ui.controls;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.MaterialsModel;
import de.dh.cad.architect.model.assets.RawMaterialModel;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.MaterialMappingConfiguration;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.AbstractSolid3DRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.SurfaceData;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.libraries.MaterialChooserDialog;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Shape3D;

public class SurfaceConfigControl extends BorderPane {
    private static final Logger log = LoggerFactory.getLogger(SurfaceConfigControl.class);
    public static final String FXML = "SurfaceConfigControl.fxml";

    protected final SurfaceData<? extends Shape3D> mSurface;
    protected de.dh.utils.Vector2D mSurfaceSize;
    protected final UiController mUiController;

    @FXML
    protected TextField mObjectTF;

    @FXML
    protected TextField mSurfaceTypeTF;

    @FXML
    protected Pane mMaterialConfigControlsParent;

    @FXML
    protected TextField mMaterialNameTF;

    @FXML
    protected Button mChoseMaterialButton;

    @FXML
    protected TextField mMaterialRefTF;

    @FXML
    protected RadioButton mStretchRadioButton;

    @FXML
    protected RadioButton mTileRadioButton;

    @FXML
    protected Pane mSizeControlsParent;

    @FXML
    protected Pane mValuesParent;

    @FXML
    protected Pane mOffsetParent;

    @FXML
    protected CheckBox mConfigureTileSizeCheckBox;

    @FXML
    protected CheckBox mMaintainAspectRatioCheckBox;

    @FXML
    protected Pane mTileSizeParent;

    protected LengthEditControl mXOffsetControl;
    protected LengthEditControl mYOffsetControl;
    protected LengthEditControl mXTileSizeControl;
    protected LengthEditControl mYTileSizeControl;
    protected RotationEditControl mRotationEditControl;

    protected Optional<Vector2D> mTileSizeFromMaterial = Optional.empty();
    protected AssetRefPath mMaterialRef = null;
    protected RawMaterialModel mMaterialModel = null;

    protected boolean mBlockUpdates = false;

    public SurfaceConfigControl(SurfaceData<? extends Shape3D> surface, UiController uiController) {
        mSurface = surface;
        mUiController = uiController;

        FXMLLoader fxmlLoader = new FXMLLoader(DivideWallLengthControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initialize();
    }

    protected void initialize() {
        String surfaceTypeId = mSurface.getSurfaceTypeId();
        AbstractSolid3DRepresentation ownerRepr = mSurface.getOwnerRepr();
        MaterialMappingConfiguration materialMapping = ownerRepr.getSurfaceMaterial(surfaceTypeId);
        mSurfaceSize = mSurface.getSurfaceSize().orElse(new de.dh.utils.Vector2D(
                CoordinateUtils.lengthToCoords(Length.ofM(3), null),
                CoordinateUtils.lengthToCoords(Length.ofM(3), null)));

        mObjectTF.setText(BaseObjectUIRepresentation.getObjName(ownerRepr.getModelObject()));
        mSurfaceTypeTF.setText(surfaceTypeId);
        AssetRefPath materialRef = materialMapping == null ? null : materialMapping.getMaterialRef();
        mChoseMaterialButton.setOnAction(event -> {
            AssetLoader assetLoader = mUiController.getAssetManager().buildAssetLoader();
            MaterialChooserDialog dialog = materialRef == null
                    ? MaterialChooserDialog.createWithProgressIndicator(assetLoader, Strings.THREE_D_SURFACE_CONFIG_BEHAVIOR_CHOOSE_MATERIAL_DIALOG_TITLE)
                    : MaterialChooserDialog.createWithProgressIndicator(assetLoader, Strings.THREE_D_SURFACE_CONFIG_BEHAVIOR_CHOOSE_MATERIAL_DIALOG_TITLE, mcd -> mcd.selectMaterial(materialRef));
            Optional<AssetRefPath> oRes = dialog.showAndWait();
            oRes.ifPresent(this::setMaterial);
        });

        Dimensions2D tileSize = materialMapping == null ? null : materialMapping.getTileSize();
        Length surfaceSizeX = CoordinateUtils.coordsToLength(mSurfaceSize.getX(), null);
        Length maxRangeX = Length.max(surfaceSizeX, tileSize == null ? Length.ofM(2) : tileSize.getX());
        Length surfaceSizeY = CoordinateUtils.coordsToLength(mSurfaceSize.getY(), null);
        Length maxRangeY = Length.max(surfaceSizeY, tileSize == null ? Length.ofM(2) : tileSize.getY());

        EventHandler<ActionEvent> updaterActionHandler = event -> updateMaterialMapping();
        ChangeListener<Object> updaterChangeListener = (observable, oldValue, newValue) -> updateMaterialMapping();
        mStretchRadioButton.setOnAction(updaterActionHandler);
        mTileRadioButton.setOnAction(updaterActionHandler);
        mSizeControlsParent.disableProperty().bind(mStretchRadioButton.selectedProperty());

        mXOffsetControl = new LengthEditControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mXOffsetControl.setMinValue(maxRangeX.negated());
        mXOffsetControl.setMaxValue(maxRangeX);
        mXOffsetControl.getValueProperty().addListener(updaterChangeListener);
        mOffsetParent.getChildren().addFirst(mXOffsetControl);
        GridPane.setColumnIndex(mXOffsetControl, 1);
        GridPane.setRowIndex(mXOffsetControl, 0);

        mYOffsetControl = new LengthEditControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mYOffsetControl.setMinValue(maxRangeY.negated());
        mYOffsetControl.setMaxValue(maxRangeY);
        mYOffsetControl.getValueProperty().addListener(updaterChangeListener);
        mOffsetParent.getChildren().addFirst(mYOffsetControl);
        GridPane.setColumnIndex(mYOffsetControl, 1);
        GridPane.setRowIndex(mYOffsetControl, 1);

        BooleanBinding dontConfigureTileSize = mConfigureTileSizeCheckBox.selectedProperty().not();
        mTileSizeParent.disableProperty().bind(dontConfigureTileSize);
        mMaintainAspectRatioCheckBox.disableProperty().bind(dontConfigureTileSize);
        mMaintainAspectRatioCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateMaterialMapping();
        });

        mXTileSizeControl = new LengthEditControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mXTileSizeControl.getValueProperty().addListener((observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                if (mMaintainAspectRatioCheckBox.isSelected()) {
                    mTileSizeFromMaterial.ifPresent(mts -> {
                        double ar = mts.getY().divideBy(mts.getX());
                        mYTileSizeControl.setValue(mXTileSizeControl.getValue().times(ar));
                    });
                }
            } finally {
                mBlockUpdates = false;
            }
            updateMaterialMapping();
        });
        mTileSizeParent.getChildren().addFirst(mXTileSizeControl);
        GridPane.setColumnIndex(mXTileSizeControl, 1);
        GridPane.setRowIndex(mXTileSizeControl, 0);

        mYTileSizeControl = new LengthEditControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mYTileSizeControl.disableProperty().bind(mMaintainAspectRatioCheckBox.selectedProperty());
        mYTileSizeControl.getValueProperty().addListener(updaterChangeListener);
        mTileSizeParent.getChildren().addFirst(mYTileSizeControl);
        GridPane.setColumnIndex(mYTileSizeControl, 1);
        GridPane.setRowIndex(mYTileSizeControl, 1);

        mRotationEditControl = new RotationEditControl();
        mValuesParent.getChildren().add(mRotationEditControl);
        GridPane.setColumnIndex(mRotationEditControl, 1);
        GridPane.setRowIndex(mRotationEditControl, 2);
        mRotationEditControl.getAngleProperty().addListener(updaterChangeListener);

        if (materialMapping != null) {
            setValues(materialMapping);
        }
    }

    protected void initForMaterial(AssetRefPath materialRef) {
        mMaterialRef = materialRef;
        boolean materialSet = materialRef != null && materialRef.getOMaterialName().isPresent();
        mMaterialConfigControlsParent.setDisable(!materialSet);
        mMaterialModel = null;
        if (materialSet) {
            try {
                AssetManager assetManager = mUiController.getAssetManager();
                MaterialSetDescriptor descriptor = assetManager.loadMaterialSetDescriptor(materialRef.withoutMaterialName());
                if (descriptor.getModel() instanceof MaterialsModel mm) {
                    mMaterialModel = mm.getMaterials().get(materialRef.getOMaterialName().get());
                }
            } catch (IOException e) {
                log.error("Error loading material <" + materialRef + ">", e);
            }
        }
        if (mMaterialModel != null) {
            mTileSizeFromMaterial = mMaterialModel.getTileSize();
            mMaterialNameTF.setText(materialRef.getOMaterialName().get());
            mMaterialRefTF.setText(materialRef.toPathString());
        } else {
            mMaterialNameTF.setText("-");
            mMaterialRefTF.setText("-");
            mTileSizeFromMaterial = Optional.empty();
        }
    }

    protected void setValues(MaterialMappingConfiguration materialMapping) {
        initForMaterial(materialMapping.getMaterialRef());
        mBlockUpdates = true;
        try {
            setLayoutMode(materialMapping.getLayoutMode());
            setTileSize(materialMapping.getTileSize());
            setOffset(materialMapping.getOffset());
            Double rotation = materialMapping.getMaterialRotationDeg();
            setRotation(rotation == null ? 0 : rotation);
        } finally {
            mBlockUpdates = false;
        }
        // Actually, when setting the values of a material mapping configuration, the UI should be in sync with those
        // values, but just to make sure the UI really reflects our values:
        updateMaterialMapping();
    }

    protected void setMaterial(AssetRefPath materialRef) {
        MaterialMappingConfiguration materialMappingConfiguration = mSurface.getMaterialMappingConfiguration();
        materialMappingConfiguration.setMaterialRef(materialRef);
        initForMaterial(materialRef);
        updateMaterialMapping();
    }

    public void setLayoutMode(MaterialMappingConfiguration.LayoutMode layoutMode) {
        switch (layoutMode) {
            case Stretch -> mStretchRadioButton.setSelected(true);
            case Tile -> mTileRadioButton.setSelected(true);
        }
    }

    public MaterialMappingConfiguration.LayoutMode getLayoutMode() {
        if (mStretchRadioButton.isSelected()) {
            return MaterialMappingConfiguration.LayoutMode.Stretch;
        } else {
            return MaterialMappingConfiguration.LayoutMode.Tile;
        }
    }

    public Dimensions2D getTileSize() {
        if (mConfigureTileSizeCheckBox.isSelected()) {
            return new Dimensions2D(mXTileSizeControl.getValue(), mYTileSizeControl.getValue());
        } else {
            return null;
        }
    }

    public void setTileSize(Dimensions2D value) {
        if (value == null) {
            mConfigureTileSizeCheckBox.setSelected(false);
            Vector2D size = mMaterialModel.getTileSize().orElse(
                    new Vector2D(
                            CoordinateUtils.coordsToLength(mSurfaceSize.getX(), null),
                            CoordinateUtils.coordsToLength(mSurfaceSize.getY(), null)
                    ));
            mXTileSizeControl.setValue(size.getX());
            mYTileSizeControl.setValue(size.getY());
        } else {
            mConfigureTileSizeCheckBox.setSelected(true);
            mXTileSizeControl.setValue(value.getX());
            mYTileSizeControl.setValue(value.getY());
        }
    }

    public Vector2D getOffset() {
        return new Vector2D(mXOffsetControl.getValue(), mYOffsetControl.getValue());
    }

    public void setOffset(Vector2D value) {
        if (value == null) {
            mXOffsetControl.setValue(Length.ZERO);
            mYOffsetControl.setValue(Length.ZERO);
        } else {
            mXOffsetControl.setValue(value.getX());
            mYOffsetControl.setValue(value.getY());
        }
    }

    public double getRotation() {
        return mRotationEditControl.getAngle();
    }

    public void setRotation(double angle) {
        mRotationEditControl.setAngle(angle);
    }

    protected void updateMaterialMapping() {
        if (mBlockUpdates) {
            return;
        }
        MaterialMappingConfiguration mmc = new MaterialMappingConfiguration();
        mmc.setMaterialRef(mMaterialRef);
        mmc.setLayoutMode(getLayoutMode());
        mmc.setTileSize(getTileSize());
        mmc.setOffset(getOffset());
        double rotation = getRotation();
        mmc.setMaterialRotationDeg(rotation == 0 ? null : rotation);
        mSurface.setMaterialMappingConfiguration(mmc);
    }

    public SurfaceData<? extends Shape3D> getSurface() {
        return mSurface;
    }
}
