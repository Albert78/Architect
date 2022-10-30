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
package de.dh.cad.architect.libraryimporter.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.libraryimporter.ObjectLoader;
import de.dh.cad.architect.libraryimporter.sh3d.furniture.CatalogPieceOfFurniture;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import de.dh.utils.fx.io.formats.obj.RawMaterialData;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CatalogPieceOfFurnitureControl extends BorderPane implements Initializable {
    private static final Logger log = LoggerFactory.getLogger(CatalogPieceOfFurnitureControl.class);

    public static final String FXML = "CatalogPieceOfFurnitureControl.fxml";

    protected final Stage mOwnerStage;
    protected final AssetManager mAssetManager;
    protected final CatalogPieceOfFurniture mPieceOfFurniture;

    @FXML
    protected Label mIdLabel;

    @FXML
    protected Label mNameLabel;

    @FXML
    protected TextArea mDescriptionTextArea;

    @FXML
    protected TextArea mInformationTextArea;

    @FXML
    protected BorderPane mIconImagePane;

    @FXML
    protected ImageView mIconImageView;

    @FXML
    protected BorderPane mPlanViewImagePane;

    @FXML
    protected ImageView mPlanViewImageView;

    @FXML
    protected BorderPane mThreeDResourcePane;

    protected ThreeDObjectViewControl mThreeDObjectView;

    protected Map<String, RawMaterialData> mDefaultMaterials = new TreeMap<>();

    public CatalogPieceOfFurnitureControl(CatalogPieceOfFurniture pieceOfFurnitureData, AssetManager assetManager, Stage window) {
        mOwnerStage = window;
        mAssetManager = assetManager;
        mPieceOfFurniture = pieceOfFurnitureData;

        mDefaultMaterials.putAll(assetManager.getDefaultMaterials());

        FXMLLoader fxmlLoader = new FXMLLoader(CatalogPieceOfFurnitureControl.class.getResource(FXML));
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
        mIdLabel.setText(mPieceOfFurniture.getId());
        mNameLabel.setText(StringUtils.trimToEmpty(mPieceOfFurniture.getName()));
        mDescriptionTextArea.setText(StringUtils.trimToEmpty(mPieceOfFurniture.getDescription()));
        mInformationTextArea.setText(StringUtils.trimToEmpty(mPieceOfFurniture.getInformation()));

        updateIconImage();
        updatePlanViewImage();

        createThreeDResourcePane();

        update3DObjectView();
    }

    protected void createThreeDResourcePane() {
        boolean coordinateSystemInitiallyVisible = true;
        mThreeDObjectView = new ThreeDObjectViewControl();
        mThreeDObjectView.setPrefWidth(250);
        mThreeDObjectView.setPrefHeight(250);
        mThreeDObjectView.setCoordinateSystemVisible(coordinateSystemInitiallyVisible);

        mThreeDResourcePane.setLeft(mThreeDObjectView);
        VBox sizesBox = new VBox();
        ObservableList<Node> boxChildren = sizesBox.getChildren();
        Label titleLabel = new Label("Standardgröße:");
        boxChildren.add(titleLabel);
        VBox.setMargin(titleLabel, new Insets(0, 0, 5, 0));
        Label l = new Label("Breite (X):");
        HBox.setMargin(l, new Insets(0, 5, 0, 0));
        Label widthLabel = new Label(lengthOrDefault(mPieceOfFurniture.getWidth()));
        boxChildren.add(new HBox(l, widthLabel));
        l = new Label("Höhe (Y):");
        HBox.setMargin(l, new Insets(0, 5, 0, 0));
        Label heightLabel = new Label(lengthOrDefault(mPieceOfFurniture.getHeight()));
        boxChildren.add(new HBox(l, heightLabel));
        l = new Label("Tiefe (Z):");
        HBox.setMargin(l, new Insets(0, 5, 0, 0));
        Label depthLabel = new Label(lengthOrDefault(mPieceOfFurniture.getDepth()));
        boxChildren.add(new HBox(l, depthLabel));
        CheckBox coordinateSystemCB = new CheckBox("Koordinatensystem");
        coordinateSystemCB.setSelected(coordinateSystemInitiallyVisible);
        coordinateSystemCB.selectedProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                mThreeDObjectView.setCoordinateSystemVisible(coordinateSystemCB.isSelected());
            }
        });
        boxChildren.add(coordinateSystemCB);
        VBox.setMargin(coordinateSystemCB, new Insets(5, 0, 0, 0));
        mThreeDResourcePane.setCenter(sizesBox);
        BorderPane.setMargin(sizesBox, new Insets(0, 0, 0, 5));
    }

    protected String lengthOrDefault(float value) {
        if (value == 0) {
            return "-";
        }
        return Float.toString(value) + " cm";
    }

    protected Image loadImage(IResourceLocator resource) {
        Image image = null;
        if (resource != null) {
            try (InputStream is = resource.inputStream()) {
                image = new Image(is);
            } catch (IOException e) {
                log.error("Image '" + resource.getFileName() + "' could not be loaded", e);
            }
        }
        if (image == null) {
            image = AssetLoader.loadBrokenImageSmall();
        }
        return image;
    }

    protected void updateIconImage() {
        Image image = loadImage(mPieceOfFurniture.getIcon());
        mIconImageView.setImage(image);
    }

    protected void updatePlanViewImage() {
        Image image = loadImage(mPieceOfFurniture.getPlanIcon());
        mPlanViewImageView.setImage(image);
    }

    protected void update3DObjectView() {
        Node objView = ObjectLoader.load3DResource(mPieceOfFurniture.getModel(), mPieceOfFurniture.getModelRotationJavaFX(), mDefaultMaterials);
        if (objView == null) {
            Group g = new Group();
            g.getChildren().addAll(AssetLoader.loadBroken3DResource().getSurfaces());
            objView = g;
        }
        mThreeDObjectView.setObjView(objView, 100);
    }
}
