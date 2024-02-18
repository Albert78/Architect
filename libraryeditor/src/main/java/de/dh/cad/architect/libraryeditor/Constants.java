package de.dh.cad.architect.libraryeditor;

import java.net.URL;

import javafx.scene.paint.Color;

public class Constants {
    public static final Color FOCUSED_COLOR = Color.BLUE.interpolate(Color.DEEPSKYBLUE, 0.5);
    public static final Color UNFOCUSED_COLOR = Color.WHITE;
    public static final URL APPLICATION_CSS = Constants.class.getResource("application.css");
    public static final String LIBRARY_EDITOR_LAYOUT_FILE_NAME = "LibraryEditorLayout.xml";
}
