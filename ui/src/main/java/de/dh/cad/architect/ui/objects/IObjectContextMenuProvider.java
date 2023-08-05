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
package de.dh.cad.architect.ui.objects;

import java.util.Optional;
import java.util.function.Consumer;

import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public interface IObjectContextMenuProvider<T> {
    public static class MenuItemData<T> {
        protected final String mMenuItemTitle;
        protected final Consumer<T> mMenuItemClicked;

        public MenuItemData(String menuItemTitle, Consumer<T> menuItemClicked) {
            mMenuItemTitle = menuItemTitle;
            mMenuItemClicked = menuItemClicked;
        }

        public String getMenuItemTitle() {
            return mMenuItemTitle;
        }

        public Consumer<T> getMenuItemClicked() {
            return mMenuItemClicked;
        }
    }

    ContextMenu getContextMenu();
    ObservableList<MenuItem> getItemsInsertPoint();
    Optional<MenuItemData<T>> getObjectMenuItemData(T object);
}
