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
package de.dh.cad.architect.ui.properties;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.map.LinkedMap;

import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.ObjectProperties;
import de.dh.cad.architect.ui.properties.UiProperty.PropertyType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableColumn.CellEditEvent;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class PropertiesControl extends BorderPane {
    public class PropertyValueTableCell extends TreeTableCell<UiProperty<?>, Object> {
        protected Node mCellGraphic = null; // Contains the current cell graphic or null

        @SuppressWarnings("unchecked")
        protected <T> IPropertyControlProvider<T> createPropertyControlProvider(UiProperty<T> property) {
            if (property == null || property.getKey() == null) {
                return null;
            }
            PropertyType propertyType = property.getType();
            return (IPropertyControlProvider<T>) propertyType.createPropertyControlProvider();
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            if (empty || getTableRow() == null) {
                setGraphic(null);
            } else {
                doUpdateItem();
            }
        }

        protected void doUpdateItem() {
            UiProperty<?> property = getTableRow().getItem();
            IPropertyControlProvider<?> provider = createPropertyControlProvider(property);
            if (provider == null) {
                mCellGraphic = null;
            } else {
                if (isEditing()) {
                    mCellGraphic = provider.getEditor(this);
                } else {
                    mCellGraphic = provider.getView(this);
                }
            }
            setGraphic(mCellGraphic);
        }

        @Override
        public void startEdit() {
            UiProperty<?> property = getTableRow().getItem();
            if (!property.isEditable()) {
                return;
            }
            IPropertyControlProvider<?> provider = createPropertyControlProvider(property);
            if (provider == null) {
                return;
            }
            super.startEdit();

            if (isEditing()) {
                mCellGraphic = provider.getEditor(this);
                setGraphic(mCellGraphic);
                mCellGraphic.requestFocus();
            }
        }

        @Override
        public void commitEdit(Object newValue) {
            super.commitEdit(newValue);
            doUpdateItem();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            doUpdateItem();
        }
    }

    public static final String FXML = "PropertiesControl.fxml";

    @FXML
    protected TreeTableView<UiProperty<?>> mTreeTableView;

    @FXML
    protected TreeTableColumn<UiProperty<?>, String> mKeyColumn;

    @FXML
    protected TreeTableColumn<UiProperty<?>, Object> mValueColumn;

    protected PropertiesControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(PropertiesControl.class.getResource(FXML));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initialize();
    }

    public static PropertiesControl create() {
        return new PropertiesControl();
    }

    public TreeTableView<UiProperty<?>> getTreeTableView() {
        return mTreeTableView;
    }

    protected <T> ObservableValue<T> createObservableValue(UiProperty<T> property) {
        return property == null ? new SimpleObjectProperty<>() : property.valueProperty();
    }

    protected void initialize() {
        mKeyColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<UiProperty<?>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<UiProperty<?>, String> param) {
                UiProperty<?> property = param.getValue().getValue();
                return property == null ? new SimpleStringProperty() : new SimpleStringProperty(property.getDisplayName());
            }
        });
        mValueColumn.setCellValueFactory(new Callback<TreeTableColumn.CellDataFeatures<UiProperty<?>, Object>, ObservableValue<Object>>() {
            @SuppressWarnings("unchecked")
            @Override
            public ObservableValue<Object> call(CellDataFeatures<UiProperty<?>, Object> param) {
                return (ObservableValue<Object>) createObservableValue(param.getValue().getValue());
            }
        });
        mValueColumn.setCellFactory(column -> {
            return new PropertyValueTableCell();
        });

        mTreeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void setPlaceholder(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setPadding(new Insets(10));
        mTreeTableView.setPlaceholder(label);
    }

    public void setEmptyInput() {
        setContent(Collections.emptyMap());
    }

    public void setSingleInput(ObjectProperties objectProperties) {
        setContent(objectProperties.getData());
    }

    @SuppressWarnings("rawtypes")
    protected class ConsolidatedUiProperty extends UiProperty<Object> {
        protected final Collection<UiProperty> mChildren = new ArrayList<>();

        public ConsolidatedUiProperty(String key, String displayName, PropertyType type, boolean editable) {
            super(null, key, displayName, type, editable);
        }

        public Collection<UiProperty> getChildren() {
            return mChildren;
        }

        /**
         * Returns the common value if all children have the same value, else {@code null}.
         */
        @Override
        public Object getValue() {
            Object consolidatedValue = null;
            for (UiProperty<?> childProperty : mChildren) {
                Object currentValue = childProperty.getValue();
                if (consolidatedValue == null) {
                    consolidatedValue = currentValue;
                } else if (!consolidatedValue.equals(currentValue)) {
                    return null;
                }
            }
            return consolidatedValue;
        }

        @Override
        public void setValue(Object value) {
            for (UiProperty childProperty : mChildren) {
                childProperty.setValue(value);
            }
        }
    }

    /**
     * Sets the given list of homogenous object properties. The properties control will show a consolidated
     * view on the property collections.
     */
    public void setMultipleInput(Collection<ObjectProperties> multipleObjectsProperties) {
        Map<String, Map<String, ConsolidatedUiProperty>> values = new LinkedMap<>(); // Map: Category name -> (property name -> consolidated property)

        for (ObjectProperties properties : multipleObjectsProperties) {
            Map<String, Collection<UiProperty<?>>> categoriesOfObject = properties.getData();
            for (Entry<String, Collection<UiProperty<?>>> entry : categoriesOfObject.entrySet()) {
                String currentCategory = entry.getKey();
                Collection<UiProperty<?>> currentProperties = entry.getValue();
                Map<String, ConsolidatedUiProperty> contentsOfCategory = values.computeIfAbsent(currentCategory, cat -> new LinkedMap<>());
                for (UiProperty<?> currentProperty : currentProperties) {
                    String currentKey = currentProperty.getKey();
                    ConsolidatedUiProperty consolidatedProperty = contentsOfCategory.computeIfAbsent(currentKey, key -> new ConsolidatedUiProperty(
                        currentKey, currentProperty.getDisplayName(), currentProperty.getType(), currentProperty.isEditable()) {
                        @Override
                        public Object getValue() {
                            if (Strings.BASE_OBJECT_PROPERTIES_GENERAL_SECTION.equals(currentCategory) && BaseObjectUIRepresentation.KEY_PROPERTY_ID.equals(currentKey)) {
                                // Special handling for "ID" property
                                return MessageFormat.format(Strings.BASE_OBJECT_PROPERTIES_ID_MULTIPLE, getChildren().size());
                            }
                            return super.getValue();
                        }
                    });
                    consolidatedProperty.getChildren().add(currentProperty);
                }
            }
        }

        // Attention: We must maintain the order of categories and properties, thus a simplification
        // via Collectors.toMap() doesn't work
        Map<String, Collection<UiProperty<?>>> categories = new LinkedMap<>();
        for (Entry<String, Map<String, ConsolidatedUiProperty>> entry : values.entrySet()) {
            Collection<UiProperty<?>> uiProperties = new ArrayList<>();
            for (UiProperty<?> uiProperty : entry.getValue().values()) {
                uiProperties.add(uiProperty);
            }
            categories.put(entry.getKey(), uiProperties);
        }

        setContent(categories);
    }

    protected void setContent(Map<String, Collection<UiProperty<?>>> categories) {
        TreeItem<UiProperty<?>> root = new TreeItem<>();
        ObservableList<TreeItem<UiProperty<?>>> rootChildren = root.getChildren();
        for (Entry<String, Collection<UiProperty<?>>> categoryEntry : categories.entrySet()) {
            String category = categoryEntry.getKey();
            Collection<UiProperty<?>> propertiesOfCategory = categoryEntry.getValue();
            TreeItem<UiProperty<?>> categoryItem = new TreeItem<>(new ConstantUiProperty<>(null, null, category, null, null)); // Dummy property for category header
            for (UiProperty<?> p : propertiesOfCategory) {
                categoryItem.getChildren().add(new TreeItem<>(p));
            }
            rootChildren.add(categoryItem);
        }
        mTreeTableView.setRoot(root);
        expandAll(root);
    }

    protected void expandAll(TreeItem<UiProperty<?>> item) {
        item.setExpanded(true);
        for (TreeItem<UiProperty<?>> child : item.getChildren()) {
            expandAll(child);
        }
    }

    @FXML
    public void onEditCommit(CellEditEvent<UiProperty<?>, Object> event) {
        UiProperty<?> property = event.getRowValue().getValue();
        property.setValue(event.getNewValue());
    }
}
