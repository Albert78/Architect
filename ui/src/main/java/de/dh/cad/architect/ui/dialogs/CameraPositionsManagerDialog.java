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
package de.dh.cad.architect.ui.dialogs;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.persistence.CameraPosition;
import de.dh.cad.architect.utils.Namespace;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;

public class CameraPositionsManagerDialog extends Dialog<Map<String, CameraPosition>> {
    protected static class NamedCameraPositionWrapper {
        protected CameraPosition mCameraPosition;
        protected String mName;

        public NamedCameraPositionWrapper(String name, CameraPosition position) {
            mCameraPosition = position;
            mName = name;
        }

        public String getName() {
            return mName;
        }

        public void setName(String value) {
            mName = value;
        }

        public CameraPosition getCameraPosition() {
            return mCameraPosition;
        }

        public void setCameraPosition(CameraPosition value) {
            mCameraPosition = value;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    protected final ObservableList<NamedCameraPositionWrapper> mCameraPositions = FXCollections.observableArrayList();
    protected boolean mChanged = false;

    public CameraPositionsManagerDialog(Map<String, CameraPosition> cameraPositions) {
        setTitle(Strings.THREE_D_CAMERA_POSITIONS_MANAGER_DIALOG_TITLE);
        // TODO: Nice graphic
        DialogPane dialogPane = getDialogPane();
        ObservableList<ButtonType> buttonTypes = dialogPane.getButtonTypes();
        buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL);

        Node content = buildContent(cameraPositions);

        dialogPane.setContent(content);
        setResizable(true);

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.setMinHeight(600);
        stage.setMinWidth(400);

        setResultConverter(new Callback<ButtonType, Map<String, CameraPosition>>() {
            @Override
            public Map<String, CameraPosition> call(ButtonType dialogButton) {
                if (mChanged && dialogButton == ButtonType.OK) {
                    return getCameraPositions();
                }
                return null;
            }
        });
    }

    public static CameraPositionsManagerDialog create(Map<String, CameraPosition> cameraPositions) {
        return new CameraPositionsManagerDialog(cameraPositions);
    }

    protected void fillCameraPositions(Map<String, CameraPosition> cameraPositions) {
        mCameraPositions.clear();
        Map<String, CameraPosition> sortedCameraPositions = new TreeMap<>(cameraPositions);
        for (Entry<String, CameraPosition> cameraPositionEntry : sortedCameraPositions.entrySet()) {
            mCameraPositions.add(new NamedCameraPositionWrapper(cameraPositionEntry.getKey(), cameraPositionEntry.getValue()));
        }
    }

    protected Pane buildContent(Map<String, CameraPosition> initialCameraPositions) {
        ListView<NamedCameraPositionWrapper> listView = new ListView<>();
        fillCameraPositions(initialCameraPositions);
        listView.setItems(mCameraPositions);
        MultipleSelectionModel<NamedCameraPositionWrapper> listViewSelectionModel = listView.getSelectionModel();
        listViewSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        Button removeButton = new Button(Strings.BUTTON_REMOVE);
        removeButton.setOnAction(event -> {
            ObservableList<NamedCameraPositionWrapper> selectedItems = listViewSelectionModel.getSelectedItems();
            mCameraPositions.removeAll(selectedItems);
            mChanged = true;
        });

        Button renameButton = new Button(Strings.BUTTON_RENAME);
        renameButton.setOnAction(event -> {
            NamedCameraPositionWrapper selectedCameraPosition = listViewSelectionModel.getSelectedItem();
            String selectedName = selectedCameraPosition.getName();

            Namespace<NamedCameraPositionWrapper> ns = new Namespace<>();
            Map<String, NamedCameraPositionWrapper> nsMappings = ns.getMappings();
            for (NamedCameraPositionWrapper cameraPosition : mCameraPositions) {
                String name = cameraPosition.getName();
                if (name.equals(selectedName)) {
                    continue;
                }
                nsMappings.put(name, cameraPosition);
            }

            Optional<String> oName = showQueryCameraNameDialog(
                Strings.THREE_D_CAMERA_POSITIONS_MANAGER_NAME_CAMERA_POSITION_DIALOG_TITLE,
                Strings.THREE_D_CAMERA_POSITIONS_MANAGER_NAME_CAMERA_POSITION_DIALOG_HEADER,
                Strings.THREE_D_CAMERA_POSITIONS_MANAGER_NAME_CAMERA_POSITION_DIALOG_CAMERA_NAME_LABEL,
                selectedName, ns);
            oName.ifPresent(name -> {
                Map<String, CameraPosition> modifiedCameraPositions = new TreeMap<>(getCameraPositions());
                modifiedCameraPositions.remove(selectedName);
                modifiedCameraPositions.put(name, selectedCameraPosition.getCameraPosition());
                fillCameraPositions(modifiedCameraPositions);
                mChanged = true;
            });
        });

        HBox buttons = new HBox(removeButton, renameButton);
        buttons.setSpacing(5);

        ObservableList<NamedCameraPositionWrapper> selectedItems = listViewSelectionModel.getSelectedItems();
        selectedItems.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends NamedCameraPositionWrapper> c) {
                removeButton.setDisable(selectedItems.isEmpty());
                renameButton.setDisable(selectedItems.size() != 1);
            }
        });

        BorderPane.setMargin(buttons, new Insets(5, 0, 0, 0));
        BorderPane root = new BorderPane(listView, null, null, buttons, null);

        return root;
    }

    public Map<String, CameraPosition> getCameraPositions() {
        return mCameraPositions
                        .stream()
                        .collect(Collectors.toMap(cp -> cp.getName(), cp -> cp.getCameraPosition()));
    }

    public static void showCameraNameAlreadyExistsDialog(String name) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(Strings.THREE_D_INVALID_CAMERA_NAME_DIALOG_TITLE);
        alert.setContentText(MessageFormat.format(Strings.THREE_D_INVALID_CAMERA_NAME_DIALOG_NAME_ALREADY_EXISTS_TEXT, name));

        alert.showAndWait();
    }

    public static Optional<String> showQueryCameraNameDialog(String title, String headerText, String cameraTextLabel, String nameProposal, Namespace<?> existingNames) {
        TextInputDialog dialog = new TextInputDialog(nameProposal);
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(cameraTextLabel);

        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            return Optional.empty();
        }
        String name = result.get();
        if (existingNames.contains(name)) {
            showCameraNameAlreadyExistsDialog(name);
            return Optional.empty();
        } else {
            return Optional.of(name);
        }
    }
}
