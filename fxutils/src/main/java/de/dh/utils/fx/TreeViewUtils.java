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
package de.dh.utils.fx;

import java.util.Objects;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;

public class TreeViewUtils {
    public static void expandTreeItem(TreeItem<?> item) {
        TreeItem<?> ti = item;
        while (ti != null) {
            TreeItem<?> parent = ti.getParent();
            if (parent == null) {
                break;
            }
            parent.setExpanded(true);
            ti = parent;
        }
    }

    public static void expandTreeItemRecursive(TreeItem<Object> item) {
        item.setExpanded(true);
        for (TreeItem<Object> child : item.getChildren()) {
            expandTreeItemRecursive(child);
        }
    }

    public static <T> void addSelection(TreeTableView<T> treeTableView, TreeItem<T> item) {
        expandTreeItem(item);
        int index = treeTableView.getRow(item);
        treeTableView.getSelectionModel().select(index);
    }

    public static <T> void showAndAddSelection(TreeTableView<T> treeTableView, TreeItem<T> item) {
        expandTreeItem(item);
        int index = treeTableView.getRow(item);
        TreeTableViewSelectionModel<T> selectionModel = treeTableView.getSelectionModel();
        // Suppress select events if selected index doesn't change; this is usually the desired behavior,
        // for example to break infinite recursive selection callbacks. Furthermore, the user doesn't
        // want the TreeView to jump to the selected position if it didn't change.
        if (selectionModel.getSelectedIndex() != index) {
            selectionModel.select(index);
        }
        treeTableView.scrollTo(index);
    }

    public static <T> TreeItem<T> findItemOfValue(TreeItem<T> root, T value) {
        if (Objects.equals(value, root.getValue())) {
            return root;
        }
        for (TreeItem<T> child : root.getChildren()) {
            TreeItem<T> res = findItemOfValue(child, value);
            if (res != null) {
                return res;
            }
        }
        return null;
    }
}
