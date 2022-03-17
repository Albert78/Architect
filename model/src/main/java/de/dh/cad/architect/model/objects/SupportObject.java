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
package de.dh.cad.architect.model.objects;

import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.dh.cad.architect.model.ChangeSet;
import de.dh.cad.architect.model.assets.AssetRefPath;
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
    public void initializeSurfaces() {
        clearSurfaces();
        // Will be done in a separate call to #initializeSurfaces(Collection<String>) by creator
    }

    public static SupportObject create(String name, Position2D position, Dimensions2D size, Length height, float rotationDeg, Length elevation,
        Set<String> surfaceNames, IObjectsContainer ownerContainer, ChangeSet changeSet) {
        SupportObject result = new SupportObject(IdGenerator.generateUniqueId(SupportObject.class), name, size, height, rotationDeg, elevation);
        ownerContainer.addOwnedChild_Internal(result, changeSet);
        result.createAnchor(AP_POSITION, position, changeSet);
        result.initializeSurfaces(surfaceNames);

        return result;
    }

    public void initializeSurfaces(Collection<String> surfaceTypeIds) {
        clearSurfaces();
        for (String typeId : surfaceTypeIds) {
            createSurface(typeId);
        }
    }

    @XmlElement(name = "DescriptorRef")
    @XmlJavaTypeAdapter(AssetRefPathJavaTypeAdapter.class)
    public AssetRefPath getSupportObjectDescriptorRef() {
        return mSupportObjectDescriptorRef;
    }

    public void setSupportObjectDescriptorRef(AssetRefPath value) {
        mSupportObjectDescriptorRef = value;
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
    @XmlJavaTypeAdapter(Dimensions2DJavaTypeAdapter.class)
    @XmlElement(name = "Size")
    public Dimensions2D getSize() {
        return mSize;
    }

    public void setSize(Dimensions2D value) {
        mSize = value;
    }

    /**
     * Gets the rotation of this support object in clockwise direction in X/Y plane.
     */
    @XmlElement(name = "RotationDeg")
    public float getRotationDeg() {
        return mRotationDeg;
    }

    public void setRotationDeg(float value) {
        mRotationDeg = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "Height")
    public Length getHeight() {
        return mHeight;
    }

    public void setHeight(Length value) {
        mHeight = value;
    }

    @XmlJavaTypeAdapter(LengthJavaTypeAdapter.class)
    @XmlElement(name = "Elevation")
    public Length getElevation() {
        return mElevation;
    }

    public void setElevation(Length value) {
        mElevation = value;
    }

    @Override
    protected String attrsToString() {
        return super.attrsToString() +
                        ", Position = " + getAnchorByAnchorType(AP_POSITION).getPosition() +
                        "; Size = " + mSize +
                        "; Rotation (Deg) = " + mRotationDeg +
                        "; Elevation = " + mElevation;
    }
}
