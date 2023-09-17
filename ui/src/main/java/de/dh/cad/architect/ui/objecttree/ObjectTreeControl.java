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
package de.dh.cad.architect.ui.objecttree;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.controller.IObjectContextMenuProvider;
import de.dh.utils.fx.TreeViewUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class ObjectTreeControl extends BorderPane {
    public static interface IObjectsVisibilityChanger {
        void setObjectsVisibility(Collection<? extends BaseObject> objs, boolean hidden);
    }

    public static final String FXML = "ObjectTreeControl.fxml";

    protected static final URL ICON_VISIBLE = ObjectTreeControl.class.getResource("visible.png");
    protected static final URL ICON_INVISIBLE = ObjectTreeControl.class.getResource("invisible.png");

    public static final Image VISIBLE_ICON = new Image(ICON_VISIBLE.toString());
    public static final Image INVISIBLE_ICON = new Image(ICON_INVISIBLE.toString());

    @FXML
    protected TreeTableView<ITreeItemData> mTreeTableView;

    @FXML
    protected TreeTableColumn<ITreeItemData, String> mTitleColumn;

    @FXML
    protected TreeTableColumn<ITreeItemData, ITreeItemData> mVisibleColumn;

    @FXML
    protected TreeTableColumn<ITreeItemData, String> mNameColumn;

    @FXML
    protected TreeTableColumn<ITreeItemData, String> mIdColumn;

    @FXML
    protected TreeTableColumn<ITreeItemData, String> mInfoColumn;

    protected final ObservableList<String> mSelectedObjectIds = FXCollections.observableArrayList();
    protected final List<IObjectContextMenuProvider> mContextMenuProviders = new ArrayList<>();
    protected IObjectsVisibilityChanger mObjectsVisibilityChanger = null;

    protected RootTreeItem mRootTreeItem = null;

    protected ObjectTreeControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(ObjectTreeControl.class.getResource(FXML));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initialize();
    }

    public static ObjectTreeControl create() {
        return new ObjectTreeControl();
    }

    public void setObjectsVisibilityChanger(IObjectsVisibilityChanger value) {
        mObjectsVisibilityChanger = value;
    }

    public TreeTableView<ITreeItemData> getTreeTableView() {
        return mTreeTableView;
    }

    protected int mSuppressSelectionChanges = 0;

    protected void initialize() {
        TreeTableViewSelectionModel<ITreeItemData> ttvSelectionModel = mTreeTableView.getSelectionModel();
        ObservableList<TreeItem<ITreeItemData>> selectedItems = ttvSelectionModel.getSelectedItems();

        mTitleColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<ITreeItemData, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<ITreeItemData, String> param) {
                ITreeItemData obj = param.getValue().getValue();
                return obj == null ? new SimpleStringProperty() : new SimpleStringProperty(obj.getTitle());
            }
        });
        mVisibleColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<ITreeItemData, ITreeItemData>, ObservableValue<ITreeItemData>>() {
            @Override
            public ObservableValue<ITreeItemData> call(CellDataFeatures<ITreeItemData, ITreeItemData> param) {
                return new SimpleObjectProperty<>(param.getValue().getValue());
            }
        });
        mVisibleColumn.setCellFactory(column -> new TreeTableCell<>() {
            private final ImageView mImageView;

            {
                mImageView = new ImageView();
                mImageView.setFitWidth(20);
                mImageView.setFitHeight(20);
                setGraphic(mImageView);
                addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        @SuppressWarnings("unchecked")
                        TreeTableCell<ITreeItemData, ITreeItemData> cell = (TreeTableCell<ITreeItemData, ITreeItemData>) event.getSource();
                        ITreeItemData tid = cell.getItem();
                        if (tid == null || tid.isVisible().isEmpty()) {
                            return;
                        }
                        setVisibility(tid.getObjects(), tid.isVisible().get());
                    }
                });
            }

            @Override
            protected void updateItem(ITreeItemData tid, boolean empty) {
                super.updateItem(tid, empty);
                if (tid == null || tid.isVisible().isEmpty()) {
                    mImageView.setVisible(false);
                    return;
                }
                mImageView.setVisible(true);
                if (tid.isVisible().get()) {
                    mImageView.setImage(VISIBLE_ICON);
                } else {
                    mImageView.setImage(INVISIBLE_ICON);
                }
            }
        });
        mNameColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<ITreeItemData, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<ITreeItemData, String> param) {
                ITreeItemData obj = param.getValue().getValue();
                if (obj instanceof IObjectTreeItemData otid) {
                    String name = otid.getObject().getName();
                    if (!StringUtils.isEmpty(name)) {
                        return new SimpleStringProperty(name);
                    }
                }
                return new SimpleStringProperty();
            }
        });
        mIdColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<ITreeItemData, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<ITreeItemData, String> param) {
                ITreeItemData obj = param.getValue().getValue();
                if (obj instanceof IIdObjectTreeItemData tidid) {
                    return new SimpleStringProperty(tidid.getId());
                }
                return new SimpleStringProperty();
            }
        });
        mInfoColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<ITreeItemData, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<ITreeItemData, String> param) {
                ITreeItemData obj = param.getValue().getValue();
                if (obj instanceof IObjectTreeItemData otid) {
                    BaseObject object = otid.getObject();
                    if (object instanceof Anchor a) {
                        String anchorType = a.getAnchorType();
                        if (!StringUtils.isEmpty(anchorType)) {
                            return new SimpleStringProperty(anchorType);
                        }
                    }
                }
                return new SimpleStringProperty();
            }
        });

        mTreeTableView.setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = new ContextMenu();
            ObservableList<MenuItem> resultItems = contextMenu.getItems();
            resultItems.clear();
            for (IObjectContextMenuProvider menuProvider : mContextMenuProviders) {
                Collection<MenuItem> items = menuProvider.getMenuItems(mSelectedObjectIds);
                if (items == null) {
                    continue;
                }
                resultItems.addAll(items);
            }
            // This does not work because the Control's context menu is shown before this OnContextMenuRequested event is fired:
            //mTreeTableView.setContextMenu(contextMenu);
            // ... so we manage the context menu manually:
            contextMenu.show(getScene().getWindow(), event.getScreenX(), event.getSceneY()); // Use this method; don't provide the tree table as anchor, else, auto closing doesn't work
        });

        mTreeTableView.setOnKeyTyped(event -> {
            if (" ".equals(event.getCharacter())) {
                event.consume();
                TreeItem<ITreeItemData> selectedItem = ttvSelectionModel.getSelectedItem();
                if (selectedItem == null) {
                    return;
                }
                ITreeItemData tid = selectedItem.getValue();
                if (tid.isVisible().isEmpty()) {
                    return;
                }
                boolean selectedObjectVisible = tid.isVisible().get();
                setVisibility(getObjects(selectedItems), selectedObjectVisible);
            }
        });
        mTreeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ttvSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        // Reflect changes from tree selection to selected objects list
        selectedItems.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends TreeItem<ITreeItemData>> c) {
                updateSelectedObjectIdsFromTreeViewSelection();
            }
        });

        // Reflect changes from selected objects list to tree selection
        mSelectedObjectIds.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends String> c) {
                updateTreeViewSelectionFromSelectedObjectIds();
            }
        });
    }

    protected void updateTreeViewSelectionFromSelectedObjectIds() {
        if (mSuppressSelectionChanges > 0) {
            // Break the chain of recursive change handler calls objects tree -> mSelectedObjectIds -> objects tree -> ...
            return;
        }
        mSuppressSelectionChanges++;
        try {
            // setAll method is not supported on the selected items list:
            //selectedItems.setAll(getTreeItems(mSelectedObjectIdsList));
            mTreeTableView.getSelectionModel().clearSelection();
            mSelectedObjectIds
                .stream()
                .flatMap(id -> mRootTreeItem.getTreeItemsByObjectId(id).stream())
                .forEach(item -> {
                    if (mSelectedObjectIds.size() > 1) {
                        TreeViewUtils.addSelection(mTreeTableView, item);
                    } else {
                        TreeViewUtils.showAndAddSelection(mTreeTableView, item);
                    }
                });
        } finally {
            mSuppressSelectionChanges--;
        }
    }

    protected void updateSelectedObjectIdsFromTreeViewSelection() {
        if (mSuppressSelectionChanges > 0) {
            // Break the chain of recursive change handler calls objects tree -> mSelectedObjectIds -> objects tree -> ...
            return;
        }
        mSuppressSelectionChanges++;
        try {
            ObservableList<TreeItem<ITreeItemData>> selectedItems = mTreeTableView.getSelectionModel().getSelectedItems();
            mSelectedObjectIds.setAll(getObjectIds(selectedItems));
        } finally {
            mSuppressSelectionChanges--;
        }
    }

    protected void setVisibility(Collection<? extends BaseObject> objects, boolean hidden) {
        mObjectsVisibilityChanger.setObjectsVisibility(objects, hidden);
    }

    protected Collection<BaseObject> getObjects(Collection<? extends TreeItem<ITreeItemData>> treeItems) {
        return treeItems
                        .stream()
                        .map(ti -> ti.getValue())
                        .filter(d -> d instanceof IObjectTreeItemData)
                        .map(d -> ((IObjectTreeItemData) d).getObject())
                        .collect(Collectors.toSet());
    }

    protected Collection<String> getObjectIds(Collection<? extends TreeItem<ITreeItemData>> treeItems) {
        return treeItems
                        .stream()
                        .map(ti -> ti.getValue())
                        .filter(d -> d instanceof IIdObjectTreeItemData)
                        .map(d -> ((IIdObjectTreeItemData) d).getId())
                        .sorted()
                        .distinct()
                        .collect(Collectors.toSet());
    }

    protected Collection<TreeItem<ITreeItemData>> getTreeItems(Collection<String> objectIds) {
        return objectIds
                        .stream()
                        .flatMap(id -> mRootTreeItem.getTreeItemsByObjectId(id).stream())
                        .collect(Collectors.toList());
    }

    public void setPlaceholder(String text) {
        mTreeTableView.setPlaceholder(new Label(text));
    }

    public void setInput(Plan plan) {
        mRootTreeItem = new RootTreeItem(plan);
        mTreeTableView.setRoot(mRootTreeItem);
        mRootTreeItem.setExpanded(true);
    }

    // Using an ObservableList here instead of an ObservableSet to prevent performance issues with the inperformant SetChangeListener
    public ObservableList<String> selectedObjectIds() {
        return mSelectedObjectIds;
    }

    public void objectsRemoved(Collection<BaseObject> removedObjects) {
        mSuppressSelectionChanges++;
        try {
            mRootTreeItem.objectsRemoved(removedObjects);
        } finally {
            mSuppressSelectionChanges--;
        }
        updateSelectedObjectIdsFromTreeViewSelection();
    }

    public void objectsChanged(Collection<BaseObject> changedObjects) {
        mSuppressSelectionChanges++;
        try {
            mRootTreeItem.objectsChanged(changedObjects);
        } finally {
            mSuppressSelectionChanges--;
        }
        updateSelectedObjectIdsFromTreeViewSelection();
    }

    public void objectsAdded(Collection<BaseObject> addedObjects) {
        mSuppressSelectionChanges++;
        try {
            mRootTreeItem.objectsAdded(addedObjects);
        } finally {
            mSuppressSelectionChanges--;
        }
        updateSelectedObjectIdsFromTreeViewSelection();
    }

    public void setContextMenuProviders(List<IObjectContextMenuProvider> contextMenuProviders) {
        mContextMenuProviders.clear();
        mContextMenuProviders.addAll(contextMenuProviders);
    }
}
