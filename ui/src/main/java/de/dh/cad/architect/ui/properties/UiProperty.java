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
package de.dh.cad.architect.ui.properties;

import java.util.function.Supplier;

import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.utils.YesNoStringConverter;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public abstract class UiProperty<T> {
    protected static class SimpleStringConverter extends StringConverter<Object> {
        @Override
        public String toString(Object obj) {
            return String.valueOf(obj);
        }

        @Override
        public Object fromString(String string) {
            return string;
        }
    }

    public enum PropertyType {
        YesNo(() -> new PropertyBooleanControlProvider(new YesNoStringConverter())),
        String(() -> new PropertyTextControlProvider<>((StringConverter<?>) new SimpleStringConverter())),
        Integer(() -> new PropertyTextControlProvider<>((StringConverter<?>) new IntegerStringConverter())),
        Double(() -> new PropertyTextControlProvider<>((StringConverter<?>) new DoubleStringConverter())),
        Length(() -> new PropertyLengthControlProvider(Constants.LOCALIZED_NUMBER_FORMAT)),
        IPosition(() -> new PropertyIPositionControlProvider(Constants.LOCALIZED_NUMBER_FORMAT)),
        Dimensions2DXY(() -> new PropertyDimensions2DControlProvider(Constants.LOCALIZED_NUMBER_FORMAT, Strings.PROPERTY_DIMENSIONS_WIDTH, Strings.PROPERTY_DIMENSIONS_DEPTH)),
        Dimensions2DXZ(() -> new PropertyDimensions2DControlProvider(Constants.LOCALIZED_NUMBER_FORMAT, Strings.PROPERTY_DIMENSIONS_WIDTH, Strings.PROPERTY_DIMENSIONS_HEIGHT)),
        Dimensions3D(() -> new PropertyDimensions3DControlProvider(Constants.LOCALIZED_NUMBER_FORMAT)),
        WallBevelType(() -> new PropertyEnumControlProvider<>(de.dh.cad.architect.model.wallmodel.WallBevelType.values(), Strings.WALL_BEVEL_TYPE_TITLE_PROVIDER)),
        WallDockEnd(() -> new PropertyEnumControlProvider<>(de.dh.cad.architect.model.wallmodel.WallDockEnd.values()));

        private final Supplier<IPropertyControlProvider<?>> mPropertyControlProviderSupplier;

        private PropertyType(Supplier<IPropertyControlProvider<?>> propertyControlProviderSupplier) {
            mPropertyControlProviderSupplier = propertyControlProviderSupplier;
        }

        protected IPropertyControlProvider<?> createPropertyControlProvider() {
            return mPropertyControlProviderSupplier.get();
        }
    }

    protected final BaseObject mOwner;
    protected final String mKey;
    protected final String mDisplayName;
    protected final PropertyType mType;
    protected final boolean mEditable;

    public UiProperty(BaseObject owner, String key, String displayName, PropertyType type, boolean editable) {
        mOwner = owner;
        mKey = key;
        mDisplayName = displayName;
        mType = type;
        mEditable = editable;
    }

    public BaseObject getOwner() {
        return mOwner;
    }

    public String getKey() {
        return mKey;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public PropertyType getType() {
        return mType;
    }

    public boolean isEditable() {
        return mEditable;
    }

    public Property<T> valueProperty() {
        return new SimpleObjectProperty<>(mOwner, mKey, getValue());
    }

    public abstract T getValue();

    // Using Object as parameter type is a hack to allow setting the value when type argument T is not known
    public abstract void setValue(Object value);

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [Key=" + mKey + ", Value=<" + valueProperty().getValue() + ">]";
    }
}
