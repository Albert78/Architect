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
package de.dh.utils.fx.viewsfx;

import java.util.Optional;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class DockDragOperation {
    public static class DockDefinition {
        protected final IDockTarget mDockTarget;
        protected final Optional<IDockFeedback> mODockFeedback;

        public DockDefinition(IDockTarget dockTarget, Optional<IDockFeedback> oDockFeedback) {
            mDockTarget = dockTarget;
            mODockFeedback = oDockFeedback;
        }

        public IDockTarget getDockTarget() {
            return mDockTarget;
        }

        public Optional<IDockFeedback> getDockFeedback() {
            return mODockFeedback;
        }
    }

    protected final EventHandler<? super KeyEvent> mDockExitKeyListener;
    protected final Dockable<?> mDockable;

    protected Optional<DockDefinition> mOCurrentDockDefinition = Optional.empty();
    protected Scene mKeyListenerScene = null;

    public DockDragOperation(Dockable<?> dockable) {
        mDockable = dockable;

        mDockExitKeyListener = event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                DockSystem.finishDragOperation();
            }
        };

        // Actually, I want to register an application-global key listener to listen for the escape event to end the dock operation.
        // In JavaFX, this doesn't seem to be so easy, so for now, I'll register it on the scene
        installDockExitKeyListener(dockable.getView().getScene());
    }

    public void dispose() {
        removeDockFeedback();
        uninstallDockExitKeyListener();
    }

    public Dockable<?> getDockable() {
        return mDockable;
    }

    public Optional<DockDefinition> getCurrentDockDefinition() {
        return mOCurrentDockDefinition;
    }

    protected void installDockExitKeyListener(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, mDockExitKeyListener);
        mKeyListenerScene = scene;
    }

    protected void uninstallDockExitKeyListener() {
        if (mKeyListenerScene == null) {
            return;
        }
        mKeyListenerScene.removeEventFilter(KeyEvent.KEY_PRESSED, mDockExitKeyListener);
        mKeyListenerScene = null;
    }

    public void setCurrentDockTarget(IDockTarget target, Optional<IDockFeedback> oFeedback) {
        if (mOCurrentDockDefinition.isPresent()) {
            DockDefinition dockFeedback = mOCurrentDockDefinition.get();
            if (dockFeedback.getDockTarget().equals(target)) {
                return;
            }
            if (dockFeedback.getDockFeedback().isPresent()) {
                dockFeedback.getDockFeedback().get().uninstall();
            }
        }

        mOCurrentDockDefinition = Optional.of(new DockDefinition(target, oFeedback));
        if (oFeedback.isPresent()) {
            oFeedback.get().install();
        }
    }

    public void removeDockFeedback() {
        if (mOCurrentDockDefinition.isEmpty()) {
            return;
        }
        DockDefinition dockDefinition = mOCurrentDockDefinition.get();
        if (dockDefinition.getDockFeedback().isPresent()) {
            dockDefinition.getDockFeedback().get().uninstall();
        }
        mOCurrentDockDefinition = Optional.empty();
    }
}
