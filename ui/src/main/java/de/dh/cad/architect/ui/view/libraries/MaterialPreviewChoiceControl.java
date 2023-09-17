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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.fx.nodes.objviewer.CoordinateSystemConfiguration;
import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.util.StringConverter;

public class MaterialPreviewChoiceControl extends BorderPane implements Initializable {
    private Logger log = LoggerFactory.getLogger(MaterialPreviewChoiceControl.class);

    public static final String FXML = "MaterialPreviewChoiceControl.fxml";

    protected final AssetLoader mAssetLoader;

    @FXML
    protected ChoiceBox<RawMaterialData> mMaterialChoiceBox;

    @FXML
    protected TextField mMaterialDescriptorRefTextField;

    @FXML
    protected BorderPane mParentPane;

    protected MaterialSetDescriptor mMaterialSetDescriptor;
    protected ThreeDObjectViewControl mThreeDObjectView;

    protected boolean mFallbackToPlaceholderOnError = true;
    protected BiConsumer<MaterialDescriptor, Exception> mOnMaterialLoadError = null;
    protected Consumer<MaterialDescriptor> mOnMaterialLoaded = null;

    protected SimpleObjectProperty<MaterialDescriptor> mSelectedMaterialProperty = new SimpleObjectProperty<>(null);

    public MaterialPreviewChoiceControl(AssetLoader assetLoader) {
        mAssetLoader = assetLoader;

        FXMLLoader fxmlLoader = new FXMLLoader(MaterialPreviewChoiceControl.class.getResource(FXML));
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
        SingleSelectionModel<RawMaterialData> mcbSelectionModel = mMaterialChoiceBox.getSelectionModel();
        mMaterialChoiceBox.setConverter(new StringConverter<RawMaterialData>() {
            @Override
            public String toString(RawMaterialData md) {
                return md == null ? "-" : md.getName();
            }

            @Override
            public RawMaterialData fromString(String str) {
                for (RawMaterialData md : mMaterialChoiceBox.getItems()) {
                    if (Objects.equals(md.getName(), str)) {
                        return md;
                    }
                }
                return null;
            }
        });
        mcbSelectionModel.selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends RawMaterialData> observable, RawMaterialData oldValue, RawMaterialData newValue) {
                updateSelectdedMaterial();
                update3DObjectView();
            }
        });

        mThreeDObjectView = new ThreeDObjectViewControl(CoordinateSystemConfiguration.architect());
        mThreeDObjectView.setPrefWidth(250);
        mThreeDObjectView.setPrefHeight(250);
        mThreeDObjectView.setCoordinateSystemVisible(false);
        mParentPane.setCenter(mThreeDObjectView);
        update3DObjectView();
    }

    public boolean isFallbackToPlaceholderOnError() {
        return mFallbackToPlaceholderOnError;
    }

    public void setFallbackToPlaceholderOnError(boolean value) {
        mFallbackToPlaceholderOnError = value;
    }

    public BiConsumer<MaterialDescriptor, Exception> getOnMaterialError() {
        return mOnMaterialLoadError;
    }

    public void setOnMaterialLoadError(BiConsumer<MaterialDescriptor, Exception> value) {
        mOnMaterialLoadError = value;
    }

    public Consumer<MaterialDescriptor> getOnMaterialLoaded() {
        return mOnMaterialLoaded;
    }

    public void setOnMaterialLoaded(Consumer<MaterialDescriptor> value) {
        mOnMaterialLoaded = value;
    }

    public void initialize(MaterialSetDescriptor descriptor) {
        mMaterialSetDescriptor = descriptor;

        AssetRefPath materialSetRef = mMaterialSetDescriptor.getSelfRef();
        SingleSelectionModel<RawMaterialData> mcbSelectionModel = mMaterialChoiceBox.getSelectionModel();
        try {
            List<RawMaterialData> materials = new ArrayList<>(mAssetLoader.loadMaterials(materialSetRef).values());
            Collections.sort(materials, Comparator.comparing(RawMaterialData::getName));

            mMaterialChoiceBox.setItems(FXCollections.observableArrayList(materials));
            if (materials.isEmpty()) {
                update3DObjectView();
            } else {
                mcbSelectionModel.select(0);
            }
        } catch (IOException e) {
            log.error("Error loading materials from <" + materialSetRef + ">", e);
        }
    }

    protected void updateSelectdedMaterial() {
        MaterialDescriptor selectedMaterialDescriptor = getSelectedMaterialDescriptor();
        mSelectedMaterialProperty.set(selectedMaterialDescriptor);
        mMaterialDescriptorRefTextField.setText(selectedMaterialDescriptor == null ? "-" : selectedMaterialDescriptor.getMaterialRef().toPathString());
    }

    public SimpleObjectProperty<MaterialDescriptor> selectedMaterialProperty() {
        return mSelectedMaterialProperty;
    }

    public RawMaterialData getSelectedMaterial() {
        return mMaterialChoiceBox.getSelectionModel().getSelectedItem();
    }

    public String getSelectedMaterialName() {
        RawMaterialData material = getSelectedMaterial();
        return material == null ? null : material.getName();
    }

    public MaterialDescriptor getSelectedMaterialDescriptor() {
        String selectedMaterialName = getSelectedMaterialName();
        return selectedMaterialName == null ? null : new MaterialDescriptor(mMaterialSetDescriptor, selectedMaterialName);
    }

    public void selectMaterial(String materialName) {
        SingleSelectionModel<RawMaterialData> selectionModel = mMaterialChoiceBox.getSelectionModel();
        if (materialName == null) {
            selectionModel.clearSelection();
            return;
        }
        List<RawMaterialData> items = mMaterialChoiceBox.getItems();
        for (RawMaterialData material : items) {
            if (materialName.equals(material.getName())) {
                selectionModel.select(material);
            }
        }
    }

    protected void update3DObjectView() {
        MaterialDescriptor materialDescriptor = getSelectedMaterialDescriptor();
        try {
            Node n = ThreeDPreview.createMaterialPreviewBox(materialDescriptor == null ? null : materialDescriptor.getMaterialRef(),
                mAssetLoader, Optional.empty(), mFallbackToPlaceholderOnError);
            mThreeDObjectView.setObjView(n);
            notifyMaterialLoaded(materialDescriptor);
        } catch (RuntimeException e) {
            mThreeDObjectView.setObjView(null);
            notifyMaterialLoadError(materialDescriptor, new RuntimeException("Error loading material <" + materialDescriptor + ">", e));
        }
    }

    protected void notifyMaterialLoaded(MaterialDescriptor descriptor) {
        if (mOnMaterialLoaded != null) {
            mOnMaterialLoaded.accept(descriptor);
        }
    }

    protected void notifyMaterialLoadError(MaterialDescriptor descriptor, Exception e) {
        if (mOnMaterialLoadError != null) {
            mOnMaterialLoadError.accept(descriptor, e);
        }
    }
}
