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
package de.dh.utils.fx;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public abstract class ListCellWithButton<T> extends ListCell<T> {
    protected HBox mHBox = new HBox();
    protected Label mLabel = new Label();
    protected Pane mSpacer = new Pane();
    protected Button mButton = new Button();

    public ListCellWithButton(Node buttonGraphic) {
        super();
        mHBox.getChildren().addAll(mLabel, mSpacer, mButton);
        HBox.setHgrow(mSpacer, Priority.ALWAYS);
        if (buttonGraphic != null) {
            mButton.setGraphic(buttonGraphic);
        }
        mButton.setOnAction(this::onButtonAction);
    }

    protected abstract void onButtonAction(ActionEvent actionEvent);

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);  // No text in label of super class
        if (empty) {
            setGraphic(null);
        } else {
            mLabel.setText(getLabel(item));
            setGraphic(mHBox);
        }
    }

    protected abstract String getLabel(T item);
}