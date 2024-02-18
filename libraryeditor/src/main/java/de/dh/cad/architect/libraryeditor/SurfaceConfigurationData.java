package de.dh.cad.architect.libraryeditor;

import java.util.Objects;

import de.dh.cad.architect.model.assets.AssetRefPath;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class SurfaceConfigurationData {
    protected final String mSurfaceTypeId;
    protected final BooleanProperty mFocusedProperty = new SimpleBooleanProperty(false);
    protected final ObjectProperty<AssetRefPath> mMaterialRefProperty = new SimpleObjectProperty<>(null);
    protected final BooleanProperty mInUseProperty = new SimpleBooleanProperty(true);

    public SurfaceConfigurationData(String surfaceTypeId, boolean inUse) {
        mSurfaceTypeId = surfaceTypeId;
    }

    public String getSurfaceTypeId() {
        return mSurfaceTypeId;
    }

    public BooleanProperty getFocusedProperty() {
        return mFocusedProperty;
    }

    public boolean isFocused() {
        return mFocusedProperty.get();
    }

    public void setFocused(boolean value) {
        mFocusedProperty.set(value);
    }

    public ObjectProperty<AssetRefPath> getMaterialRefProperty() {
        return mMaterialRefProperty;
    }

    public AssetRefPath getMaterialRef() {
        return mMaterialRefProperty.get();
    }

    public void setMaterialRef(AssetRefPath value) {
        mMaterialRefProperty.set(value);
    }

    /**
     * Gets the information if this object contains any (non-trivial) configuration (which is worth to be preserved).
     */
    public boolean containsConfiguredValues() {
        return getMaterialRef() != null;
    }

    public BooleanProperty getInUseProperty() {
        return mInUseProperty;
    }

    /**
     * Gets the information if this configuration is currently used by a 3D object mesh.
     * Configurations which are not in use but which possibly contain important configuration data can be preserved until they are used again.
     */
    public boolean isInUse() {
        return mInUseProperty.get();
    }

    public void setInUse(boolean value) {
        mInUseProperty.set(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSurfaceTypeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SurfaceConfigurationData other = (SurfaceConfigurationData) obj;
        return Objects.equals(mSurfaceTypeId, other.mSurfaceTypeId);
    }

    @Override
    public String toString() {
        return mSurfaceTypeId;
    }
}
