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
package de.dh.cad.architect.ui.view.libraries;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.model.Paragraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.codeeditors.MtlEditor;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.assets.MaterialSetDescriptor;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.utils.SystemUtils;
import de.dh.cad.architect.utils.vfs.IDirectoryLocator;
import de.dh.cad.architect.utils.vfs.IResourceLocator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

public class RawMaterialsFileEditControl extends BorderPane implements Initializable {
    private Logger log = LoggerFactory.getLogger(RawMaterialsFileEditControl.class);

    public static final String FXML = "RawMaterialsFileEditControl.fxml";

    @FXML
    protected BorderPane mSourceCodeParent;

    @FXML
    protected Button mOpenDirectoryButton;

    @FXML
    protected BorderPane mThreeDViewParentPane;

    @FXML
    protected TextArea mPreviewLogTextArea;

    protected final AssetLoader mAssetLoader;
    protected MaterialSetDescriptor mDescriptor;
    protected MaterialPreviewChoiceControl mMaterialPreviewControl;
    protected MtlEditor mMtlEditor;
    protected NavigableMap<Integer, String> mMaterialDefinitions;

    public RawMaterialsFileEditControl(AssetLoader assetLoader) {
        mAssetLoader = assetLoader;

        FXMLLoader fxmlLoader = new FXMLLoader(RawMaterialsFileEditControl.class.getResource(FXML));
        fxmlLoader.setController(this);
        fxmlLoader.setRoot(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mMtlEditor = new MtlEditor();
        mSourceCodeParent.setCenter(mMtlEditor);
        mMtlEditor.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                updateMaterials();
                updatePreview();
            }
        });
        mMtlEditor.selectionProperty().addListener(new ChangeListener<IndexRange>() {
            @Override
            public void changed(ObservableValue<? extends IndexRange> observable, IndexRange oldValue, IndexRange newValue) {
                updatePreview();
            }
        });
        mMaterialPreviewControl = new MaterialPreviewChoiceControl(mAssetLoader);
        mMaterialPreviewControl.setFallbackToPlaceholderOnError(false);
        mMaterialPreviewControl.setOnMaterialLoadError(this::logMaterialLoadError);
        mMaterialPreviewControl.setOnMaterialLoaded(this::logMaterialLoaded);
        mThreeDViewParentPane.setCenter(mMaterialPreviewControl);

        updatePreview(null);
    }

    public void initialize(MaterialSetDescriptor descriptor) {
        mDescriptor = descriptor;
        AssetRefPath materialSetRef = mDescriptor.getSelfRef();
        try {
            loadSourceCode();
            IDirectoryLocator materialSetResourcesDirectory = mAssetLoader.getModelDirectory(materialSetRef);
            if (materialSetResourcesDirectory.exists()) {
                mOpenDirectoryButton.setOnAction(event -> {
                    SystemUtils.openDirectoryInExplorer(Path.of(materialSetResourcesDirectory.getAbsolutePath()));
                });
            }
        } catch (IOException e) {
            log.error("Error accessing mtl resources for material set descriptor <" + materialSetRef + ">", e);
        }
    }

    protected void loadSourceCode() throws IOException {
        AssetRefPath materialSetRef = mDescriptor.getSelfRef();
        IResourceLocator mtlResource = mAssetLoader.getMtlResource(materialSetRef);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(mtlResource.inputStream()))) {
            List<String> lines = br.lines().toList();
            mMtlEditor.replaceText(0, 0, StringUtils.join(lines, "\n"));
        }
    }

    protected void saveSourceCode() throws IOException {
        AssetRefPath materialSetRef = mDescriptor.getSelfRef();
        IResourceLocator mtlResource = mAssetLoader.getMtlResource(materialSetRef);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(mtlResource.outputStream()))) {
            bw.append(mMtlEditor.getText());
        }
        mMaterialPreviewControl.initialize(mDescriptor);
    }

    protected void updateMaterials() {
        AssetRefPath materialSetRef = mDescriptor.getSelfRef();
        try {
            saveSourceCode();
        } catch (IOException e) {
            log.error("Error accessing mtl resources for material set descriptor <" + materialSetRef + ">", e);
        }

        NavigableMap<Integer, String> materialDefinitions = new TreeMap<>();
        List<Paragraph<Collection<String>, String, Collection<String>>> paragraphs = mMtlEditor.getParagraphs();
        int numParagraphs = paragraphs.size();
        for (int paragraphNo = 0; paragraphNo < numParagraphs; paragraphNo++) {
            // Here, we use a bit internal knowledge about the MTL editor, so we have to import classes of the code editor implementation.
            // This is actually not what we want but the alternative would be to formalize all the calls to the paragraphs in the code editor.
            // But since this raw materials editor is a bit a workaround, we do it this simple way.
            Paragraph<Collection<String>, String, Collection<String>> p = paragraphs.get(paragraphNo);
            String line = p.getText();
            if (StringUtils.startsWithIgnoreCase(line, "newmtl ")) {
                String mtlName = line.substring("newmtl ".length()).trim();
                materialDefinitions.put(paragraphNo, mtlName);
            }
        }
        mMaterialDefinitions = materialDefinitions;
    }

    protected String getSelectedMaterial() {
        int pos = mMtlEditor.getAnchor();
        int lineNo = mMtlEditor.offsetToPosition(pos, null).getMajor();
        Integer selectedMaterialStartLine = mMaterialDefinitions.floorKey(lineNo);
        String selectedMaterialName = selectedMaterialStartLine == null ? null : mMaterialDefinitions.get(selectedMaterialStartLine);
        return selectedMaterialName;
    }

    protected void updatePreview() {
        String selectedMaterialName = getSelectedMaterial();
        updatePreview(selectedMaterialName);
    }

    protected void clearLogView() {
        mPreviewLogTextArea.deleteText(0, mPreviewLogTextArea.getLength());
    }

    protected void appendLogMsg(String msg) {
        mPreviewLogTextArea.appendText(msg + "\n");
    }

    protected void updatePreview(String materialName) {
        try {
            mMaterialPreviewControl.selectMaterial(materialName);
        } catch (Exception e) {
            logMaterialLoadError(new MaterialDescriptor(mMaterialPreviewControl.getSelectedMaterialDescriptor().getMaterialSetDescriptor(), materialName), e);
        }
    }

    protected void logMaterialLoaded(MaterialDescriptor descriptor) {
        clearLogView();
        appendLogMsg("Successfully loaded material " + descriptor.getMaterialRef());
    }

    protected void logMaterialLoadError(MaterialDescriptor descriptor, Exception e) {
        clearLogView();
        try (StringWriter sw = new StringWriter()) {
            e.printStackTrace(new PrintWriter(sw));
            appendLogMsg(sw.toString() + "\n");
        } catch (IOException ex) {
            appendLogMsg("- Error writing error message -" + ex.getMessage());
        }
    }

    public BooleanProperty validProperty() {
        // TODO
        return new SimpleBooleanProperty(true);
    }
}
