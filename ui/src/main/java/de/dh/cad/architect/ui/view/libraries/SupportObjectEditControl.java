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
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.fx.nodes.objviewer.CoordinateSystemConfiguration;
import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.fx.nodes.objviewer.VirtualFloorNode;
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
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Box;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;

// Coordinate system of objects stored here is in global coordinates, i.e. before rotation to the visible
// JavaFX representation -> Y/Z exchanged
public class SupportObjectEditControl extends AbstractAssetEditControl implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(SupportObjectEditControl.class);

    public static final String FXML = "SupportObjectEditControl.fxml";
    public static final int MIN_IMAGE_SIZE = 100;
    public static final Length MAX_NORMAL_OBJECT_SIZE = Length.ofM(4);

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

    @FXML
    protected VBox mRotationControlsVBox;

    protected Node mThreeDObjectRaw = new Box();
    protected Node mThreeDObjectRot = new Box();
    protected Node mThreeDObjectScaled = new Box();

    protected LengthControl mWidthControl;
    protected LengthControl mHeightControl;
    protected LengthControl mDepthControl;
    protected LengthControl mElevationControl;
    protected CheckBox mShowVirtualFloorCheckBox;
    protected Affine mObjectRotation = new Affine();

    @FXML
    protected Button mResetValuesButton;

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
    protected VirtualFloorNode mVirtualFloor = new VirtualFloorNode();
    protected boolean mPreventUpdate = false;

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

        mThreeDObjectView = new ThreeDObjectViewControl(CoordinateSystemConfiguration.architect());
        mThreeDObjectView.setPrefWidth(350);
        mThreeDObjectView.setCoordinateSystemVisible(true);
        mThreeDResourcePane.setCenter(mThreeDObjectView);
        createSizeUiControls(mSizeControlsVBox);
        createRotationUiControls(mRotationControlsVBox, true);

        mResetValuesButton.setOnAction(event -> resetObjectValues());

        mChooseThreeDObjectButton.setOnAction(event -> {
            Path path = openObject3DResourceDialog();
            if (path != null) {
                importThreeDObject(path, Optional.empty());
            }
        });
        mTakeSnapshotButton.setOnAction(event -> {
            saveValues();
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
            SupportObjectDescriptor descriptor = getDescriptor();
            dialog.showFor(buildSnapshot3DObject(),
                (int) Math.max(CoordinateUtils.lengthToCoords(descriptor.getWidth(), null), MIN_IMAGE_SIZE),
                (int) Math.max(CoordinateUtils.lengthToCoords(descriptor.getDepth(), null), MIN_IMAGE_SIZE), getStage());
        });
    }

    protected void createSizeUiControls(VBox parent) {
        ChangeListener<? super Length> updateThreeDChangeListener = (observable, oldValue, newValue) -> {
            if (mPreventUpdate) {
                return;
            }
            setThreeDObject(mThreeDObjectRaw);
        };

        ObservableList<Node> boxChildren = parent.getChildren();
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
        mWidthControl.lengthProperty().addListener(updateThreeDChangeListener);
        boxChildren.add(mWidthControl);
        l = new Label(Strings.SUPPORT_OBJECT_CONTROL_DEPTH_Y_LABEL);
        boxChildren.add(l);
        VBox.setMargin(l, margin);
        mDepthControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mDepthControl.setPrefWidth(CONTROL_WIDTH);
        mDepthControl.setMinWidth(CONTROL_WIDTH);
        mDepthControl.setMaxWidth(CONTROL_WIDTH);
        mDepthControl.lengthProperty().addListener(updateThreeDChangeListener);
        boxChildren.add(mDepthControl);
        l = new Label(Strings.SUPPORT_OBJECT_CONTROL_HEIGHT_Z_LABEL);
        boxChildren.add(l);
        VBox.setMargin(l, margin);
        mHeightControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mHeightControl.setPrefWidth(CONTROL_WIDTH);
        mHeightControl.setMinWidth(CONTROL_WIDTH);
        mHeightControl.setMaxWidth(CONTROL_WIDTH);
        mHeightControl.lengthProperty().addListener(updateThreeDChangeListener);
        boxChildren.add(mHeightControl);
        l = new Label(Strings.SUPPORT_OBJECT_CONTROL_ELEVATION_Y_LABEL);
        boxChildren.add(l);
        VBox.setMargin(l, margin);
        mElevationControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mElevationControl.setPrefWidth(CONTROL_WIDTH);
        mElevationControl.setMinWidth(CONTROL_WIDTH);
        mElevationControl.setMaxWidth(CONTROL_WIDTH);
        mElevationControl.lengthProperty().addListener(updateThreeDChangeListener);
        boxChildren.add(mElevationControl);

        mShowVirtualFloorCheckBox = new CheckBox(Strings.SUPPORT_OBJECT_CONTROL_SHOW_VIRTUAL_FLOOR);
        mShowVirtualFloorCheckBox.setSelected(true);
        VBox.setMargin(mShowVirtualFloorCheckBox, margin);
        mShowVirtualFloorCheckBox.setOnAction(event -> {
            updateVirtualFloor();
        });
        boxChildren.add(mShowVirtualFloorCheckBox);
    }

    protected void createRotationUiGroup(ObservableList<Node> parentBoxChildren, Point3D axis, String groupLabel, String rotateUpLabel, String rotateDownLabel) {
        final int CONTROL_WIDTH = 150;
        final int ROTATE_BUTTON_WIDTH = 60;

        final Insets vMargin = new Insets(5, 0, 0, 0);
        Label l = new Label(groupLabel);
        parentBoxChildren.add(l);
        VBox.setMargin(l, vMargin);
        GridPane rotPane = new GridPane();
        rotPane.setPrefWidth(CONTROL_WIDTH);
        rotPane.setMinWidth(CONTROL_WIDTH);
        rotPane.setMaxWidth(CONTROL_WIDTH);
        parentBoxChildren.add(rotPane);
        Button rotateUp = new Button(rotateUpLabel);
        rotateUp.setPrefWidth(ROTATE_BUTTON_WIDTH);
        rotateUp.setOnAction(event -> addObjectRotation(90, axis));
        rotPane.add(rotateUp, 0, 0);
        rotPane.setHgap(5);
        Button rotateDown = new Button(rotateDownLabel);
        rotateDown.setPrefWidth(ROTATE_BUTTON_WIDTH);
        rotateDown.setOnAction(event -> addObjectRotation(-90, axis));
        rotPane.add(rotateDown, 1, 0);
    }

    protected void createRotationUiControls(VBox parent, boolean showAxisCheckboxInitiallySelected) {
        ObservableList<Node> parentBoxChildren = parent.getChildren();
        parentBoxChildren.add(new Label(Strings.SUPPORT_OBJECT_CONTROL_ROTATION_TITLE));
        final Insets margin = new Insets(5, 0, 5, 0);
        CheckBox showAxisCheckBox = new CheckBox(Strings.SUPPORT_OBJECT_CONTROL_SHOW_AXIS);
        showAxisCheckBox.setSelected(showAxisCheckboxInitiallySelected);
        VBox.setMargin(showAxisCheckBox, margin);
        showAxisCheckBox.setOnAction(event -> {
            mThreeDObjectView.setCoordinateSystemVisible(showAxisCheckBox.isSelected());
        });
        parentBoxChildren.add(showAxisCheckBox);

        createRotationUiGroup(parentBoxChildren, Rotate.X_AXIS, Strings.SUPPORT_OBJECT_CONTROL_ROT_X_LABEL, Strings.SUPPORT_OBJECT_CONTROL_ROT_X_UP_BUTTON, Strings.SUPPORT_OBJECT_CONTROL_ROT_X_DOWN_BUTTON);
        createRotationUiGroup(parentBoxChildren, Rotate.Y_AXIS, Strings.SUPPORT_OBJECT_CONTROL_ROT_Y_LABEL, Strings.SUPPORT_OBJECT_CONTROL_ROT_Y_UP_BUTTON, Strings.SUPPORT_OBJECT_CONTROL_ROT_Y_DOWN_BUTTON);
        createRotationUiGroup(parentBoxChildren, Rotate.Z_AXIS, Strings.SUPPORT_OBJECT_CONTROL_ROT_Z_LABEL, Strings.SUPPORT_OBJECT_CONTROL_ROT_Z_UP_BUTTON, Strings.SUPPORT_OBJECT_CONTROL_ROT_Z_DOWN_BUTTON);
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

    /**
     * Loads the 3D object from the asset loader with applied object root transformation.
     */
    protected ThreeDObject load3DObjectView() {
        SupportObjectDescriptor descriptor = getDescriptor();
        ThreeDObject result = null;
        try {
            result = mAssetLoader.loadSupportObject3DResource(descriptor, Optional.empty());
        } catch (IOException e) {
            log.error("3D resource of support object <" + descriptor + "> could not be loaded", e);
        }
        if (result == null) {
            result = AssetLoader.loadBroken3DResource();
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
            resetSizesTo(mThreeDObjectRot);
        } catch (IOException e) {
            showErrorImportDialog(Strings.LIBRARY_MANAGER_ERROR_IMPORTING_3D_OBJECT_TEXT, e);
        }
    }

    protected void resetSizesTo(Node objectState) {
        mPreventUpdate = true;
        try {
            Bounds boundsRot = objectState.getBoundsInParent();
            Length objectWidth = CoordinateUtils.coordsToLength(boundsRot.getWidth(), null);
            // Attention: Global coordinate system -> Y/Z exchanged
            Length objectDepth = CoordinateUtils.coordsToLength(boundsRot.getHeight(), null);
            Length objectHeight = CoordinateUtils.coordsToLength(boundsRot.getDepth(), null);
            Length maxSize = Length.max(objectWidth, objectDepth, objectHeight);
            if (maxSize.gt(MAX_NORMAL_OBJECT_SIZE)) {
                double scale = MAX_NORMAL_OBJECT_SIZE.divideBy(maxSize);
                objectWidth = objectWidth.times(scale);
                objectDepth = objectDepth.times(scale);
                objectHeight = objectHeight.times(scale);
            }
            mWidthControl.setLength(objectWidth, null);
            mDepthControl.setLength(objectDepth, null);
            mHeightControl.setLength(objectHeight, null);
            mElevationControl.setLength(Length.ZERO, null);
        } finally {
            mPreventUpdate = false;
        }
        setThreeDObject(mThreeDObjectRaw);
    }

    /**
     * Resets the object sizes to match the raw, rotated model.
     */
    public void resetObjectSizesToModel() {
        resetSizesTo(mThreeDObjectRot);
    }

    /**
     * Resets the object sizes to match the rotated and already sized model, i.e. applies the sizes
     * of the 3D model in its current rotation state.
     */
    public void resetObjectSizesAfterRotation() {
        resetSizesTo(mThreeDObjectScaled);
    }

    /**
     * Rotates the object.
     */
    public void addObjectRotation(double degrees, Point3D axis) {
        if (mThreeDObjectRaw == null) {
            return;
        }
        mObjectRotation.prepend(new Rotate(degrees, axis));
        resetObjectSizesAfterRotation();
        setThreeDObject(mThreeDObjectRaw);
    }

    /**
     * Resets rotation and sizes of the object to the raw 3D model state.
     */
    public void resetObjectValues() {
        mObjectRotation.setToIdentity();
        resetObjectSizesToModel();
    }

    protected double getObjectWidth() {
        return CoordinateUtils.lengthToCoords(mWidthControl.getLength(), null);
    }

    protected double getObjectHeight() {
        return CoordinateUtils.lengthToCoords(mHeightControl.getLength(), null);
    }

    protected double getObjectDepth() {
        return CoordinateUtils.lengthToCoords(mDepthControl.getLength(), null);
    }

    protected double getObjectElevation() {
        return CoordinateUtils.lengthToCoords(mElevationControl.getLength(), null);
    }

    protected static Group rotateAndCenter(Node node, Transform rotate) {
        Group result = new Group(node);
        ThreeDObject.rotateAndCenter(result, rotate);
        return result;
    }

    protected static Group scaleAndElevate(Node node, double width, double depth, double height, double elevation) {
        Bounds bounds = node.getBoundsInParent();

        // Attention: Global coordinate system -> Y/Z exchanged
        Scale scale = new Scale(width / bounds.getWidth(), depth / bounds.getHeight(), height / bounds.getDepth());
        Translate elevationTranslate = new Translate(0, 0, elevation);

        Group result = new Group(node);
        result.getTransforms().addAll(0, Arrays.asList(elevationTranslate, scale));
        return result;
    }

    protected void setThreeDObject(Node node) {
        // Node is in original (3D-model) size, not rotated yet
        mThreeDObjectRaw = node;
        mThreeDObjectRot = rotateAndCenter(mThreeDObjectRaw, mObjectRotation);

        double width = getObjectWidth();
        double depth = getObjectDepth();
        double height = getObjectHeight();
        double elevation = getObjectElevation();

        mThreeDObjectScaled = scaleAndElevate(mThreeDObjectRot, width, depth, height, elevation);

        mVirtualFloor.setSize(2*Math.max(width, depth));

        Group group = new Group(mThreeDObjectScaled, mVirtualFloor);
        mThreeDObjectView.setObjView(group);
    }

    /**
     * Reloads the 3D object from its model, using the currently active rotation, scale and rotation.
     */
    protected void updateThreeDObject() {
        ThreeDObject threeDObject = load3DObjectView();
        mObjectRotation = threeDObject.getORootTransformation().map(t -> new Affine(t)).orElse(new Affine());

        Group node = new Group();
        node.getChildren().addAll(threeDObject.getSurfaceMeshViews());
        setThreeDObject(node);
    }

    /**
     * Creates a new 3D object with the currently active rotation, scale and elevation.
     */
    public Node buildSnapshot3DObject() {
        ThreeDObject threeDObject = load3DObjectView();
        Affine objectRotation = threeDObject.getORootTransformation().map(t -> new Affine(t)).orElse(new Affine());

        Group node = new Group();
        node.getChildren().addAll(threeDObject.getSurfaceMeshViews());

        node = rotateAndCenter(node, objectRotation);

        double width = getObjectWidth();
        double depth = getObjectDepth();
        double height = getObjectHeight();

        return scaleAndElevate(node, width, depth, height, 0);
    }

    public boolean isVirtualFloorVisible() {
        return mShowVirtualFloorCheckBox.isSelected();
    }

    protected void updateVirtualFloor() {
        mVirtualFloor.setVisible(isVirtualFloorVisible());
    }

    public void initializeValues(SupportObjectDescriptor descriptor) {
        mPreventUpdate = true;
        try {
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
        } finally {
            mPreventUpdate = false;
        }

        updateIconImage();
        updatePlanViewImage();
        updateThreeDObject();
        updateVirtualFloor();
    }

    public void saveValues() {
        SupportObjectDescriptor descriptor = getDescriptor();
        saveValues(descriptor);
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

            mAssetLoader.updateSupportObject3DViewObjRotation(getDescriptor(), mObjectRotation);

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
