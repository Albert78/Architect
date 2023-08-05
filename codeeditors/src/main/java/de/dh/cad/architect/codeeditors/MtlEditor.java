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
package de.dh.cad.architect.codeeditors;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.collection.ListModification;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class MtlEditor extends CodeArea {
    protected class VisibleParagraphStyler<PS, SEG, S> implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>> {
        private final GenericStyledArea<PS, SEG, S> mArea;
        private final Function<String,StyleSpans<S>> mComputeStyles;
        private int prevParagraph, mPrevTextLength;

        public VisibleParagraphStyler(GenericStyledArea<PS, SEG, S> area, Function<String,StyleSpans<S>> computeStyles) {
            mComputeStyles = computeStyles;
            mArea = area;
        }

        @Override
        public void accept(ListModification<? extends Paragraph<PS, SEG, S>> lm) {
            if (lm.getAddedSize() > 0) {
                int paragraph = Math.min(mArea.firstVisibleParToAllParIndex() + lm.getFrom(), mArea.getParagraphs().size() - 1);
                String text = mArea.getText(paragraph, 0, paragraph, mArea.getParagraphLength(paragraph));

                if (paragraph != prevParagraph || text.length() != mPrevTextLength) {
                    int startPos = mArea.getAbsolutePosition(paragraph, 0);
                    Platform.runLater( () -> mArea.setStyleSpans(startPos, mComputeStyles.apply(text)));
                    mPrevTextLength = text.length();
                    prevParagraph = paragraph;
                }
            }
        }
    }

    protected static final String[] KEYWORDS = new String[] {
            "newmtl",
            "Ka", "Kd", "Ks", "Tf",
            "illum", "d", "Ns",
            "sharpness", "Ni",
            "Map_Ka", "Map_Kd", "Map_Ks", "Map_Ns", "Map_d",
            "disp", "decal", "bump",
            "refl"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PARAM_PATTERN = "-\\w*";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<PARAM>" + PARAM_PATTERN + ")",
            Pattern.CASE_INSENSITIVE
    );

    public MtlEditor() {
        setParagraphGraphicFactory(LineNumberFactory.get(this));

        /*
        // Recompute the syntax highlighting for all text, 500 ms after user stops editing area
        // Note that this shows how it can be done but is not recommended for production with
        // large files as it does a full scan of ALL the text every time there is a change !
        Subscription cleanupWhenNoLongerNeedIt =

                // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
                // multi plain changes = save computation by not rerunning the code multiple times
                //   when making multiple changes (e.g. renaming a method at multiple parts in file)
                multiPlainChanges()

                // do not emit an event until 500 ms have passed since the last emission of previous stream
                .successionEnds(Duration.ofMillis(500))

                // run the following code block when previous stream emits an event
                .subscribe(ignore -> setStyleSpans(0, computeHighlighting(getText())));

        // When no longer need syntax highlighting and wish to clean up memory leaks
        // run: `cleanupWhenNoLongerNeedIt.unsubscribe();`
        */

        // Recompute syntax highlighting only for visible paragraph changes
        // Note that this shows how it can be done but is not recommended for production where multi-
        // line syntax requirements are needed, like comment blocks without a leading * on each line.
        getVisibleParagraphs().addModificationObserver(new VisibleParagraphStyler<>(this, this::computeHighlighting));

        // Auto-indent: insert previous line's indents on enter
        final Pattern whiteSpace = Pattern.compile("^\\s+");
        addEventHandler(KeyEvent.KEY_PRESSED, KE -> {
            if (KE.getCode() == KeyCode.ENTER) {
                int caretPosition = getCaretPosition();
                int currentParagraph = getCurrentParagraph();
                Matcher m0 = whiteSpace.matcher(getParagraph(currentParagraph - 1).getSegments().get(0));
                if (m0.find()) {
                    Platform.runLater(() -> insertText(caretPosition, m0.group()));
                }
            }
        });
        getStylesheets().add(MtlEditor.class.getResource("mtleditor.css").toExternalForm());
    }

    protected StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("PARAM") != null ? "param" :
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
