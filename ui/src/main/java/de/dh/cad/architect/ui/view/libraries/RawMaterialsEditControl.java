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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.codeeditors.MtlEditor;
import de.dh.cad.architect.fx.nodes.objviewer.CoordinateSystemConfiguration;
import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.model.assets.AbstractModelResource;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.model.assets.MaterialsModel;
import de.dh.cad.architect.model.assets.RawMaterialModel;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.controls.LengthControl;
import de.dh.cad.architect.ui.utils.DialogUtils;
import de.dh.cad.architect.utils.Namespace;
import de.dh.cad.architect.utils.SystemUtils;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.PlainFileSystemResourceLocator;
import de.dh.utils.MaterialMapping;
import de.dh.utils.io.fx.MaterialData;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.PhongMaterial;
import javafx.util.StringConverter;

public class RawMaterialsEditControl extends BorderPane implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(RawMaterialsEditControl.class);

    public static final String FXML = "RawMaterialsEditControl.fxml";

    @FXML
    protected TextField mMaterialSetNameTF;

    @FXML
    protected TextField mMaterialSetRefPathTF;

    @FXML
    protected TextField mResourceDirectoryTF;

    @FXML
    protected Button mOpenResourceDirectoryButton;

    @FXML
    protected ChoiceBox<RawMaterialModel> mMaterialChoiceBox;

    @FXML
    protected Button mAddMaterialButton;

    @FXML
    protected Button mRemoveMaterialButton;

    @FXML
    protected Button mCopyMaterialButton;

    @FXML
    protected Button mImportTextureButton;

    @FXML
    protected TextField mMaterialNameTF;

    @FXML
    protected TextField mMaterialRefPathTF;

    @FXML
    protected CheckBox mFixedTileSizeCheckBox;

    @FXML
    protected Pane mTileSizeControlsParent;

    @FXML
    protected Pane mTileSizeXParent;

    @FXML
    protected Pane mTileSizeYParent;

    @FXML
    protected BorderPane mSourceCodeParent;

    @FXML
    protected BorderPane mThreeDViewParentPane;

    @FXML
    protected TextArea mPreviewLogTextArea;

    protected final AssetLoader mAssetLoader;
    protected ThreeDObjectViewControl mThreeDObjectView;
    protected MtlEditor mMtlEditor;
    protected LengthControl mTileSizeXControl;
    protected LengthControl mTileSizeYControl;

    protected MaterialSetDescriptor mDescriptor = null;
    protected IDirectoryLocator mResourceDirectory = null;
    protected boolean mBlockUpdates = false;

    public RawMaterialsEditControl(AssetLoader assetLoader) {
        mAssetLoader = assetLoader;

        FXMLLoader fxmlLoader = new FXMLLoader(RawMaterialsEditControl.class.getResource(FXML));
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
        SingleSelectionModel<RawMaterialModel> mcbSelectionModel = mMaterialChoiceBox.getSelectionModel();
        mMaterialChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(RawMaterialModel mm) {
                return mm == null ? "-" : mm.getName();
            }

            @Override
            public RawMaterialModel fromString(String str) {
                for (RawMaterialModel mm : mMaterialChoiceBox.getItems()) {
                    if (Objects.equals(mm.getName(), str)) {
                        return mm;
                    }
                }
                return null;
            }
        });
        mcbSelectionModel.selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateToSelectedMaterial();
        });
        mAddMaterialButton.setOnAction(event -> {
            RawMaterialModel newMaterial = createNewMaterial();
            addMaterial(newMaterial);
        });
        mRemoveMaterialButton.setOnAction(event -> {
            RawMaterialModel selectedMaterial = getSelectedMaterial();
            if (selectedMaterial == null) {
                return;
            }
            Optional<ButtonType> res = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_MATERIAL_SET_DELETE_MATERIAL_CONFIRMATION_TEXT, selectedMaterial.getName()),
                    ButtonType.YES, ButtonType.CANCEL).showAndWait();
            if (res.orElse(null) == ButtonType.YES) {
                removeMaterial(selectedMaterial);
            }
        });
        mCopyMaterialButton.setOnAction(event -> {
            RawMaterialModel selectedMaterial = getSelectedMaterial();
            if (selectedMaterial == null) {
                return;
            }
            RawMaterialModel copy = copyMaterial(selectedMaterial);
            addMaterial(copy);
        });
        mImportTextureButton.setOnAction(event -> {
            importTexture();
        });
        mMaterialNameTF.textProperty().addListener((observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            mBlockUpdates = true;
            try {
                Collection<String> availableMaterialNames = getMaterialNames();
                availableMaterialNames.remove(oldValue);
                String name = validateMaterialName(newValue, availableMaterialNames);
                RawMaterialModel selectedMaterial = getSelectedMaterial();
                selectedMaterial.setName(name);

                // Force material choice box update the displayed name
                StringConverter<RawMaterialModel> converter = mMaterialChoiceBox.getConverter();
                mMaterialChoiceBox.setConverter(null);
                mMaterialChoiceBox.setConverter(converter);
            } finally {
                mBlockUpdates = false;
            }
        });
        ChangeListener<Object> materialPropertyChangeListener = (observable, oldValue, newValue) -> {
            if (mBlockUpdates) {
                return;
            }
            RawMaterialModel selectedMaterial = getSelectedMaterial();
            saveMaterialValues(selectedMaterial);
            updatePreview(selectedMaterial);
        };
        mFixedTileSizeCheckBox.selectedProperty().addListener(materialPropertyChangeListener);
        mTileSizeXControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mTileSizeXParent.getChildren().add(mTileSizeXControl);
        BooleanBinding sizeControlsDisabled =
                mFixedTileSizeCheckBox.selectedProperty().not()
                .or(mFixedTileSizeCheckBox.disabledProperty());
        mTileSizeControlsParent.disableProperty().bind(sizeControlsDisabled);
        mTileSizeXControl.lengthProperty().addListener(materialPropertyChangeListener);

        mTileSizeYControl = new LengthControl(Constants.LOCALIZED_NUMBER_FORMAT);
        mTileSizeYParent.getChildren().add(mTileSizeYControl);
        mTileSizeYControl.lengthProperty().addListener(materialPropertyChangeListener);

        mThreeDObjectView = new ThreeDObjectViewControl(CoordinateSystemConfiguration.architect());
        mThreeDObjectView.setPrefWidth(250);
        mThreeDObjectView.setPrefHeight(250);
        mThreeDObjectView.setCoordinateSystemVisible(false);
        mThreeDViewParentPane.setCenter(mThreeDObjectView);

        mMtlEditor = new MtlEditor();
        mSourceCodeParent.setCenter(mMtlEditor);
        mMtlEditor.textProperty().addListener(materialPropertyChangeListener);

        updateToSelectedMaterial();
    }

    protected Collection<String> getMaterialNames() {
        return new ArrayList<>(mMaterialChoiceBox.getItems().stream().map(RawMaterialModel::getName).toList());
    }

    protected String validateMaterialName(String template, Collection<String> availableMaterialNames) {
        String name = template;
        if (StringUtils.isEmpty(name)) {
            name = Strings.NEW_MATERIAL_NAME;
        }
        // TODO: Remove illegal characters, check for characters valid for java identifiers should be ok
        return Namespace.generateName(name, availableMaterialNames);
    }

    public static RawMaterialModel createNewMaterial() {
        return new RawMaterialModel(Strings.NEW_MATERIAL_NAME, Optional.of(new Vector2D(Length.ofCM(20), Length.ofCM(20))),
                Arrays.asList("Kd 0.1 0.1 1"));
    }

    public void addMaterial(RawMaterialModel material) {
        material.setName(validateMaterialName(material.getName(), getMaterialNames()));
        mMaterialChoiceBox.getItems().add(material);
        mMaterialChoiceBox.getSelectionModel().select(material);
    }

    public void removeMaterial(RawMaterialModel material) {
        SingleSelectionModel<RawMaterialModel> selectionModel = mMaterialChoiceBox.getSelectionModel();
        RawMaterialModel selectedItem = selectionModel.getSelectedItem();
        mMaterialChoiceBox.getItems().remove(material);
        if (selectedItem == material) {
            selectionModel.select(0);
        }
    }

    protected RawMaterialModel copyMaterial(RawMaterialModel material) {
        return new RawMaterialModel(material.getName(), material.getTileSize(), material.getCommands());
    }

    protected void importTexture() {
        AssetRefPath materialSetRef = mDescriptor.getSelfRef();
        Path imagePath = DialogUtils.openImageDialog(Strings.LIBRARY_MANAGER_IMPORT_TEXTURE_FOR_MATERIAL_TITLE,
                mAssetLoader.getAssetManager().getConfiguration(), getScene().getWindow());
        try {
            Collection<AssetLoader.ImportResource> resources = List.of(AssetLoader.ImportResource.fromResource(new PlainFileSystemResourceLocator(imagePath)));
            mAssetLoader.importAssetResources(resources, materialSetRef);
        } catch (IOException e) {
            log.error("Error importing texture resource from " + imagePath + " to <" + materialSetRef + ">");
        }
        Path imageFileName = imagePath.getFileName();
        String materialName = FilenameUtils.getBaseName(imageFileName.toString());
        RawMaterialModel newMaterial = new RawMaterialModel(materialName, Optional.empty(),
                Arrays.asList("map_Kd " + imageFileName));
        addMaterial(newMaterial);
    }

    /**
     * Initializes this control with the given material set descriptor.
     * @param descriptor Descriptor whose contents should be edited.
     */
    public void initialize(MaterialSetDescriptor descriptor) {
        mDescriptor = descriptor;
        AssetRefPath materialSetRef = mDescriptor.getSelfRef();
        try {
            mMaterialSetNameTF.setText(mDescriptor.getName());
            mMaterialSetRefPathTF.setText(materialSetRef.toPathString());
            mResourceDirectory = mAssetLoader.getResourcesDirectory(materialSetRef);
            mResourceDirectoryTF.setText(mResourceDirectory.getAbsolutePath());
            IDirectoryLocator materialSetResourcesDirectory = mResourceDirectory;
            mOpenResourceDirectoryButton.setOnAction(event -> {
                try {
                    if (!materialSetResourcesDirectory.exists()) {
                        materialSetResourcesDirectory.mkDirs();
                    }
                    SystemUtils.openDirectoryInExplorer(Path.of(materialSetResourcesDirectory.getAbsolutePath()));
                } catch (IOException e) {
                    log.error("Unable to create resources directory to material set descriptor <" + materialSetRef + ">");
                }
            });

            AbstractModelResource model = mDescriptor.getModel();
            Collection<RawMaterialModel> materials = Collections.emptyList();
            if (model instanceof MaterialsModel mm) {
                materials = mm.getMaterials().values();
            } else {
                logMaterialLoadError(new NotImplementedException("Material set descriptor <" + materialSetRef + "> contains a materials model of unknown class " + model.getClass()));
            }
            ObservableList<RawMaterialModel> items = FXCollections.observableArrayList(materials);
            mMaterialChoiceBox.setItems(items);
            mMaterialChoiceBox.getSelectionModel().select(0);
        } catch (IOException e) {
            log.error("Error loading data from material set descriptor <" + materialSetRef + ">", e);

            mMaterialSetNameTF.setText("-");
            mMaterialSetRefPathTF.setText("-");
            mResourceDirectoryTF.setText("-");

            mMaterialChoiceBox.setItems(FXCollections.observableArrayList());
        }
    }

    public void saveValues() {
        try {
            saveCurrentMaterialValues();
            mDescriptor.setModel(new MaterialsModel(mMaterialChoiceBox.getItems()));

            mAssetLoader.getAssetManager().saveAssetDescriptor(mDescriptor);
        } catch (IOException e) {
            log.error("Error while saving material set '" + mDescriptor.getSelfRef() + "'", e);
        }
    }

    protected void saveCurrentMaterialValues() {
        RawMaterialModel selectedMaterial = getSelectedMaterial();
        saveMaterialValues(selectedMaterial);
    }

    protected void saveMaterialValues(RawMaterialModel material) {
        material.setName(mMaterialNameTF.getText());
        Length sizeX = mTileSizeXControl.getLength();
        Length sizeY = mTileSizeYControl.getLength();
        if (mFixedTileSizeCheckBox.isSelected() && sizeX != null && sizeY != null) {
            material.setTileSize(Optional.of(new Vector2D(sizeX, sizeY)));
        } else {
            material.setTileSize(Optional.empty());
        }
        material.setCommands(getSourceCode());
    }

    protected void loadSourceCode(List<String> lines) {
        mMtlEditor.replaceText(StringUtils.join(lines, "\n"));
    }

    protected List<String> getSourceCode() {
        return Arrays.asList(mMtlEditor.getText().split("\n"));
    }

    protected RawMaterialModel getSelectedMaterial() {
        return mMaterialChoiceBox.getValue();
    }

    /**
     * Updates all controls to reflect the state of the selected material.
     */
    protected void updateToSelectedMaterial() {
        if (mBlockUpdates) {
            return;
        }
        mBlockUpdates = true;
        try {
            RawMaterialModel material = getSelectedMaterial();
            boolean materialIsEmpty = material == null;
            mMaterialNameTF.setDisable(materialIsEmpty);
            mMaterialRefPathTF.setDisable(materialIsEmpty);
            mFixedTileSizeCheckBox.setDisable(materialIsEmpty);
            mRemoveMaterialButton.setDisable(materialIsEmpty);
            mCopyMaterialButton.setDisable(materialIsEmpty);
            if (materialIsEmpty) {
                mMaterialNameTF.setText("");
                mMaterialRefPathTF.setText("");
                mFixedTileSizeCheckBox.setSelected(true);
                mTileSizeXControl.setLength(Length.ofCM(20));
                mTileSizeYControl.setLength(Length.ofCM(20));
                loadSourceCode(Collections.emptyList());
            } else {
                String materialName = material.getName();
                mMaterialNameTF.setText(materialName);
                AssetRefPath materialRef = mDescriptor.getSelfRef().withMaterialName(materialName);
                mMaterialRefPathTF.setText(materialRef.toPathString());
                if (material.getTileSize().isPresent()) {
                    mFixedTileSizeCheckBox.setSelected(true);
                    Vector2D tileSize = material.getTileSize().get();
                    mTileSizeXControl.setLength(tileSize.getX());
                    mTileSizeYControl.setLength(tileSize.getY());
                } else {
                    mFixedTileSizeCheckBox.setSelected(false);
                }
                loadSourceCode(material.getCommands());
            }
            updatePreview(material);
        } finally {
            mBlockUpdates = false;
        }
    }

    protected void clearLogView() {
        mPreviewLogTextArea.deleteText(0, mPreviewLogTextArea.getLength());
    }

    protected void appendLogMsg(String msg) {
        mPreviewLogTextArea.appendText(msg + "\n");
    }

    protected void updatePreview(RawMaterialModel rawMaterial) {
        MaterialData materialData = rawMaterial == null ? null : AssetLoader.mapMaterial(rawMaterial, mResourceDirectory);
        try {
            PhongMaterial material = materialData == null ? null : mAssetLoader.buildMaterial_Strict(materialData, MaterialMapping.stretch());
            Node materialPreviewBox = ThreeDPreview.createMaterialPreviewBox(material, 100);
            mThreeDObjectView.setObjView(materialPreviewBox);
            logMaterialLoaded(rawMaterial == null ? "-" : rawMaterial.getName());
        } catch (Exception e) {
            logMaterialLoadError(e);
        }
    }

    protected void logMaterialLoaded(String materialName) {
        clearLogView();
        appendLogMsg("Successfully loaded material '" + materialName + "'");
    }

    protected void logMaterialLoadError(Exception e) {
        clearLogView();
        try (StringWriter sw = new StringWriter()) {
            e.printStackTrace(new PrintWriter(sw));
            appendLogMsg(sw.toString() + "\n");
        } catch (IOException ex) {
            appendLogMsg("- Error writing error message -" + ex.getMessage());
        }
    }

    public BooleanProperty validProperty() {
        // TODO: Check validity of controls
        return new SimpleBooleanProperty(true);
    }
}
