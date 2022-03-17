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
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.assets.AbstractAssetDescriptor;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.utils.fx.FxUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MultipleAssetsEditControl extends AbstractEditControl implements Initializable {
    public static final String FXML = "MultipleAssetsEditControl.fxml";

    @FXML
    protected CheckBox mAuthorCheckBox;

    @FXML
    protected TextField mAuthorTextField;

    @FXML
    protected CheckBox mCategoryCheckBox;

    @FXML
    protected ComboBox<String> mCategoryComboBox;

    @FXML
    protected CheckBox mTypeCheckBox;

    @FXML
    protected ComboBox<String> mTypeComboBox;

    @FXML
    protected CheckBox mDescriptionCheckBox;

    @FXML
    protected TextArea mDescriptionTextArea;

    public MultipleAssetsEditControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(MultipleAssetsEditControl.class.getResource(FXML));
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
        FxUtils.setImmediateCommitText(mCategoryComboBox);
        FxUtils.setImmediateCommitText(mTypeComboBox);
        ChangeListener<Boolean> checkBoxChangeListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                updateEditors();
            }
        };
        mCategoryCheckBox.selectedProperty().addListener(checkBoxChangeListener);
        mTypeCheckBox.selectedProperty().addListener(checkBoxChangeListener);
        mAuthorCheckBox.selectedProperty().addListener(checkBoxChangeListener);
        mDescriptionCheckBox.selectedProperty().addListener(checkBoxChangeListener);

        updateEditors();
    }

    protected void updateEditors() {
        mCategoryComboBox.setDisable(!mCategoryCheckBox.isSelected());
        mTypeComboBox.setDisable(!mTypeCheckBox.isSelected());
        mAuthorTextField.setDisable(!mAuthorCheckBox.isSelected());
        mDescriptionTextArea.setDisable(!mDescriptionCheckBox.isSelected());
    }

    public void initializeValues(Collection<? extends AbstractAssetDescriptor> assetDescriptors, AssetManager assetManager) {
        Collection<String> categories = assetDescriptors
                        .stream()
                        .map(ad -> ad.getCategory())
                        .collect(Collectors.toSet());
        Collection<String> types = assetDescriptors
                        .stream()
                        .map(ad -> ad.getType())
                        .collect(Collectors.toSet());
        Collection<String> authors = assetDescriptors
                        .stream()
                        .map(ad -> ad.getName())
                        .collect(Collectors.toSet());
        Collection<String> descriptions = assetDescriptors
                        .stream()
                        .map(ad -> ad.getDescription())
                        .collect(Collectors.toSet());

        mCategoryCheckBox.setSelected(false);
        mCategoryComboBox.setItems(FXCollections.observableArrayList(CollectionUtils.union(assetManager.getMaterialSetCategories(), assetManager.getSupportObjectCategories())));
        mCategoryComboBox.getSelectionModel().select(categories.size() == 1 ? categories.iterator().next() : "");
        mTypeCheckBox.setSelected(false);
        mTypeComboBox.setItems(FXCollections.observableArrayList(CollectionUtils.union(assetManager.getMaterialSetTypes(), assetManager.getSupportObjectTypes())));
        mTypeComboBox.getSelectionModel().select(types.size() == 1 ? types.iterator().next() : "");
        mAuthorCheckBox.setSelected(false);
        mAuthorTextField.setText(StringUtils.trimToEmpty(authors.size() == 1 ? authors.iterator().next() : ""));
        mDescriptionCheckBox.setSelected(false);
        mDescriptionTextArea.setText(StringUtils.trimToEmpty(descriptions.size() == 1 ? descriptions.iterator().next() : ""));
    }

    public void updateDescriptors(Collection<? extends AbstractAssetDescriptor> assetDescriptors) {
        for (AbstractAssetDescriptor descriptor : assetDescriptors) {
            if (mAuthorCheckBox.isSelected()) {
                descriptor.setName(StringUtils.trimToNull(mAuthorTextField.getText()));
            }
            if (mCategoryCheckBox.isSelected()) {
                descriptor.setCategory(StringUtils.trimToNull(mCategoryComboBox.getValue()));
            }
            if (mTypeCheckBox.isSelected()) {
                descriptor.setType(StringUtils.trimToNull(mTypeComboBox.getValue()));
            }
            if (mDescriptionCheckBox.isSelected()) {
                descriptor.setType(StringUtils.trimToNull(mDescriptionTextArea.getText()));
            }
        }
    }
}
