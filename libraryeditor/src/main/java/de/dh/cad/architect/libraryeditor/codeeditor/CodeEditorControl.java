package de.dh.cad.architect.libraryeditor.codeeditor;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.ScrollPane;

public class CodeEditorControl extends ScrollPane {
    private static final String[] KEYWORDS = new String[]{
        "def", "in", "as", "abstract", "assert", "boolean", "break", "byte",
        "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else",
        "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import",
        "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws",
        "transient", "try", "void", "volatile", "while"
    };

    protected static final Pattern KEYWORD_PATTERN
            = Pattern.compile("\\b(" + String.join("|", KEYWORDS) + ")\\b");

    protected final CodeArea mCodeArea = new CodeArea();

    public CodeEditorControl() {
        initialize();
        setFitToHeight(true);
        setFitToWidth(true);
    }

    protected void initialize() {
        mCodeArea.textProperty().addListener(
            (ov, oldText, newText) -> {
                Matcher matcher = KEYWORD_PATTERN.matcher(newText);
                int lastKwEnd = 0;
                StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
                while (matcher.find()) {
                    spansBuilder.add(Collections.emptyList(),
                            matcher.start() - lastKwEnd);
                    spansBuilder.add(Collections.singleton("keyword"),
                            matcher.end() - matcher.start());
                    lastKwEnd = matcher.end();
                }
                spansBuilder.add(Collections.emptyList(),
                        newText.length() - lastKwEnd);
                mCodeArea.setStyleSpans(0, spansBuilder.create());
            });

        setContent(mCodeArea);
    }

    public void setCode(String code) {
        mCodeArea.replaceText(code);
    }

    public String getCode() {
        return mCodeArea.getText();
    }

    public ObservableValue<String> getCodeProperty() {
        return mCodeArea.textProperty();
    }
}
