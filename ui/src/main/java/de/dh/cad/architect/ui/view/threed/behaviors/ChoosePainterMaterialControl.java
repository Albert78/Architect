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
package de.dh.cad.architect.ui.view.threed.behaviors;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.view.libraries.MaterialChooserDialog;
import de.dh.cad.architect.ui.view.libraries.ThreeDPreview;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class ChoosePainterMaterialControl extends BorderPane {
    private static final Logger log = LoggerFactory.getLogger(ChoosePainterMaterialControl.class);

    protected final UiController mUiController;
    protected final AssetManager mAssetManager;
    protected final AssetLoader mAssetLoader;
    protected final Label mCurrentMaterialLabel;
    protected final ThreeDObjectViewControl mThreeDObjectView;

    protected AssetRefPath mSelectedMaterial = null;

    public ChoosePainterMaterialControl(UiController uiController) {
        mUiController = uiController;
        mAssetManager = uiController.getAssetManager();
        mAssetLoader = mAssetManager.buildAssetLoader();
        VBox box = new VBox(5);
        setMargin(box, new Insets(5));
        Label titleLabel = new Label(Strings.THREE_D_PAINTER_BEHAVIOR_MATERIAL_CONTROL_TITLE);
        titleLabel.setStyle(Constants.INTERACTIONS_TITLE_STYLE);

        mCurrentMaterialLabel = new Label("-");
        Button chooseMaterialButton = new Button("...");
        chooseMaterialButton.setOnAction(event -> {
            MaterialChooserDialog dialog = MaterialChooserDialog.createWithProgressIndicator(mAssetLoader, Strings.THREE_D_PAINTER_BEHAVIOR_CHOOSE_MATERIAL_DIALOG_TITLE);
            dialog.selectMaterial(mSelectedMaterial);
            Optional<AssetRefPath> oRes = dialog.showAndWait();
            oRes.ifPresent(mrp -> {
                setMaterial(mrp);
            });
        });
        mThreeDObjectView = new ThreeDObjectViewControl();
        mThreeDObjectView.setPrefWidth(250);
        mThreeDObjectView.setPrefHeight(250);
        mThreeDObjectView.setCoordinateSystemVisible(false);
        BorderPane currentMaterialPane = new BorderPane(mCurrentMaterialLabel, null, chooseMaterialButton, null, null);
        BorderPane.setAlignment(mCurrentMaterialLabel, Pos.CENTER_LEFT);
        box.getChildren().addAll(titleLabel, currentMaterialPane, mThreeDObjectView);
        setCenter(box);

        setMaterial(null);
    }

    public void setMaterial(AssetRefPath materialRef) {
        String materialName = materialRef == null ? null : materialRef.getOMaterialName().orElse(null);
        if (materialName != null) {
            try {
                @SuppressWarnings("null")
                MaterialSetDescriptor descriptor = mAssetManager.loadMaterialSetDescriptor(materialRef.withoutMaterialName());
                mSelectedMaterial = materialRef;
                mCurrentMaterialLabel.setText(descriptor.getName() + " / " + materialName);
                mThreeDObjectView.setObjView(ThreeDPreview.createMaterialPreviewBox(mSelectedMaterial, mAssetLoader, Optional.empty(), true), 200);
                return;
            } catch (IOException e) {
                log.error("Error loading material <" + materialRef + ">");
            }
        }
        mSelectedMaterial = null;
        mCurrentMaterialLabel.setText(Strings.THREE_D_PAINTER_BEHAVIOR_NO_MATERIAL_CHOOSEN);
        mThreeDObjectView.setObjView(null, 200);
    }

    public AssetRefPath getSelectedMaterial() {
        return mSelectedMaterial;
    }
}
