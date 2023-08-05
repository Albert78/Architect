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
package de.dh.cad.architect.model.objects;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.changes.ObjectModificationChange;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.coords.Position2D;
import de.dh.cad.architect.model.jaxb.AssetRefPathJavaTypeAdapter;
import de.dh.cad.architect.model.jaxb.Dimensions2DJavaTypeAdapter;
import de.dh.cad.architect.model.jaxb.LengthJavaTypeAdapter;
import de.dh.cad.architect.utils.IdGenerator;

public class SupportObject extends BaseSolidObject {
    protected Dimensions2D mSize;
    protected float mRotationDeg;
    protected Length mHeight;
    protected Length mElevation;

    protected AssetRefPath mSupportObjectDescriptorRef;

    public SupportObject() {
        // For JAXB
    }

    /**
     * Position handle anchor, represents the center of this support object.
     */
    public static final String AP_POSITION = "Position";

    public SupportObject(String id, String name, Dimensions2D size, Length height, float rotationDeg, Length elevation) {
        super(id, name);
        mSize = size;
        mHeight = height;
        mRotationDeg = rotationDeg;
        mElevation = elevation;
    }

    @Override
    protected void initializeSurfaces(List<IModelChange> changeTrace) {
        clearSurfaces(changeTrace);
        // Will be done in a separate call to #initializeSurfaces(Collection<String>) by creator
    }

    public static SupportObject create(String name, AssetRefPath supportObjectDescriptorRef,
        Position2D position, Dimensions2D size, Length height, float rotationDeg, Length elevation,
        Set<String> surfaceNames, IObjectsContainer ownerContainer, List<IModelChange> changeTrace) {
        SupportObject result = new SupportObject(IdGenerator.generateUniqueId(SupportObject.class), name, size, height, rotationDeg, elevation);
        result.setSupportObjectDescriptorRef(supportObjectDescriptorRef, changeTrace);
        ownerContainer.addOwnedChild_Internal(result, changeTrace);
        result.createAnchor(AP_POSITION, position, changeTrace);
        result.initializeSurfaces(surfaceNames, changeTrace);

        return result;
    }

    public void initializeSurfaces(Collection<String> surfaceTypeIds, List<IModelChange> changeTrace) {
        clearSurfaces(changeTrace);
        for (String surfaceTypeId : surfaceTypeIds) {
            createSurface(surfaceTypeId, changeTrace);
        }
    }

    @XmlTransient
    public AssetRefPath getSupportObjectDescriptorRef() {
        return mSupportObjectDescriptorRef;
    }

    public void setSupportObjectDescriptorRef(AssetRefPath value, List<IModelChange> changeTrace) {
        AssetRefPath oldDescriptorRef = mSupportObjectDescriptorRef;
        mSupportObjectDescriptorRef = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setSupportObjectDescriptorRef(oldDescriptorRef, changeTrace);
            }
        });
    }

    /**
     * Returns the (main) handle anchor of this support object.
     * The handle anchor is located in the middle of the support object. The support object is rotated by {@link #getRotationDeg()} degrees.
     */
    @XmlTransient
    public Anchor getHandleAnchor() {
        return getAnchorByAnchorType(AP_POSITION);
    }

    @Override
    public boolean isHandle(Anchor anchor) {
        return AP_POSITION.equals(anchor.getAnchorType());
    }

    public Position2D getCenterPoint() {
        return getHandleAnchor().getPosition().projectionXY();
    }

    /**
     * Gets the size this support object takes in the plan in X/Y direction.
     * The 3D model will be scaled from its native size to to this size.
     */
    @XmlTransient
    public Dimensions2D getSize() {
        return mSize;
    }

    public void setSize(Dimensions2D value, List<IModelChange> changeTrace) {
        Dimensions2D oldSize = mSize;
        mSize = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public Optional<IModelChange> tryMerge(IModelChange oldChange) {
                if (getClass().equals(oldChange.getClass())) {
                    return Optional.of(oldChange);
                }
                return Optional.empty();
            }

            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setSize(oldSize, undoChangeTrace);
            }
        });
    }

    /**
     * Gets the rotation of this support object in clockwise direction in X/Y plane.
     */
    @XmlTransient
    public float getRotationDeg() {
        return mRotationDeg;
    }

    public void setRotationDeg(float value, List<IModelChange> changeTrace) {
        float oldRotationDeg = mRotationDeg;
        mRotationDeg = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public Optional<IModelChange> tryMerge(IModelChange oldChange) {
                if (getClass().equals(oldChange.getClass())) {
                    return Optional.of(oldChange);
                }
                return Optional.empty();
            }

            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setRotationDeg(oldRotationDeg, undoChangeTrace);
            }
        });
    }

    @XmlTransient
    public Length getHeight() {
        return mHeight;
    }

    public void setHeight(Length value, List<IModelChange> changeTrace) {
        Length oldHeight = mHeight;
        mHeight = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public Optional<IModelChange> tryMerge(IModelChange oldChange) {
                if (getClass().equals(oldChange.getClass())) {
                    return Optional.of(oldChange);
                }
                return Optional.empty();
            }

            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setHeight(oldHeight, undoChangeTrace);
            }
        });
    }

    @XmlTransient
    public Length getElevation() {
        return mElevation;
    }

    public void setElevation(Length value, List<IModelChange> changeTrace) {
        Length oldElevation = mElevation;
        mElevation = value;
        changeTrace.add(new ObjectModificationChange(this) {
            @Override
            public Optional<IModelChange> tryMerge(IModelChange oldChange) {
                if (getClass().equals(oldChange.getClass())) {
                    return Optional.of(oldChange);
                }
                return Optional.empty();
            }

            @Override
            public void undo(List<IModelChange> undoChangeTrace) {
                setElevation(oldElevation, undoChangeTrace);
            }
        });
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() +
                        ", Position = " + getAnchorByAnchorType(AP_POSITION).getPosition() +
                        "; Size = " + mSize +
                        "; Rotation (Deg) = " + mRotationDeg +
                        "; Elevation = " + mElevation;
    }

    @XmlElement(name = "DescriptorRef")
    @XmlJavaTypeAdapter(AssetRefPathJavaTypeAdapter.class)
    public AssetRefPath getSupportObjectDescriptorRef_JAXB() {
        return mSupportObjectDescriptorRef;
    }

    public void setSupportObjectDescriptorRef_JAXB(AssetRefPath value) {
        mSupportObjectDescriptorRef = value;
    }

    @XmlJavaTypeAdapter(Dimensions2DJavaTypeAdapter.class)
    @XmlElement(name = "Size")
    public Dimensions2D getSize_JAXB() {
        return mSize;
    }

    public void setSize_JAXB(Dimensions2D value) {
        mSize = value;
    }

    @XmlElement(name = "RotationDeg")
    public float getRotationDeg_JAXB() {
        return mRotationDeg;
    }

    public void setRotationDeg_JAXB(float value) {
        mRotationDeg = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "Height")
    public Length getHeight_JAXB() {
        return mHeight;
    }

    public void setHeight_JAXB(Length value) {
        mHeight = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "Elevation")
    public Length getElevation_JAXB() {
        return mElevation;
    }

    public void setElevation_JAXB(Length value) {
        mElevation = value;
    }
}
