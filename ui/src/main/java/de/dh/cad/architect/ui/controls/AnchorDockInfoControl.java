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

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.utils.fx.TreeViewUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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

        mDockTree.setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = new ContextMenu();
            ObservableList<MenuItem> resultItems = contextMenu.getItems();
            Collection<MenuItem> items = buildContextMenu();
            if (items.isEmpty()) {
                return;
            }
            resultItems.addAll(items);
            // This does not work because the Control's context menu is shown before this OnContextMenuRequested event is fired:
            //mDockTree.setContextMenu(contextMenu);
            // ... so we manage the context menu manually:
            contextMenu.show(getScene().getWindow(), event.getScreenX(), event.getSceneY()); // Use this method; don't provide the tree table as anchor, else, auto closing doesn't work
        });

        setCenter(mDockTree);

        Label hintLabel = new Label(Strings.ANCHOR_DOCK_INFO_HINT_LABEL);
        hintLabel.setPadding(new Insets(5));
        hintLabel.setWrapText(true);
        setBottom(hintLabel);
    }

    protected Collection<MenuItem> buildContextMenu() {
        Anchor selectedAnchor = getSelectedAnchor();
        boolean dockMasterPresent = selectedAnchor.getDockMaster().isPresent();
        boolean dockSlavesPresent = !selectedAnchor.getDockSlaves().isEmpty();
        MenuItem undockFromOwnerItem = new MenuItem(Strings.ACTION_ANCHOR_UNDOCK_FROM_MASTER);
        undockFromOwnerItem.setDisable(!dockMasterPresent);
        undockFromOwnerItem.setOnAction(event -> {
            mUiController.undockFromMasterIfDocked(selectedAnchor);
        });
        MenuItem removeFromDockItem = new MenuItem(Strings.ACTION_ANCHOR_REMOVE_FROM_DOCK);
        removeFromDockItem.setDisable(!dockMasterPresent && !dockSlavesPresent);
        removeFromDockItem.setOnAction(event -> {
            mUiController.removeAnchorFromDock(selectedAnchor);
        });
        MenuItem undockAllChildrenItem = new MenuItem(Strings.ACTION_ANCHOR_UNDOCK_ALL_CHILDREN);
        undockAllChildrenItem.setDisable(!dockSlavesPresent);
        undockAllChildrenItem.setOnAction(event -> {
            mUiController.undockAllChildren(selectedAnchor);
        });
        return Arrays.asList(undockFromOwnerItem, removeFromDockItem, undockAllChildrenItem);
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
        TreeItem<Anchor> rootItem = buildItem(rootMaster);
        mDockTree.setRoot(rootItem);
        TreeItem<Anchor> selectAnchorItem = TreeViewUtils.findItemOfValue(rootItem, anchor);
        TreeViewUtils.showAndAddSelection(mDockTree, selectAnchorItem);
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
