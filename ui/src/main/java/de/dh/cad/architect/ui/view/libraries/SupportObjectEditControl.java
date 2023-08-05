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
package de.dh.cad.architect.ui.view.libraries;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.LengthUnit;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetLoader.ThreeDResourceImportMode;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.assets.AssetManagerConfiguration;
import de.dh.cad.architect.ui.assets.ThreeDObject;
import de.dh.cad.architect.ui.controls.LengthControl;
import de.dh.cad.architect.ui.utils.CoordinateUtils;
import de.dh.cad.architect.ui.view.libraries.TakeSnapshotDialog.IImageSaveEvents;
import de.dh.cad.architect.utils.vfs.PlainFileSystemResourceLocator;
import de.dh.utils.fx.FxUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;

public class SupportObjectEditControl extends AbstractAssetEditControl implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(SupportObjectEditControl.class);

    public static final String FXML = "SupportObjectEditControl.fxml";
    public static final int MIN_IMAGE_SIZE = 100;

    @FXML
    protected TextField mAssetCollectionNameTextField;

    @FXML
    protected TextField mIdTextField;

    @FXML
    protected TextField mAssetRefPathTextField;

    @FXML
    protected TextField mNameTextField;

    @FXML
    protected ComboBox<String> mCategoryComboBox;

    @FXML
    protected ComboBox<String> mTypeComboBox;

    @FXML
    protected TextArea mDescriptionTextArea;

    @FXML
    protected ImageView mIconImageView;

    @FXML
    protected Button mChooseIconImageButton;

    @FXML
    protected VBox mSizeControlsVBox;

    protected LengthControl mWidthControl;
    protected LengthControl mHeightControl;
    protected LengthControl mDepthControl;
    protected LengthControl mElevationControl;

    @FXML
    protected ImageView mPlanViewImageView;

    @FXML
    protected Button mChoosePlanViewImageButton;

    @FXML
    protected BorderPane mThreeDResourcePane;

    @FXML
    protected Button mChooseThreeDObjectButton;

    @FXML
    protected Button mTakeSnapshotButton;

    protected ThreeDObjectViewControl mThreeDObjectView;
    protected Node mThreeDHelperObject = null; // For calculating sizes etc.

    public SupportObjectEditControl(AssetManager assetManager) {
        super(assetManager);

        FXMLLoader fxmlLoader = new FXMLLoader(SupportObjectEditControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mCategoryComboBox.setItems(FXCollections.observableArrayList(mAssetManager.getSupportObjectCategories()));
        FxUtils.setImmediateCommitText(mCategoryComboBox);
        mTypeComboBox.setItems(FXCollections.observableArrayList(mAssetManager.getSupportObjectTypes()));
        FxUtils.setImmediateCommitText(mTypeComboBox);
        mChooseIconImageButton.setOnAction(event -> {
            Path path = openIconImageDialog();
            if (path != null) {
                importIconImage(path);
            }
        });

        mChoosePlanViewImageButton.setOnAction(event -> {
            Path path = openPlanViewImageDialog();
            if (path != null) {
                importPlanViewImage(path);
            }
        });

        mThreeDObjectView = new ThreeDObjectViewControl();
        mThreeDObjectView.setPrefWidth(250);
        mThreeDObjectView.setPrefHeight(250);
        mThreeDObjectView.setCoordinateSystemVisible(true);
        mThreeDResourcePane.setCenter(mThreeDObjectView);
        ObservableList<Node> boxChildren = mSizeControlsVBox.getChildren();
        boxChildren.add(new Label(Strings.SUPPORT_OBJECT_CONTROL_DEFAULT_SIZE_TITLE));
        final int CONTROL_WIDTH = 150;
        final Insets margin = new Insets(5, 0, 0, 0);
        Label l = new Label(Strings.SUPPORT_OBJECT_CONTROL_WIDTH_X_LABEL);
        boxChildren.add(l);
        VBox.setMargin(l, margin);
        mWidthControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mWidthControl.setPrefWidth(CONTROL_WIDTH);
        mWidthControl.setMinWidth(CONTROL_WIDTH);
        mWidthControl.setMaxWidth(CONTROL_WIDTH);
        boxChildren.add(mWidthControl);
        l = new Label(Strings.SUPPORT_OBJECT_CONTROL_HEIGHT_Y_LABEL);
        boxChildren.add(l);
        VBox.setMargin(l, margin);
        mHeightControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mHeightControl.setPrefWidth(CONTROL_WIDTH);
        mHeightControl.setMinWidth(CONTROL_WIDTH);
        mHeightControl.setMaxWidth(CONTROL_WIDTH);
        boxChildren.add(mHeightControl);
        l = new Label(Strings.SUPPORT_OBJECT_CONTROL_DEPTH_Z_LABEL);
        boxChildren.add(l);
        VBox.setMargin(l, margin);
        mDepthControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mDepthControl.setPrefWidth(CONTROL_WIDTH);
        mDepthControl.setMinWidth(CONTROL_WIDTH);
        mDepthControl.setMaxWidth(CONTROL_WIDTH);
        boxChildren.add(mDepthControl);
        l = new Label(Strings.SUPPORT_OBJECT_CONTROL_ELEVATION_Y_LABEL);
        boxChildren.add(l);
        VBox.setMargin(l, margin);
        mElevationControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mElevationControl.setPrefWidth(CONTROL_WIDTH);
        mElevationControl.setMinWidth(CONTROL_WIDTH);
        mElevationControl.setMaxWidth(CONTROL_WIDTH);
        boxChildren.add(mElevationControl);

        mChooseThreeDObjectButton.setOnAction(event -> {
            Path path = openObject3DResourceDialog();
            if (path != null) {
                importThreeDObject(path, Optional.empty());
            }
        });
        mTakeSnapshotButton.setOnAction(event -> {
            TakeSnapshotDialog dialog = new TakeSnapshotDialog(new IImageSaveEvents() {
                @Override
                public void onSaveIconImage(Image img) {
                    importIconImage(img, AssetManager.ICON_IMAGE_DEFAULT_BASE_NAME + "." + AssetManager.STORE_IMAGE_EXTENSION);
                }

                @Override
                public void onSavePlanViewImage(Image img) {
                    importPlanViewImage(img, AssetManager.PLAN_VIEW_IMAGE_DEFAULT_BASE_NAME + "." + AssetManager.STORE_IMAGE_EXTENSION);
                }
            });
            dialog.showFor(load3DObjectView(),
                (int) Math.max(CoordinateUtils.lengthToCoords(mWidthControl.getLength()), MIN_IMAGE_SIZE),
                (int) Math.max(CoordinateUtils.lengthToCoords(mDepthControl.getLength()), MIN_IMAGE_SIZE), getStage());
        });
    }

    @Override
    public SupportObjectDescriptor getDescriptor() {
        return (SupportObjectDescriptor) super.getDescriptor();
    }

    protected Image loadPlanViewImage() {
        Image image = null;
        SupportObjectDescriptor descriptor = getDescriptor();
        try {
            image = mAssetLoader.loadSupportObjectPlanViewImage(descriptor);
        } catch (IOException e) {
            log.error("Plan view image of support object <" + descriptor + "> could not be loaded", e);
        }
        if (image == null) {
            image = AssetLoader.loadBrokenImageBig();
        }
        return image;
    }

    protected Node load3DObjectView() {
        Group result = new Group();
        ObservableList<Node> children = result.getChildren();
        SupportObjectDescriptor descriptor = getDescriptor();
        try {
            ThreeDObject threeDObject = mAssetLoader.loadSupportObject3DResource(descriptor, Optional.empty());
            children.addAll(threeDObject.getSurfaceMeshViews());
            Bounds boundsInParent = result.getBoundsInParent();
            result.getTransforms().add(0, new Translate(-boundsInParent.getCenterX(), -boundsInParent.getCenterY(), -boundsInParent.getCenterZ()));
        } catch (IOException e) {
            log.error("3D resource of support object <" + descriptor + "> could not be loaded", e);
        }
        if (children.isEmpty()) {
            children.addAll(AssetLoader.loadBroken3DResource().getSurfaceMeshViews());
        }
        return result;
    }

    @Override
    protected void updateIconImage() {
        Image image = loadIconImage();
        mIconImageView.setImage(image);
    }

    protected void importPlanViewImage(Path path) {
        try {
            mAssetLoader.importSupportObjectPlanViewImage(getDescriptor(), new PlainFileSystemResourceLocator(path),
                Optional.of(AssetManager.PLAN_VIEW_IMAGE_DEFAULT_BASE_NAME + "." + AssetManager.STORE_IMAGE_EXTENSION));
            updatePlanViewImage();
        } catch (IOException e) {
            showErrorImportDialog(Strings.LIBRARY_MANAGER_ERROR_IMPORTING_PLAN_VIEW_IMAGE_TEXT, e);
        }
    }

    protected void importPlanViewImage(Image image, String imageName) {
        try {
            mAssetLoader.importSupportObjectPlanViewImage(getDescriptor(), image, imageName);
            updatePlanViewImage();
        } catch (IOException e) {
            showErrorImportDialog(Strings.LIBRARY_MANAGER_ERROR_IMPORTING_PLAN_VIEW_IMAGE_TEXT, e);
        }
    }

    protected void updatePlanViewImage() {
        Image image = loadPlanViewImage();
        mPlanViewImageView.setImage(image);
    }

    protected void importThreeDObject(Path path, Optional<float[][]> modelRotation) {
        try {
            mAssetLoader.importSupportObject3DViewObjResource(getDescriptor(), new PlainFileSystemResourceLocator(path), modelRotation,
                ThreeDResourceImportMode.Directory, Optional.empty());
            updateThreeDObject();

            Bounds layoutBounds = mThreeDHelperObject.getLayoutBounds();
            mWidthControl.setLength(CoordinateUtils.coordsToLength(layoutBounds.getWidth()), null);
            mHeightControl.setLength(CoordinateUtils.coordsToLength(layoutBounds.getHeight()), null);
            mDepthControl.setLength(CoordinateUtils.coordsToLength(layoutBounds.getDepth()), null);
            mElevationControl.setLength(Length.ZERO, null);
        } catch (IOException e) {
            showErrorImportDialog(Strings.LIBRARY_MANAGER_ERROR_IMPORTING_3D_OBJECT_TEXT, e);
        }
    }

    protected void updateThreeDObject() {
        Node node = load3DObjectView();
        mThreeDObjectView.setObjView(node, 200);
        mThreeDHelperObject = load3DObjectView();
    }

    public void initializeValues(SupportObjectDescriptor descriptor) {
        mDescriptor = descriptor;
        AssetRefPath assetRefPath = descriptor.getSelfRef();
        String anchorName = assetRefPath.getAnchor().toString();

        mAssetCollectionNameTextField.setText(anchorName);
        mIdTextField.setText(descriptor.getId());
        mAssetRefPathTextField.setText(assetRefPath.toPathString());
        mNameTextField.setText(StringUtils.trimToEmpty(descriptor.getName()));
        mCategoryComboBox.getSelectionModel().select(descriptor.getCategory());
        mTypeComboBox.getSelectionModel().select(descriptor.getType());
        mDescriptionTextArea.setText(StringUtils.trimToEmpty(descriptor.getDescription()));

        mWidthControl.setLength(lengthOrDefault(descriptor.getWidth(), Length.ofCM(100)), LengthUnit.CM);
        mHeightControl.setLength(lengthOrDefault(descriptor.getHeight(), Length.ofCM(100)), LengthUnit.CM);
        mDepthControl.setLength(lengthOrDefault(descriptor.getDepth(), Length.ofCM(100)), LengthUnit.CM);
        mElevationControl.setLength(lengthOrDefault(descriptor.getElevation(), Length.ZERO), LengthUnit.CM);

        updateIconImage();
        updatePlanViewImage();
        updateThreeDObject();
    }

    public void saveValues(SupportObjectDescriptor descriptor) {
        try {
            descriptor.setName(StringUtils.trimToNull(mNameTextField.getText()));
            descriptor.setCategory(StringUtils.trimToNull(mCategoryComboBox.getValue()));
            descriptor.setType(StringUtils.trimToNull(mTypeComboBox.getValue()));
            descriptor.setDescription(StringUtils.trimToNull(mDescriptionTextArea.getText()));
            descriptor.setWidth(mWidthControl.getLength());
            descriptor.setHeight(mHeightControl.getLength());
            descriptor.setDepth(mDepthControl.getLength());
            descriptor.setElevation(mElevationControl.getLength());

            mAssetManager.saveAssetDescriptor(descriptor);
        } catch (IOException e) {
            log.error("Error while saving support object '" + descriptor.getId() + "'", e);
        }
    }

    protected Path openPlanViewImageDialog() {
        return openImageDialog(Strings.SELECT_PLAN_VIEW_IMAGE_DIALOG_TITLE);
    }

    protected Path openObject3DResourceDialog() {
        FileChooser fc = new FileChooser();
        fc.setTitle(Strings.SELECT_OBJECT_3D_RESOURCE_DIALOG_TITLE);
        AssetManagerConfiguration configuration = mAssetManager.getConfiguration();
        Optional<Path> oLast3DResourcePath = configuration.getLast3DResourcePath();
        oLast3DResourcePath.ifPresent(path -> fc.setInitialDirectory(path.toFile()));
        fc.getExtensionFilters().addAll(getImageExtensionFilters());
        File resource3DFile = fc.showOpenDialog(getStage());
        if (resource3DFile == null) {
            return null;
        }
        Path resource3DFilePath = resource3DFile.toPath();
        Path resource3DDirectoryPath = resource3DFilePath.getParent();
        configuration.setLast3DResourcePath(resource3DDirectoryPath);
        return resource3DFilePath;
    }
}
