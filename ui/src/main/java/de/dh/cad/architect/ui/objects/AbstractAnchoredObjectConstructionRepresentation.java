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
package de.dh.cad.architect.ui.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.ui.view.construction.Abstract2DView;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class AbstractAnchoredObjectConstructionRepresentation extends Abstract2DRepresentation {
    protected static class AnchorClickContext {
        private Map<String, AnchorConstructionRepresentation> BaseAnchors = new TreeMap<>();
        private EventHandler<MouseEvent> MouseEnteredHandler;
        private EventHandler<MouseEvent> AnchorMouseMovedHandler;
        private EventHandler<MouseEvent> MouseExitedHandler;
        private EventHandler<MouseEvent> MouseClickedHandler;
        private Predicate<AnchorConstructionRepresentation> ForceAnchorVisible;

        private boolean IsMouseOverOwner = false;
        private Map<String, AnchorConstructionRepresentation> MouseOverAnchors = new TreeMap<>();
    }

    protected AnchorClickContext mAnchorClickContext = null;

    protected AbstractAnchoredObjectConstructionRepresentation(BaseAnchoredObject modelObject, Abstract2DView parentView) {
        super(modelObject, parentView);
    }

    @Override
    public ConstructionView getParentView() {
        return (ConstructionView) mParentView;
    }

    @Override
    public BaseAnchoredObject getModelObject() {
        return (BaseAnchoredObject) super.getModelObject();
    }

    public Collection<AnchorConstructionRepresentation> getAnchorRepresentations() {
        if (mModelObject instanceof BaseAnchoredObject bao) {
            List<Anchor> anchors = bao.getAnchors();
            Collection<AnchorConstructionRepresentation> result = new ArrayList<>(anchors.size());
            for (Anchor anchor : anchors) {
                AnchorConstructionRepresentation anchorRepr = (AnchorConstructionRepresentation) mParentView.getRepresentationByModelId(anchor.getId());
                if (anchorRepr == null) {
                    continue;
                }
                result.add(anchorRepr);
            }
            return result;
        }
        return Collections.emptyList();
    }

    protected void checkAnchorVisibility(AnchorClickContext anchorVisibilityContext) {
        for (AnchorConstructionRepresentation aRepr : anchorVisibilityContext.BaseAnchors.values()) {
            boolean visible = // Show the anchor if ...
                    anchorVisibilityContext.ForceAnchorVisible.test(aRepr) // The client forces us so (perhaps the anchor is so important that it should remain visible...?)
                    || anchorVisibilityContext.IsMouseOverOwner // the mouse is over the owner, or ...
                    || anchorVisibilityContext.MouseOverAnchors.containsKey(aRepr.getModelId()); // the mouse is over the anchor itself
                aRepr.setVisible(visible);
        }
    }

    public boolean installAnchorChoiceFeature(Predicate<Anchor> anchorFilter,
        Consumer<AnchorConstructionRepresentation> anchorAimedHandler, Consumer<AnchorConstructionRepresentation> anchorClickHandler) {
        return installAnchorChoiceFeature(anchorFilter, null, null, anchorAimedHandler, anchorClickHandler, null);
    }

    public boolean installAnchorChoiceFeature(Predicate<Anchor> visibleAnchorsFilter,
        Consumer<AbstractAnchoredObjectConstructionRepresentation> objectAimedHandler,
        Consumer<AbstractAnchoredObjectConstructionRepresentation> objectClickHandler,
        Consumer<AnchorConstructionRepresentation> anchorAimedHandler,
        Consumer<AnchorConstructionRepresentation> anchorClickHandler,
        Predicate<AnchorConstructionRepresentation> forceAnchorVisible) {
        if (mAnchorClickContext != null) {
            return false;
        }

        mAnchorClickContext = new AnchorClickContext();
        mAnchorClickContext.ForceAnchorVisible = forceAnchorVisible == null ? (ar -> false) : forceAnchorVisible;

        mAnchorClickContext.MouseEnteredHandler = new EventHandler<>() {
            @Override
            public void handle(MouseEvent event) {
                Object source = event.getSource();
                if (source == AbstractAnchoredObjectConstructionRepresentation.this) {
                    mAnchorClickContext.IsMouseOverOwner = true;
                    if (objectAimedHandler != null) {
                        objectAimedHandler.accept(AbstractAnchoredObjectConstructionRepresentation.this);
                    }
                } else if (source instanceof AnchorConstructionRepresentation anchorRepr) {
                    mAnchorClickContext.MouseOverAnchors.put(anchorRepr.getModelId(), anchorRepr);
                    if (anchorAimedHandler != null) {
                        anchorAimedHandler.accept(anchorRepr);
                    }
                }
                checkAnchorVisibility(mAnchorClickContext);
                event.consume();
            }
        };
        // Mouse move handler is necessary to consume the move event before the general move handler comes into play
        mAnchorClickContext.AnchorMouseMovedHandler = new EventHandler<>() {
            @Override
            public void handle(MouseEvent event) {
                Object source = event.getSource();
                if (source == AbstractAnchoredObjectConstructionRepresentation.this) {
                    if (objectAimedHandler != null) {
                        objectAimedHandler.accept(AbstractAnchoredObjectConstructionRepresentation.this);
                    }
                } else if (source instanceof AnchorConstructionRepresentation anchorRepr) {
                    if (anchorAimedHandler != null) {
                        anchorAimedHandler.accept(anchorRepr);
                    }
                }
                event.consume();
            }
        };
        mAnchorClickContext.MouseExitedHandler = new EventHandler<>() {
            @Override
            public void handle(MouseEvent event) {
                Object source = event.getSource();
                if (source == AbstractAnchoredObjectConstructionRepresentation.this) {
                    mAnchorClickContext.IsMouseOverOwner = false;
                    if (objectAimedHandler != null) {
                        objectAimedHandler.accept(null);
                    }
                } else if (source instanceof AnchorConstructionRepresentation anchorRepr) {
                    mAnchorClickContext.MouseOverAnchors.remove(anchorRepr.getModelId());
                    if (anchorAimedHandler != null) {
                        anchorAimedHandler.accept(null);
                    }
                }
                checkAnchorVisibility(mAnchorClickContext);
                event.consume();
            }
        };
        addEventHandler(MouseEvent.MOUSE_ENTERED, mAnchorClickContext.MouseEnteredHandler);
        addEventHandler(MouseEvent.MOUSE_EXITED, mAnchorClickContext.MouseExitedHandler);

        for (Anchor anchor : getModelObject().getAnchors()) {
            AnchorConstructionRepresentation anchorRepr = (AnchorConstructionRepresentation) getParentView().getRepresentationByModelId(anchor.getId());
            String anchorModelId = anchorRepr.getModelId();
            if (visibleAnchorsFilter != null && visibleAnchorsFilter.test(anchor)) {
                mAnchorClickContext.BaseAnchors.put(anchorModelId, anchorRepr);
                anchorRepr.addEventHandler(MouseEvent.MOUSE_ENTERED, mAnchorClickContext.MouseEnteredHandler);
                anchorRepr.addEventHandler(MouseEvent.MOUSE_MOVED, mAnchorClickContext.AnchorMouseMovedHandler);
                anchorRepr.addEventHandler(MouseEvent.MOUSE_EXITED, mAnchorClickContext.MouseExitedHandler);
                EventHandler<? super MouseEvent> anchorMouseClickHandler = event -> {
                    if (event.getButton() != MouseButton.PRIMARY
                            || event.isSecondaryButtonDown()
                            || event.isMiddleButtonDown()) {
                        // We only react to primary button clicks without other buttons
                        return;
                    }
                    event.consume();
                    anchorClickHandler.accept(anchorRepr);
                };
                anchorRepr.setOnMouseClicked(anchorMouseClickHandler);
            }
        }

        if (objectClickHandler != null) {
            mAnchorClickContext.MouseClickedHandler = new EventHandler<>() {
                @Override
                public void handle(MouseEvent event) {
                    objectClickHandler.accept(AbstractAnchoredObjectConstructionRepresentation.this);
                    event.consume();
                }
            };
            addEventHandler(MouseEvent.MOUSE_CLICKED, mAnchorClickContext.MouseClickedHandler);
        } else {
            mAnchorClickContext.MouseClickedHandler = null;
        }
        return true;
    }

    public void uninstallAnchorChoiceFeature() {
        if (mAnchorClickContext == null) {
            return;
        }

        removeEventHandler(MouseEvent.MOUSE_ENTERED, mAnchorClickContext.MouseEnteredHandler);
        removeEventHandler(MouseEvent.MOUSE_EXITED, mAnchorClickContext.MouseExitedHandler);
        if (mAnchorClickContext.MouseClickedHandler != null) {
            removeEventHandler(MouseEvent.MOUSE_CLICKED, mAnchorClickContext.MouseClickedHandler);
        }

        for (AnchorConstructionRepresentation anchorRepr : mAnchorClickContext.BaseAnchors.values()) {
            anchorRepr.removeEventHandler(MouseEvent.MOUSE_ENTERED, mAnchorClickContext.MouseEnteredHandler);
            anchorRepr.removeEventHandler(MouseEvent.MOUSE_MOVED, mAnchorClickContext.AnchorMouseMovedHandler);
            anchorRepr.removeEventHandler(MouseEvent.MOUSE_EXITED, mAnchorClickContext.MouseExitedHandler);
            anchorRepr.setOnMouseClicked(null);
        }
        mAnchorClickContext = null;
    }
}

