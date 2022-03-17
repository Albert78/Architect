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
package de.dh.cad.architect.ui.view.libraries;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.PlainFileSystemDirectoryLocator;
import de.dh.utils.fx.FxUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

public class LibraryImporterDialog extends Dialog<Collection<IDirectoryLocator>> implements Initializable {
    protected static final String FXML = "LibraryImporterDialog.fxml";

    protected static final int DIALOG_MIN_WIDTH = 600;
    protected static final int DIALOG_MIN_HEIGHT = 400;

    protected GridPane mRoot;

    @FXML
    protected TextField mPathEditTextField;

    @FXML
    protected Button mRootPathChooserButton;

    @FXML
    protected ListView<CheckableLibraryEntry> mLibrariesListView;

    @FXML
    protected Button mSelectAllButton;

    @FXML
    protected Button mSelectNoneButton;

    @FXML
    protected Button mRefreshButton;

    protected final Node mOpenButton;
    protected final ObservableList<CheckableLibraryEntry> mLibraries = FXCollections.observableArrayList();
    protected final Set<IDirectoryLocator> mDisabledEntries = new HashSet<>();

    public LibraryImporterDialog() {
        setTitle(Strings.LIBRARY_MANAGER_IMPORT_DIALOG_TITLE);
        setHeaderText(Strings.LIBRARY_MANAGER_IMPORT_DIALOG_HEADER);
        // TODO: Nice graphic
        DialogPane dialogPane = getDialogPane();
        loadDialog(dialogPane);

        ObservableList<ButtonType> buttonTypes = dialogPane.getButtonTypes();

        ButtonType openButtonType = new ButtonType(Strings.LIBRARY_MANAGER_IMPORT_OPEN_BUTTON_TITLE, ButtonData.APPLY);
        buttonTypes.addAll(openButtonType, ButtonType.CANCEL);
        mOpenButton = dialogPane.lookupButton(openButtonType);

        setResizable(true);

        Scene scene = dialogPane.getScene();

        scene.getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());

        Stage stage = (Stage) scene.getWindow();
        stage.setMinHeight(DIALOG_MIN_HEIGHT);
        stage.setMinWidth(DIALOG_MIN_WIDTH);

        validate();

        setResultConverter(new Callback<ButtonType, Collection<IDirectoryLocator>>() {
            @Override
            public Collection<IDirectoryLocator> call(ButtonType dialogButton) {
                if (dialogButton == openButtonType) {
                    return mLibraries
                            .stream()
                            .filter(CheckableLibraryEntry::isSelected)
                            .map(le -> le.getRootDirectory())
                            .toList();
                }
                return null;
            }
        });
    }

    protected void loadDialog(DialogPane dialogPane) {
        FXMLLoader fxmlLoader = new FXMLLoader(LibraryImporterDialog.class.getResource(FXML));
        fxmlLoader.setController(this);
        try {
            mRoot = fxmlLoader.load();
            dialogPane.setContent(mRoot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean isDisabledLibrary(IDirectoryLocator libraryRootDirectory) {
        return mDisabledEntries.contains(libraryRootDirectory);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mLibrariesListView.setCellFactory(listView -> {
            CheckBoxListCell<CheckableLibraryEntry> result = new CheckBoxListCell<>(le -> le.selectedProperty()) {
                public void updateItem(CheckableLibraryEntry entry, boolean empty) {
                    super.updateItem(entry, empty);
                    if (entry != null && !empty) {
                        setDisable(isDisabledLibrary(entry.getRootDirectory()));
                    }
                }
            };
            return result;
        });

        mPathEditTextField.textProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                loadLibraries();
            }
        });
        mRootPathChooserButton.setOnAction(event -> showPathChooserDialog());
        mSelectAllButton.setOnAction(event -> setAllLibrariesSelected(true));
        mSelectNoneButton.setOnAction(event -> setAllLibrariesSelected(false));
        mRefreshButton.setOnAction(event -> loadLibraries());
        mLibrariesListView.setItems(mLibraries);
        mLibraries.addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends CheckableLibraryEntry> c) {
                validate();
            }
        });
    }

    protected void setAllLibrariesSelected(boolean value) {
        for (CheckableLibraryEntry libraryEntry : mLibraries) {
            libraryEntry.setSelected(value);
        }
    }

    public Set<IDirectoryLocator> getDisabledLibraries() {
        return mDisabledEntries;
    }

    public void setDisabledLibraries(Collection<IDirectoryLocator> value) {
        mDisabledEntries.clear();
        mDisabledEntries.addAll(value);
    }

    public Optional<Path> getORootPath() {
        try {
            Path path = Path.of(mPathEditTextField.getText());
            return Files.isDirectory(path) ? Optional.of(path.toAbsolutePath()) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    public void setRootPath(Path value) {
        mPathEditTextField.setText(value.toAbsolutePath().toString());
    }

    protected void validate() {
        boolean valid = false;
        for (CheckableLibraryEntry libraryEntry : mLibraries) {
            if (libraryEntry.isSelected() && !mDisabledEntries.contains(libraryEntry.getRootDirectory())) {
                valid = true;
                break;
            }
        }
        mOpenButton.setDisable(!valid);
    }

    protected static Label createPlaceholderText(String text) {
        Label result = new Label(text);
        result.setWrapText(true);
        return result;
    }

    protected void loadLibraries() {
        Optional<Path> oRootPath = getORootPath();
        if (oRootPath.isEmpty()) {
            mLibrariesListView.setPlaceholder(createPlaceholderText(Strings.LIBRARY_MANAGER_IMPORT_NO_DIRECTORY));
            mLibraries.clear();
            return;
        }
        Path rootPath = oRootPath.get();
        mLibrariesListView.setPlaceholder(createPlaceholderText(MessageFormat.format(Strings.LIBRARY_MANAGER_IMPORT_NO_LIBRARIES_FOUND, rootPath)));
        try {
            Collection<CheckableLibraryEntry> libraries = scanLibraries(rootPath);
            for (CheckableLibraryEntry libraryEntry : libraries) {
                libraryEntry.selectedProperty().addListener(new ChangeListener<>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        validate();
                    }
                });
            }
            mLibraries.setAll(libraries);
        } catch (IOException e) {
            mLibrariesListView.setPlaceholder(createPlaceholderText(MessageFormat.format(Strings.LIBRARY_MANAGER_IMPORT_ERROR_READING_LIBRARIES, rootPath)));
        }
    }

    protected Collection<CheckableLibraryEntry> scanLibraries(Path rootPath) throws IOException {
        Collection<CheckableLibraryEntry> result = new ArrayList<>();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                PlainFileSystemDirectoryLocator directoryLocator = new PlainFileSystemDirectoryLocator(dir);
                if (AssetManager.isAssetLibraryDirectory(directoryLocator)) {
                    result.add(new CheckableLibraryEntry(AssetManager.loadAssetLibrary(directoryLocator), directoryLocator));
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    protected void showPathChooserDialog() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(Strings.LIBRARY_MANAGER_LOAD_ASSET_LIBRARIES_DIALOG_TITLE);
        getORootPath().ifPresent(rootPath -> {
            FxUtils.trySetInitialDirectory(directoryChooser, rootPath);
        });
        File selectedDirectory = directoryChooser.showDialog(mRoot.getScene().getWindow());
        if (selectedDirectory == null) {
            return;
        }
        setRootPath(selectedDirectory.toPath());
    }
}
