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
package de.dh.cad.architect.ui.controls;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.utils.fx.TreeViewUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class AnchorDockInfoControl extends BorderPane {
    protected final UiController mUiController;
    protected final TreeTableView<Anchor> mDockTree;

    @SuppressWarnings("unchecked")
    public AnchorDockInfoControl(UiController uiController) {
        mUiController = uiController;
        TreeTableColumn<Anchor, String> ownerColumn = new TreeTableColumn<>(Strings.ANCHOR_DOCK_INFO_OWNER_COLUMN);
        ownerColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Anchor,String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Anchor, String> param) {
                return new SimpleStringProperty(BaseObjectUIRepresentation.getObjName(param.getValue().getValue().getAnchorOwner()));
            }
        });
        ownerColumn.setPrefWidth(250);

        mDockTree = new TreeTableView<>();
        TreeTableColumn<Anchor, String> nameColumn = new TreeTableColumn<>(Strings.ANCHOR_DOCK_INFO_ANCHOR_COLUMN);
        nameColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<Anchor,String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<Anchor, String> param) {
                return new SimpleStringProperty(BaseObjectUIRepresentation.getShortName(param.getValue().getValue()));
            }
        });
        nameColumn.setPrefWidth(200);

        mDockTree.getColumns().setAll(new TreeTableColumn[] {nameColumn, ownerColumn});
        TreeTableViewSelectionModel<Anchor> selectionModel = mDockTree.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<Anchor>> observable, TreeItem<Anchor> oldValue, TreeItem<Anchor> newValue) {
                if (newValue != null) {
                    Anchor newSelectedAnchor = newValue.getValue();
                    mUiController.setSelectedObjectId(newSelectedAnchor.getId());
                }
            }
        });

        mDockTree.setShowRoot(true);
        BorderPane.setAlignment(mDockTree, Pos.CENTER);

        setCenter(mDockTree);

        Label hintLabel = new Label(Strings.ANCHOR_DOCK_INFO_HINT_LABEL);
        hintLabel.setPadding(new Insets(5));
        hintLabel.setWrapText(true);
        setBottom(hintLabel);
    }

    public Anchor getRootAnchor() {
        TreeItem<Anchor> rootItem = mDockTree.getRoot();
        return rootItem == null ? null : rootItem.getValue();
    }

    public Anchor getSelectedAnchor() {
        TreeItem<Anchor> selectedItem = mDockTree.getSelectionModel().getSelectedItem();
        return selectedItem == null ? null : selectedItem.getValue();
    }

    public void updateForAnchor(Anchor anchor) {
        Anchor rootMaster = anchor.getRootMasterOfAnchorDock();
        Anchor selectedAnchor = getSelectedAnchor();
        TreeItem<Anchor> rootItem;
        if (!Objects.equals(rootMaster, getRootAnchor())) {
            rootItem = buildItem(rootMaster);
            mDockTree.setRoot(rootItem);
        } else {
            rootItem = mDockTree.getRoot();
        }
        TreeItem<Anchor> selectAnchorItem = TreeViewUtils.findItemOfValue(rootItem, anchor);
        Anchor selectAnchor = Optional.ofNullable(selectAnchorItem)
                .map(TreeItem::getValue)
                .orElse(null);
        if (!Objects.equals(selectedAnchor, selectAnchor)) {
            TreeViewUtils.showAndAddSelection(mDockTree, selectAnchorItem);
        }
    }

    protected TreeItem<Anchor> buildItem(Anchor anchor) {
        TreeItem<Anchor> result = new TreeItem<>(anchor);
        result.getChildren().addAll(anchor
            .getDockSlaves()
            .stream()
            .map(this::buildItem)
            .collect(Collectors.toList()));
        result.setExpanded(true);
        return result;
    }
}
