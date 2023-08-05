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

import java.io.IOException;
import java.util.List;

import de.dh.cad.architect.model.assets.SupportObjectDescriptor;
import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.coords.Vector2D;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.controller.UiController;

public abstract class SupportObjectTemplate {
    public abstract SupportObjectDescriptor getSupportObjectDescriptor();

    public abstract Dimensions2D getDimensions();

    public Vector2D getPositionOffset() {
        return Vector2D.zero();
    }

    public float getRotationDeg() {
        return 0;
    }

    public abstract SupportObject createSupportObject(Position2D pos, UiController uiController, List<IModelChange> changeTrace) throws IOException;

    public static SupportObjectTemplate createNew(SupportObjectDescriptor descriptor) {
        return new SupportObjectTemplate() {
            @Override
            public SupportObjectDescriptor getSupportObjectDescriptor() {
                return descriptor;
            }

            @Override
            public Dimensions2D getDimensions() {
                return new Dimensions2D(descriptor.getWidth(), descriptor.getDepth());
            }

            @Override
            public SupportObject createSupportObject(Position2D pos, UiController uiController, List<IModelChange> changeTrace) throws IOException {
                return uiController.doCreateNewSupportObject(descriptor, pos.plus(getPositionOffset()), changeTrace);
            }
        };
    }

    public static SupportObjectTemplate copyOf(SupportObject origSO, AssetManager assetManager) throws IOException {
        return copyOf(origSO, origSO.getCenterPoint(), assetManager);
    }

    public static SupportObjectTemplate copyOf(SupportObject origSO, Position2D copyCenter, AssetManager assetManager) throws IOException {
        SupportObjectDescriptor descriptor = assetManager.loadSupportObjectDescriptor(origSO.getSupportObjectDescriptorRef());
        Vector2D offset = origSO.getCenterPoint().minus(copyCenter);
        return new SupportObjectTemplate() {
            @Override
            public SupportObjectDescriptor getSupportObjectDescriptor() {
                return descriptor;
            }

            @Override
            public Dimensions2D getDimensions() {
                return origSO.getSize();
            }

            @Override
            public Vector2D getPositionOffset() {
                return offset;
            }

            @Override
            public float getRotationDeg() {
                return origSO.getRotationDeg();
            }

            @Override
            public SupportObject createSupportObject(Position2D pos, UiController uiController, List<IModelChange> changeTrace) throws IOException {
                SupportObject result = uiController.doCreateNewSupportObject(descriptor, pos.plus(getPositionOffset()), changeTrace);
                copyValues(result, changeTrace);
                return result;
            }

            protected void copyValues(SupportObject targetSupportObject, List<IModelChange> changeTrace) {
                targetSupportObject.setSize(origSO.getSize(), changeTrace);
                targetSupportObject.setRotationDeg(origSO.getRotationDeg(), changeTrace);
                targetSupportObject.setHeight(origSO.getHeight(), changeTrace);
                targetSupportObject.setElevation(origSO.getElevation(), changeTrace);
            }
        };
    }
}
