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
package de.dh.cad.architect.ui.view.libraries;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.utils.fx.FxUtils;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MaterialSetEditControl extends AbstractAssetEditControl implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(MaterialSetEditControl.class);

    public static final String FXML = "MaterialSetEditControl.fxml";

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
    protected HBox mMaterialChoiceParent;

    @FXML
    protected Button mEditMaterialButton;

    protected MaterialPreviewChoiceControl mMaterialPreviewChoiceControl;

    public MaterialSetEditControl(AssetManager assetManager) {
        super(assetManager);

        FXMLLoader fxmlLoader = new FXMLLoader(MaterialSetEditControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MaterialSetDescriptor getDescriptor() {
        return (MaterialSetDescriptor) super.getDescriptor();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mCategoryComboBox.setItems(FXCollections.observableArrayList(mAssetManager.getMaterialSetCategories()));
        FxUtils.setImmediateCommitText(mCategoryComboBox);
        mTypeComboBox.setItems(FXCollections.observableArrayList(mAssetManager.getMaterialSetTypes()));
        FxUtils.setImmediateCommitText(mTypeComboBox);
        mChooseIconImageButton.setOnAction(event -> {
            Path path = openIconImageDialog();
            if (path != null) {
                importIconImage(path);
            }
        });

        mMaterialPreviewChoiceControl = new MaterialPreviewChoiceControl(mAssetLoader);
        mMaterialChoiceParent.getChildren().add(0, mMaterialPreviewChoiceControl);

        mEditMaterialButton.setOnAction(event -> {
            showRawMaterialSetEditDialog();
        });
    }

    public void initializeValues(MaterialSetDescriptor descriptor) {
        super.initialize(descriptor);

        AssetRefPath assetRefPath = descriptor.getSelfRef();
        mAssetCollectionNameTextField.setText(assetRefPath.getAnchor().toString());
        mIdTextField.setText(descriptor.getId());
        mAssetRefPathTextField.setText(assetRefPath.toPathString());
        mNameTextField.setText(StringUtils.trimToEmpty(descriptor.getName()));

        mCategoryComboBox.getSelectionModel().select(descriptor.getCategory());
        mTypeComboBox.getSelectionModel().select(descriptor.getType());
        mDescriptionTextArea.setText(StringUtils.trimToEmpty(descriptor.getDescription()));

        mMaterialPreviewChoiceControl.initialize(descriptor);

        updateIconImage();
    }

    public void saveValues(MaterialSetDescriptor descriptor) {
        try {
            descriptor.setName(StringUtils.trimToNull(mNameTextField.getText()));
            descriptor.setCategory(StringUtils.trimToNull(mCategoryComboBox.getValue()));
            descriptor.setType(StringUtils.trimToNull(mTypeComboBox.getValue()));
            descriptor.setDescription(StringUtils.trimToNull(mDescriptionTextArea.getText()));

            mAssetManager.saveAssetDescriptor(descriptor);
        } catch (IOException e) {
            log.error("Error while saving material set '" + descriptor.getId() + "'", e);
        }
    }

    protected void showRawMaterialSetEditDialog() {
        MaterialSetDescriptor descriptor = getDescriptor();
        Alert editDialog = new Alert(AlertType.NONE);
        editDialog.setTitle(Strings.LIBRARY_MANAGER_EDIT_RAW_MATERIAL_SET_DIALOG_TITLE);
        editDialog.setHeaderText(MessageFormat.format(Strings.LIBRARY_MANAGER_EDIT_RAW_MATERIAL_SET_DIALOG_HEADER, descriptor.getName()));
        editDialog.setResizable(true);

        RawMaterialsFileEditControl editControl = new RawMaterialsFileEditControl(mAssetLoader);
        editControl.initialize(descriptor);

        DialogPane dialogPane = editDialog.getDialogPane();
        dialogPane.setContent(editControl);
        Scene scene = dialogPane.getScene();

        scene.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Stage stage = (Stage) scene.getWindow();
        stage.setHeight(800);
        stage.setWidth(1000);

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        BooleanExpression invalidProperty = editControl.validProperty().not();
        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        // Actually, I would bind the disableProperty to the invalidProperty but unfortunately, disableProperty is read only...
        okButton.setDisable(invalidProperty.get());
        invalidProperty.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                okButton.setDisable(invalidProperty.get());
            }
        });

        editControl.requestFocus();

        editDialog.showAndWait();
        mMaterialPreviewChoiceControl.initialize(descriptor);
    }

    @Override
    protected void updateIconImage() {
        Image image = loadIconImage();
        mIconImageView.setImage(image);
    }
}
