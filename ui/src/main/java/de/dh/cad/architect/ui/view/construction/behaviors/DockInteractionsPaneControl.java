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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.utils.fx.ListViewUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Shows the dock interaction wizard pane.
 */
public class DockInteractionsPaneControl extends BorderPane {
    public static class DockTargetObject {
        protected final BaseAnchoredObject mTargetObject;
        protected final Collection<Anchor> mDockableTargetAnchors;

        public DockTargetObject(BaseAnchoredObject targetObject, Collection<Anchor> dockableTargetAnchors) {
            mTargetObject = targetObject;
            mDockableTargetAnchors = dockableTargetAnchors;
        }

        public BaseAnchoredObject getTargetObject() {
            return mTargetObject;
        }

        public Collection<Anchor> getDockableTargetAnchors() {
            return mDockableTargetAnchors;
        }
    }

    public static interface IChangeListener {
        void changed(DockInteractionsPaneControl pane);
    }

    protected final List<IChangeListener> mListeners = new ArrayList<>();
    protected String mDockToAnchorButtonText = Strings.DOCK_OPERATION_PERMANENT_DOCK_TO_TARGET_ANCHOR;

    protected Anchor mSourceAnchor = null;
    protected DockTargetObject mDockTargetObject = null;

    protected Label mSourceObjectText;
    protected Label mSourceAnchorText;
    protected Label mTargetObjectText;
    protected Label mTargetAnchorLabel;
    protected ListView<Anchor> mTargetAnchorListView;
    protected Comparator<Anchor> mTargetAnchorListComparator;
    protected Button mChooseAnchorButton;

    public DockInteractionsPaneControl(Consumer<Anchor> onDockToTargetAnchor) {
        Label title = new Label(Strings.DOCK_OPERATION_DESCRIPTION);
        title.setStyle(Constants.INTERACTIONS_TITLE_STYLE);
        VBox.setMargin(title, new Insets(0, 0, 10, 0));
        Label sourceObjectLabel = new Label(Strings.DOCK_OPERATION_SOURCE_OBJECT);
        mSourceObjectText = new Label();
        mSourceObjectText.setStyle(Constants.INTERACTIONS_TEXT_STYLE);
        VBox.setMargin(mSourceObjectText, new Insets(0, 0, 10, 0));
        Label sourceAnchorLabel = new Label(Strings.DOCK_OPERATION_SOURCE_ANCHOR);
        mSourceAnchorText = new Label();
        mSourceAnchorText.setStyle(Constants.INTERACTIONS_TEXT_STYLE);
        VBox.setMargin(mSourceAnchorText, new Insets(0, 0, 20, 0));

        Label targetObjectLabel = new Label(Strings.DOCK_OPERATION_TARGET_OBJECT);
        mTargetObjectText = new Label();
        mTargetObjectText.setStyle(Constants.INTERACTIONS_TEXT_STYLE);
        VBox.setMargin(mTargetObjectText, new Insets(0, 0, 10, 0));

        mTargetAnchorLabel = new Label(Strings.DOCK_OPERATION_TARGET_ANCHOR);

        VBox elements = new VBox(
            title,
            sourceObjectLabel, mSourceObjectText,
            sourceAnchorLabel, mSourceAnchorText,
            targetObjectLabel, mTargetObjectText,
            mTargetAnchorLabel);
        elements.setFillWidth(true);
        setTop(elements);

        mTargetAnchorListView = new ListView<>();
        mTargetAnchorListView.setCellFactory(param -> new ListCell<>() {
            protected void updateItem(Anchor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(BaseObjectUIRepresentation.getShortName(item));
                }
            }
        });
        mTargetAnchorListComparator = Comparator.comparing(anchor -> BaseObjectUIRepresentation.getShortName(anchor));
        mTargetAnchorListView.setPrefHeight(-1);
        mTargetAnchorListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Anchor> observable, Anchor oldValue, Anchor newValue) {
                updateButton();
                fireChangeListeners();
            }
        });
        setCenter(mTargetAnchorListView);

        mChooseAnchorButton = new Button(mDockToAnchorButtonText);
        mChooseAnchorButton.setOnAction(event -> {
            Anchor anchor = getSelectedTargetAnchor();
            onDockToTargetAnchor.accept(anchor);
        });
        BorderPane.setMargin(mChooseAnchorButton, new Insets(5, 0, 10, 0));
        setBottom(mChooseAnchorButton);

        setPadding(new Insets(10));
    }

    protected void updateButton() {
        boolean anchorChoosen = getSelectedTargetAnchor() != null;
        mChooseAnchorButton.setDisable(!anchorChoosen);
        mChooseAnchorButton.setText(anchorChoosen ? mDockToAnchorButtonText : Strings.DOCK_OPERATION_NO_TARGET_ANCHOR);
    }

    protected void update() {
        mSourceObjectText.setText(mSourceAnchor == null ? "-" : BaseObjectUIRepresentation.getShortName(mSourceAnchor.getAnchorOwner()));
        mSourceAnchorText.setText(mSourceAnchor == null ? "-" : BaseObjectUIRepresentation.getShortName(mSourceAnchor));
        if (mDockTargetObject == null) {
            mTargetObjectText.setText(Strings.DOCK_OPERATION_NO_TARGET_OBJECT);
            mTargetAnchorLabel.setVisible(false);
            mTargetAnchorListView.setVisible(false);
            mTargetAnchorListView.getItems().clear(); // Necessary to fire change handler
        } else {
            mTargetObjectText.setText(BaseObjectUIRepresentation.getShortName(mDockTargetObject.getTargetObject()));
            List<Anchor> anchors = new ArrayList<>(mDockTargetObject.getDockableTargetAnchors());
            Collections.sort(anchors, mTargetAnchorListComparator);
            mTargetAnchorListView.getItems().setAll(anchors);
            mTargetAnchorLabel.setVisible(true);
            mTargetAnchorListView.setVisible(true);
        }
        updateButton();
    }

    public Anchor getSourceAnchor() {
        return mSourceAnchor;
    }

    /**
     * Sets the source anchor of the desired dock operation.
     */
    public void setSourceAnchor(Anchor value) {
        mSourceAnchor = value;
        update();
    }

    public String getDockToAnchorButtonText() {
        return mDockToAnchorButtonText;
    }

    /**
     * Sets the text of the dock commit button in enabled state.
     */
    public void setDockToAnchorButtonText(String value) {
        mDockToAnchorButtonText = value;
    }

    public Anchor getSelectedTargetAnchor() {
        return mTargetAnchorListView.getSelectionModel().getSelectedItem();
    }

    /**
     * Sets the selected target anchor in the current dock target object.
     */
    public void setSelectedTargetAnchor(Anchor anchor) {
        if (anchor != null && !anchor.equals(getSelectedTargetAnchor())) {
            ListViewUtils.showAndSelect(mTargetAnchorListView, anchor);
        }
    }

    public DockTargetObject getDockTargetObject() {
        return mDockTargetObject;
    }

    /**
     * Sets dock target object together with the selected target anchor.
     */
    public void setDockTargetObject(DockTargetObject value, Anchor selectedTargetAnchor) {
        if (!Objects.equals(value,  mDockTargetObject)) {
            mDockTargetObject = value;
            update();
        }
        if (!Objects.equals(selectedTargetAnchor, getSelectedTargetAnchor())) {
            setSelectedTargetAnchor(selectedTargetAnchor);
        }
    }

    /**
     * Registers a listener to become fired when the user selects a different target anchor.
     */
    public void addChangeListener(IChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeChangeListener(IChangeListener listener) {
        mListeners.remove(listener);
    }

    protected void fireChangeListeners() {
        for (IChangeListener listener : mListeners) {
            listener.changed(this);
        }
    }
}
