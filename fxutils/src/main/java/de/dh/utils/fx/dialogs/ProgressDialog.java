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
package de.dh.utils.fx.dialogs;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressDialog {
    private final Stage mDialogStage;
    private final ProgressBar mProgressBar = new ProgressBar();

    public ProgressDialog(String title, javafx.stage.Window ownerWindow) {
        mDialogStage = new Stage();
        mDialogStage.initOwner(ownerWindow);
        mDialogStage.initStyle(StageStyle.UTILITY);
        mDialogStage.setResizable(false);
        mDialogStage.initModality(Modality.WINDOW_MODAL);
        mDialogStage.setOnCloseRequest(event -> {
            // Disable close
            event.consume();
        });

        // PROGRESS BAR
        mProgressBar.setProgress(-1F);
        mProgressBar.setPrefWidth(200);

        BorderPane pane = new BorderPane(mProgressBar);

        Label titleLabel = new Label(title);
        titleLabel.setPadding(new Insets(5, 10, 5, 10));
        titleLabel.setStyle("-fx-font-weight: bold");
        pane.setTop(titleLabel);

        Scene scene = new Scene(pane);
        mDialogStage.setScene(scene);

        mDialogStage.setHeight(60);
        mDialogStage.setWidth(250);
        mDialogStage.setResizable(false);
        mDialogStage.initStyle(StageStyle.UNDECORATED);
    }

    public void start(Task<?> task)  {
        mProgressBar.progressProperty().bind(task.progressProperty());
        EventHandler<WorkerStateEvent> closeHandler = event -> {
            close();
        };
        task.setOnCancelled(closeHandler);
        task.setOnFailed(closeHandler);
        task.setOnSucceeded(closeHandler);
        new Thread(task).start();
        mDialogStage.show();
    }

    public void close() {
        mDialogStage.close();
    }
}