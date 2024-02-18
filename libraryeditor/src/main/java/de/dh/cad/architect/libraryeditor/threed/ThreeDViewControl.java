package de.dh.cad.architect.libraryeditor.threed;

import java.util.Collection;

import de.dh.cad.architect.fx.nodes.objviewer.CoordinateSystemConfiguration;
import de.dh.cad.architect.fx.nodes.objviewer.ThreeDObjectViewControl;
import de.dh.cad.architect.libraryeditor.LibraryEditorConfiguration;
import de.dh.cad.architect.libraryeditor.Strings;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.MeshView;

public class ThreeDViewControl extends BorderPane {
    protected final ThreeDObjectViewControl mThreeDViewControl = new ThreeDObjectViewControl(CoordinateSystemConfiguration.architect());

    protected CheckBox mShowCoordinateSystemCheckBox = new CheckBox(Strings.LIBRARY_EDITOR_SHOW_COORDINATE_SYSTEM_CHECKBOX);

    public ThreeDViewControl() {
        setCenter(mThreeDViewControl);
        setTop(mShowCoordinateSystemCheckBox);

        mShowCoordinateSystemCheckBox.selectedProperty().addListener((prop, oldVal, newVal) -> {
            checkCoordinateSystemVisible();
        });
        checkCoordinateSystemVisible();
    }

    public static ThreeDViewControl create() {
        return new ThreeDViewControl();
    }

    protected void checkCoordinateSystemVisible() {
        mThreeDViewControl.setCoordinateSystemVisible(mShowCoordinateSystemCheckBox.isSelected());
    }

    public void setMeshes(Collection<MeshView> meshViews) {
        Group meshesGroup = new Group(meshViews.stream().map(mv -> (Node) mv).toList());
        mThreeDViewControl.setObjView(meshesGroup);
    }

    public Image takeSnapshot(int imageWidth, int imageHeight) {
        return mThreeDViewControl.takeSnapshot(imageWidth, imageHeight);
    }

    public void loadSettings(LibraryEditorConfiguration config) {
        mShowCoordinateSystemCheckBox.setSelected(config.isShowCoordinateSystem());
    }

    public void saveSettings(LibraryEditorConfiguration config) {
        config.setShowCoordinateSystem(mShowCoordinateSystemCheckBox.isSelected());
    }
}
