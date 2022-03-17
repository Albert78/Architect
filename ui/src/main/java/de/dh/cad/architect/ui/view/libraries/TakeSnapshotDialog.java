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

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class TakeSnapshotDialog extends Dialog<AssetRefPath> {
    public static interface IImageSaveEvents {
        void onSaveIconImage(Image img);
        void onSavePlanViewImage(Image img);
    }

    public static final String FXML = "TakeSnapshotDialog.fxml";

    protected static final int DIALOG_MIN_WIDTH = 1200;
    protected static final int DIALOG_MIN_HEIGHT = 800;

    protected static final int SNAPSHOT_CONTROL_PREFERRED_WIDTH = 800;
    protected static final int SNAPSHOT_CONTROL_PREFERRED_HEIGHT = 600;

    @FXML
    protected Button mTakeSnapshotButton;

    @FXML
    protected ImageView mImageView;

    @FXML
    protected Label mImageSizeLabel;

    @FXML
    protected Button mUseAsIconImageButton;

    @FXML
    protected Button mUseAsPlanViewImageButton;

    @FXML
    protected Label mHintLabel;

    protected Timeline mRemoveHintAnimation;
    protected TakeSnapshotControl mTakeSnapshotControl;
    protected final IImageSaveEvents mOnSaveImage;

    public TakeSnapshotDialog(IImageSaveEvents onSaveImage) {
        mOnSaveImage = onSaveImage;
        setTitle(Strings.LIBRARY_MANAGER_EDIT_SUPPORT_OBJECT_TAKE_SNAPSHOT_DIALOG_TITLE);

        // TODO: Nice graphic
        DialogPane dialogPane = getDialogPane();
        ObservableList<ButtonType> buttonTypes = dialogPane.getButtonTypes();
        buttonTypes.add(ButtonType.CLOSE);

        buildLayout(dialogPane);

        setResizable(true);

        Scene scene = dialogPane.getScene();

        scene.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Stage stage = (Stage) scene.getWindow();
        stage.setMinHeight(DIALOG_MIN_HEIGHT);
        stage.setMinWidth(DIALOG_MIN_WIDTH);
    }

    protected void buildLayout(DialogPane dialogPane) {
        BorderPane root = new BorderPane();
        dialogPane.setContent(root);
        FXMLLoader fxmlLoader = new FXMLLoader(TakeSnapshotControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(root);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mRemoveHintAnimation = new Timeline(new KeyFrame(Duration.seconds(5), new KeyValue(mHintLabel.textProperty(), "")));

        mTakeSnapshotControl = new TakeSnapshotControl();
        mTakeSnapshotControl.setPrefSize(SNAPSHOT_CONTROL_PREFERRED_WIDTH, SNAPSHOT_CONTROL_PREFERRED_HEIGHT);
        root.setLeft(mTakeSnapshotControl);

        mTakeSnapshotButton.setOnAction(event -> {
            Image img = mTakeSnapshotControl.createSnapshot();
            showImage(img);
            mImageSizeLabel.setText((int) img.getWidth() + " x " + (int) img.getHeight());
        });

        mUseAsIconImageButton.setOnAction(event -> {
            mOnSaveImage.onSaveIconImage(mImageView.getImage());
            showHint(Strings.LIBRARY_MANAGER_EDIT_SUPPORT_OBJECT_TAKE_SNAPSHOT_ICON_IMAGE_SAVED);
        });

        mUseAsPlanViewImageButton.setOnAction(event -> {
            mOnSaveImage.onSavePlanViewImage(mImageView.getImage());
            showHint(Strings.LIBRARY_MANAGER_EDIT_SUPPORT_OBJECT_TAKE_SNAPSHOT_PLAN_VIEW_IMAGE_SAVED);
        });
    }

    protected void showImage(Image img) {
        mImageView.setImage(img);
        mUseAsIconImageButton.setDisable(false);
        mUseAsPlanViewImageButton.setDisable(false);
    }

    protected void showHint(String hintText) {
        mHintLabel.setText(hintText);
        mRemoveHintAnimation.stop();
        mRemoveHintAnimation.play();
    }

    public void showFor(Node threeDObject, int defaultPlanViewImageWidth, int defaultPlanViewImageHeight, Window owner) {
        mTakeSnapshotControl.setObject(threeDObject, 400, defaultPlanViewImageWidth, defaultPlanViewImageHeight);
        showAndWait();
    }
}
