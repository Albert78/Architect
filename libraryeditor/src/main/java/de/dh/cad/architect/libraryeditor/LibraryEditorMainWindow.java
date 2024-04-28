/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.dh.cad.architect.libraryeditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.libraryeditor.codeeditor.CodeEditorControl;
import de.dh.cad.architect.libraryeditor.surfacelist.SurfaceConfigurationsListControl;
import de.dh.cad.architect.libraryeditor.threed.ThreeDViewControl;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.assets.AssetManager;
import de.dh.cad.architect.ui.logoutput.LogOutputControl;
import de.dh.cad.architect.ui.view.libraries.MaterialChooserDialog;
import de.dh.utils.MaterialMapping;
import de.dh.utils.csg.CSGSurfaceAwareAddon;
import de.dh.utils.fx.ImageUtils;
import de.dh.utils.fx.StageState;
import de.dh.utils.fx.viewsfx.AbstractDockableViewLocationDescriptor;
import de.dh.utils.fx.viewsfx.DefaultDockHostCreator;
import de.dh.utils.fx.viewsfx.DockAreaControl;
import de.dh.utils.fx.viewsfx.DockSystem;
import de.dh.utils.fx.viewsfx.Dockable;
import de.dh.utils.fx.viewsfx.DockableFloatingStage;
import de.dh.utils.fx.viewsfx.IDockZoneParent;
import de.dh.utils.fx.viewsfx.PersistentTabDockHost;
import de.dh.utils.fx.viewsfx.TabDockHost;
import de.dh.utils.fx.viewsfx.ViewLifecycleManager;
import de.dh.utils.fx.viewsfx.ViewsRegistry;
import de.dh.utils.fx.viewsfx.io.ViewsLayoutStateIO;
import de.dh.utils.fx.viewsfx.layout.AbstractDockLayoutDescriptor;
import de.dh.utils.fx.viewsfx.layout.PerspectiveDescriptor;
import de.dh.utils.fx.viewsfx.layout.SashLayoutDescriptor;
import de.dh.utils.fx.viewsfx.layout.SashLayoutDescriptor.Orientation;
import de.dh.utils.fx.viewsfx.layout.TabHostLayoutDescriptor;
import de.dh.utils.fx.viewsfx.state.ViewsLayoutState;
import de.dh.utils.io.MeshData;
import de.dh.utils.io.fx.FxMeshBuilder;
import eu.mihosoft.jcsg.CSG;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class LibraryEditorMainWindow implements Initializable {
    protected static final String FXML = "Main.fxml";

    public static final String MAIN_DOCK_HOST_ID = "Main";

    public static final String DOCK_ZONE_ID_MAIN = "Main"; // Complete area of window

    public static final String DOCK_ZONE_ID_MAIN_LEFT = "MainLeft"; // Left area of Main - contains code editor
    public static final String DOCK_ZONE_ID_MAIN_CENTER_RIGHT = "MainCenterRight"; // Center and right area of Main - Contains 3D view and surfaces list
    public static final String DOCK_ZONE_ID_MAIN_RIGHT = "MainRight"; // Right area of MAIN_CENTER_RIGHT - Contains surfaces list
    public static final String DOCK_ZONE_ID_MAIN_CENTER = "MainCenter"; // Center area of MAIN_CENTER_RIGHT - Contains 3D view

    public static final String DOCK_ZONE_ID_MAIN_CENTER_BOTTOM = "MainCenterBottom"; // Lower area of MainCenter
    public static final String DOCK_ZONE_ID_MAIN_CENTER_MAIN = "MainCenterMain"; // Upper area of MainCenter

    public static final String TAB_DOCK_HOST_ID_LEFT = "Views1"; // Code editor
    public static final String TAB_DOCK_HOST_ID_RIGHT = "Views2"; // Surfaces list

    public static final String TAB_DOCK_HOST_ID_ADDITIONAL_VIEWS = "AdditionalViews"; // Log output and additional views
    public static final String TAB_DOCK_HOST_ID_CENTER = "Center"; // Editor views

    public static final String VIEW_ID_CODEEDITOR = "CodeEditor";
    public static final String VIEW_ID_SURFACESLIST = "SurfacesList";
    public static final String VIEW_ID_3DVIEW = "3D-View";
    public static final String VIEW_ID_LOG_OUTPUT = "LogOutput";

    public static final int LEFT_AREA_WIDTH = 400;
    public static final int RIGHT_AREA_WIDTH = 400;

    protected static final int WINDOW_SIZE_X = 1024;
    protected static final int WINDOW_SIZE_Y = 768;

    private static Logger log = LoggerFactory.getLogger(LibraryEditorMainWindow.class);

    protected final LibraryEditorConfiguration mConfig;

    protected final AssetManager mAssetManager;
    protected final AssetLoader mAssetLoader;

    protected Stage mPrimaryStage;

    protected DockAreaControl mMainDockHost;

    protected CodeEditorControl mCodeEditorControl;
    protected ThreeDViewControl mThreeDViewControl;
    protected SurfaceConfigurationsListControl mSurfacesListViewControl;
    protected LogOutputControl mLogOutputControl;

    protected ViewLifecycleManager<CodeEditorControl> mCodeEditorViewManager = null;
    protected ViewLifecycleManager<ThreeDViewControl> mThreeDViewManager = null;
    protected ViewLifecycleManager<SurfaceConfigurationsListControl> mSurfacesListViewManager = null;
    protected ViewLifecycleManager<LogOutputControl> mLogOutputViewManager = null;

    protected BooleanProperty mAutoCompileProperty = new SimpleBooleanProperty(true);

    protected CSG mCSGObject;

    protected Map<String, MeshView> mSurfacesToMeshViews = new HashMap<>(); // Cleared for each recompilation of our object

    // Contains a configuration object for each configured surface - surfaces which have not been
    // configured don't have a corresponding entry. An entry remains in this map even if the current object doesn't contain
    // a corresponding surface until the entry is explicitly removed by the user.
    protected Map<String, SurfaceConfigurationData> mSurfaceConfigurations = new HashMap<>();

    @FXML
    protected BorderPane mRoot;

    @FXML
    protected StackPane mDockHostParent;

    public LibraryEditorMainWindow(AssetManager assetManager, LibraryEditorConfiguration config) {
        mConfig = config;
        mAssetManager = assetManager;
        mAssetLoader = mAssetManager.buildAssetLoader();
        FXMLLoader fxmlLoader = new FXMLLoader(LibraryEditorMainWindow.class.getResource(FXML));
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LibraryEditorMainWindow create(AssetManager assetManager, LibraryEditorConfiguration config) {
        return new LibraryEditorMainWindow(assetManager, config);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mSurfacesListViewControl = new SurfaceConfigurationsListControl(mAssetLoader);

        mCodeEditorControl = new CodeEditorControl();
        EventStream<Change<String>> textEvents = EventStreams.changesOf(mCodeEditorControl.getCodeProperty());

        textEvents.reduceSuccessions((a, b) -> b, Duration.ofMillis(500)).subscribe(
            code -> {
                if (isAutoCompile()) {
                    compile(code.getNewValue());
                }
            });
        mAutoCompileProperty.addListener((prop, oldVal, newVal) -> {
            if (newVal && !oldVal) {
                compile(mCodeEditorControl.getCode());
            }
        });

        mCodeEditorControl.setCode(
            "CSG res = CSGs.box(300, 200, 100);\n"
            + "CSG x = CSGs.box(100, 100, 100, 'x');\n"
            + "x = x.transformed(Transform.unity().scale(0.5));\n"
            + "res = res.difference(x);\n"
            + "res;");

        mLogOutputControl = LogOutputControl.create();

        mThreeDViewControl = ThreeDViewControl.create();
    }
    // Creation/Restoration of views needs access to the scene, so this
    // method must be called when the stage has already been shown.
    protected void initializeViewsAndDockZones(Optional<ViewsLayoutState> oViewsLayoutState) {
        DockSystem.getFloatingStages().addListener(new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends DockableFloatingStage> c) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        c.getAddedSubList().forEach(st -> {
                            st.getScene().getStylesheets().add(Constants.APPLICATION_CSS.toExternalForm());
                        });
                    }
                }
            }
        });

        DockSystem.setDockHostCreator(new DefaultDockHostCreator() {
            @Override
            public TabDockHost createTabDockHost(IDockZoneParent parentDockHost, String dockZoneId, String tabDockHostId) {
                if (tabDockHostId.equals(TAB_DOCK_HOST_ID_CENTER)) {
                    PersistentTabDockHost result = new PersistentTabDockHost(tabDockHostId, dockZoneId, parentDockHost) {
                        @Override
                        protected Node createPlaceholder() {
                            return new Text(Strings.EDITORS_DOCKZONE_EMPTY_PLACEHOLDER);
                        }
                    };
                    result.initialize();
                    return result;
                }
                return TabDockHost.create(parentDockHost, dockZoneId, tabDockHostId);
            }
        });

        // Step 1: Create view registry and view lifecycle managers.
        ViewsRegistry viewsRegistry = new ViewsRegistry();
        DockSystem.setViewsManager(viewsRegistry);
        viewsRegistry.addView(mCodeEditorViewManager = new ViewLifecycleManager<>(VIEW_ID_CODEEDITOR, true) {
            @Override
            protected Dockable<CodeEditorControl> createDockable(String viewId) {
                return Dockable.of(mCodeEditorControl, viewId, Strings.CODE_EDITOR_VIEW_TITLE, false);
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_LEFT);
            }
        });
        viewsRegistry.addView(mThreeDViewManager = new ViewLifecycleManager<>(VIEW_ID_3DVIEW, true) {
            @Override
            protected Dockable<ThreeDViewControl> createDockable(String viewId) {
                return Dockable.of(mThreeDViewControl, viewId, Strings.THREE_D_VIEW_TITLE, false);
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_CENTER);
            }
        });
        viewsRegistry.addView(mSurfacesListViewManager = new ViewLifecycleManager<>(VIEW_ID_SURFACESLIST, true) {
            @Override
            protected Dockable<SurfaceConfigurationsListControl> createDockable(String viewId) {
                return Dockable.of(mSurfacesListViewControl, viewId, Strings.SURFACES_LIST_VIEW_TITLE, false);
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_RIGHT);
            }
        });
        viewsRegistry.addView(mLogOutputViewManager = new ViewLifecycleManager<>(VIEW_ID_LOG_OUTPUT, true) {
            @Override
            protected Dockable<LogOutputControl> createDockable(String viewId) {
                Dockable<LogOutputControl> result = Dockable.of(mLogOutputControl, viewId, Strings.LOG_OUTPUT_VIEW_TITLE, true);
                return result;
            }

            @Override
            protected Optional<? extends AbstractDockableViewLocationDescriptor> getPreferredViewLocation() {
                return DockSystem.getViewLocationDescriptorForTabDockHost(TAB_DOCK_HOST_ID_ADDITIONAL_VIEWS);
            }
        });

        Map<String, AbstractDockLayoutDescriptor> dockAreaLayouts = new HashMap<>();
        dockAreaLayouts.put(MAIN_DOCK_HOST_ID,
            new SashLayoutDescriptor(DOCK_ZONE_ID_MAIN,
                Orientation.Horizontal, 0.3,
                new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_LEFT, TAB_DOCK_HOST_ID_LEFT),
                new SashLayoutDescriptor(DOCK_ZONE_ID_MAIN_CENTER_RIGHT,
                    Orientation.Horizontal, 0.85,
                    new SashLayoutDescriptor(DOCK_ZONE_ID_MAIN_CENTER,
                        Orientation.Vertical, 0.7,
                        new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_CENTER_MAIN, TAB_DOCK_HOST_ID_CENTER),
                        new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_CENTER_BOTTOM, TAB_DOCK_HOST_ID_ADDITIONAL_VIEWS)),
                    new TabHostLayoutDescriptor(DOCK_ZONE_ID_MAIN_RIGHT, TAB_DOCK_HOST_ID_RIGHT)
                    )));
        PerspectiveDescriptor perspectiveLayout = new PerspectiveDescriptor(
            dockAreaLayouts,
            Collections.emptyList(),
            Arrays.asList(
                VIEW_ID_CODEEDITOR,
                VIEW_ID_3DVIEW,
                VIEW_ID_SURFACESLIST,
                VIEW_ID_LOG_OUTPUT));

        DockSystem.setPerspectiveLayout(perspectiveLayout);

        // Step 2: Create all needed dock host controls and install the controls in the node hierarchy.
        // When restoring the dock host control, all views and dock zones must have been declared.
        mMainDockHost = DockAreaControl.create(MAIN_DOCK_HOST_ID);
        mDockHostParent.getChildren().add(mMainDockHost);

        // Step 3: Restore or initialize perspective layout
        if (oViewsLayoutState.isPresent()) {
            ViewsLayoutState viewsLayoutState = oViewsLayoutState.get();
            DockSystem.restoreLayoutState(viewsLayoutState);
        } else {
            DockSystem.resetPerspective();
        }

        Platform.runLater(() -> {
            if (mCodeEditorViewManager.getDockable().map(d -> d.isVisible()).orElse(false)) {
                mCodeEditorControl.requestFocus();
            }
        });
    }

    public void show(Stage primaryStage) {
        Scene scene = new Scene(mRoot, WINDOW_SIZE_X, WINDOW_SIZE_Y);
        mPrimaryStage = primaryStage;

        ObservableList<String> stylesheets = scene.getStylesheets();
        stylesheets.add(LibraryEditorMainWindow.class.getResource("java-keywords.css").toExternalForm()); // Syntax highlighting in code editor
        stylesheets.add(LibraryEditorMainWindow.class.getResource("surfaces-list-view.css").toExternalForm());

        restoreStageState();

        primaryStage.setScene(scene);
        primaryStage.setTitle(Strings.LIBRARY_EDITOR_WINDOW_TITLE);
        primaryStage.show();

        mPrimaryStage.setOnCloseRequest(event -> {
            shutdown();
        });

        Platform.runLater(() -> {
            initializeAfterShow();
        });
    }

    public void shutdown() {
        saveStageState();
        saveSettings();
        saveViewsLayoutState();

        Platform.exit();
    }

    public void initializeAfterShow() {
        Optional<ViewsLayoutState> oViewsLayoutState = Optional.empty();
        try {
            Path settingsPath = Paths.get(Constants.LIBRARY_EDITOR_LAYOUT_FILE_NAME);
            if (Files.exists(settingsPath)) {
                try (BufferedReader br = Files.newBufferedReader(settingsPath)) {
                    oViewsLayoutState = Optional.of(ViewsLayoutStateIO.deserialize(br));
                }
            }
        } catch (Exception e) {
            log.warn("Unable to load settings", e);
        }
        initializeViewsAndDockZones(oViewsLayoutState);

        loadSettings();
    }

    public void saveViewsLayoutState() {
        try {
            ViewsLayoutState viewsLayoutState = DockSystem.saveLayoutState();

            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(Constants.LIBRARY_EDITOR_LAYOUT_FILE_NAME))) {
                ViewsLayoutStateIO.serialize(viewsLayoutState, bw);
            }
        } catch (Exception e) {
            log.warn("Unable to store settings", e);
        }
    }

    protected void saveStageState() {
        Optional<StageState> oFormerStageState = StageState.buildStageState(mConfig.getLastWindowState());
        String stageState = StageState.fromStage(mPrimaryStage, oFormerStageState).serializeState();
        mConfig.setLastWindowState(stageState);
    }

    protected void restoreStageState() {
        Optional<StageState> oFormerStageState = StageState.buildStageState(mConfig.getLastWindowState());
        oFormerStageState.ifPresent(formerStageState -> {
            formerStageState.applyToStage(mPrimaryStage);
        });
    }

    public void loadSettings() {
        mThreeDViewControl.loadSettings(mConfig);
        setAutoCompile(mConfig.isAutoCompile());
    }

    public void saveSettings() {
        mConfig.setAutoCompile(isAutoCompile());
        mThreeDViewControl.saveSettings(mConfig);
    }

    protected void clearLog() {
        mLogOutputControl.clearLog();
    }

    protected void logCodeResult(String msg) {
        mLogOutputControl.log(msg);
    }

    protected void compile(String code) {
        mCSGObject = null;
        clearLog();
        try {
            CompilerConfiguration cc = new CompilerConfiguration().addCompilationCustomizers(
                    new ImportCustomizer().
                    addStarImports(
                            "de.dh.utils.csg",
                            "eu.mihosoft.vvecmath",
                            "eu.mihosoft.jcsg").
                    addStaticStars("eu.mihosoft.vvecmath.Transform")
            );

            GroovyShell shell = new GroovyShell(getClass().getClassLoader(),
                    new Binding(), cc);
            Script script = shell.parse(code);
            Object obj = script.run();

            if (obj instanceof CSG csg) {
                mCSGObject = csg;

                Collection<MeshView> meshViews = new ArrayList<>();
                Collection<String> currentSurfaceTypeIds = new ArrayList<>();

                Map<String, MeshData> meshes = CSGSurfaceAwareAddon.createMeshes(mCSGObject, Optional.empty());
                for (Entry<String, MeshData> entry : meshes.entrySet()) {
                    String surfaceTypeId = entry.getKey();

                    MeshData meshData = entry.getValue();

                    Mesh mesh = FxMeshBuilder.buildMesh(meshData);
                    MeshView meshView = new MeshView(mesh);

                    meshView.setId(surfaceTypeId);
                    mSurfacesToMeshViews.put(surfaceTypeId, meshView);

                    meshView.setCullFace(CullFace.BACK);
                    configureSurfaceNode(meshView, surfaceTypeId);

                    currentSurfaceTypeIds.add(surfaceTypeId);

                    meshViews.add(meshView);
                }

                updateSurfaceList(currentSurfaceTypeIds);

                mThreeDViewControl.setMeshes(meshViews);
            } else {
                logCodeResult(">> No CSG object returned :(");
            }
            logCodeResult("Script was successfully executed.");
        } catch (Throwable ex) {
            logCodeResult(ex.getMessage());
        }
    }

    protected void setSurfaceMaterial(SurfaceConfigurationData surfaceConfiguration, AssetRefPath materialRef) {
        surfaceConfiguration.setMaterialRef(materialRef);
    }

    protected void configureSurfaceNode(MeshView meshView, String surfaceTypeId) {
        SurfaceConfigurationData surfaceConfiguration = mSurfaceConfigurations.computeIfAbsent(surfaceTypeId, sti -> {
            SurfaceConfigurationData scd = new SurfaceConfigurationData(sti, true);
            return scd;
        });

        meshView.setMaterial(buildSurfaceMaterial(surfaceConfiguration));

        meshView.setOnMouseEntered(event -> {
            surfaceConfiguration.setFocused(true);
        });
        meshView.setOnMouseExited(event -> {
            surfaceConfiguration.setFocused(false);
        });

        surfaceConfiguration.getFocusedProperty().addListener((prop, oldVal, newVal) -> {
            ((PhongMaterial) meshView.getMaterial()).setDiffuseColor(newVal ? Constants.FOCUSED_COLOR : Constants.UNFOCUSED_COLOR);
        });

        surfaceConfiguration.getMaterialRefProperty().addListener((prop, oldVal, newVal) -> {
            PhongMaterial material = buildSurfaceMaterial(surfaceConfiguration);
            meshView.setMaterial(material);
        });

        meshView.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            MaterialChooserDialog dialog = MaterialChooserDialog.createWithProgressIndicator(mAssetLoader, "Choose material for surface '" + surfaceTypeId + "'",
                mcd -> mcd.selectMaterial(surfaceConfiguration.getMaterialRef()));
            Optional<AssetRefPath> oRes = dialog.showAndWait();
            oRes.ifPresent(materialRef -> {
                setSurfaceMaterial(surfaceConfiguration, materialRef);
            });
        });
    }

    protected PhongMaterial buildSurfaceMaterial(SurfaceConfigurationData surfaceConfiguration) {
        // TODO: If we want to support "tile" mode for the applied material, we need to extend the SupportObjectDescriptor's
        // capabilities, see comment in class MeshConfiguration
        return mAssetLoader.buildMaterial(surfaceConfiguration.getMaterialRef(), MaterialMapping.stretch());
    }

    public void updateSurfaceList(Collection<String> currentSurfaceTypeIds) {
        mSurfaceConfigurations.values().forEach(sc -> {
            sc.setInUse(currentSurfaceTypeIds.contains(sc.getSurfaceTypeId()));
        });
        mSurfaceConfigurations.values().removeIf(scd -> !scd.containsConfiguredValues() && !scd.isInUse());

        mSurfacesListViewControl.updateList(mSurfaceConfigurations);
    }

    public boolean isAutoCompile() {
        return mAutoCompileProperty.get();
    }

    public void setAutoCompile(boolean value) {
        mAutoCompileProperty.set(value);
    }

    @FXML
    private void onLoadFile(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open JFXScad File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "JFXScad files (*.jfxscad, *.groovy)",
                        "*.jfxscad", "*.groovy"));

        File f = fileChooser.showOpenDialog(null);

        if (f == null) {
            return;
        }

        String fName = f.getAbsolutePath();

        if (!fName.toLowerCase().endsWith(".groovy")
                && !fName.toLowerCase().endsWith(".jfxscad")) {
            fName += ".jfxscad";
        }

        try {
            mCodeEditorControl.setCode(Files.readString(Paths.get(fName), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.error("Error loading file " + f.getAbsolutePath(), ex);
        }
    }

    @FXML
    private void onSaveFile(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save JFXScad File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "JFXScad files (*.jfxscad, *.groovy)",
                        "*.jfxscad", "*.groovy"));

        File f = fileChooser.showSaveDialog(null);

        if (f == null) {
            return;
        }

        String fName = f.getAbsolutePath();

        if (!fName.toLowerCase().endsWith(".groovy")
                && !fName.toLowerCase().endsWith(".jfxscad")) {
            fName += ".jfxscad";
        }

        try {
            Files.writeString(Paths.get(fName), mCodeEditorControl.getCode(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Error saving file " + f.getAbsolutePath(), ex);
        }
    }

    @FXML
    private void onExportAsPngFile(ActionEvent e) {
        if (mCSGObject == null) {
            Alert dialog = new Alert(AlertType.ERROR, "Cannot export PNG. There is no geometry :(", ButtonType.OK);
            dialog.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export PNG File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image files (*.png)",
                        "*.png"));

        Path p = fileChooser.showSaveDialog(null).toPath();

        if (p == null) {
            return;
        }

        String fName = p.getFileName().toString();

        if (!fName.toLowerCase().endsWith(".png")) {
            p = p.resolveSibling(fName + ".png");
        }

        int snWidth = 1024;
        int snHeight = 1024;

        Image snapshot = mThreeDViewControl.takeSnapshot(snWidth, snHeight);
        try {
            ImageUtils.saveImage(snapshot, ".png", p);
        } catch (IOException ex) {
            log.error("Error exporting file to PNG: " + p, ex);
        }
    }

    @FXML
    protected void onCompileAndRun(ActionEvent e) {
        compile(mCodeEditorControl.getCode());
    }

    @FXML
    protected void onClose(ActionEvent e) {
        shutdown();
    }

    @FXML
    protected void onAutoCompile(ActionEvent e) {
        setAutoCompile(!isAutoCompile());
    }

    @FXML
    protected void onShowLogOutput(ActionEvent e) {
        mLogOutputViewManager.ensureVisible(mPrimaryStage, false, false);
    }

    @FXML
    protected void onResetPerspective(ActionEvent e) {
        DockSystem.resetPerspective();
    }
}
