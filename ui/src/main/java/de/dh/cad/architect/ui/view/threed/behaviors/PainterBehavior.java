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
package de.dh.cad.architect.ui.view.threed.behaviors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.fx.nodes.CombinedTransformGroup;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.ui.IConfig;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.objects.Abstract3DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation;
import de.dh.cad.architect.ui.objects.Abstract3DRepresentation.ObjectSurface;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.utils.Cursors;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.InteractionsTab;
import de.dh.cad.architect.ui.view.threed.ThreeDView;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class PainterBehavior extends Abstract3DViewBehavior {
    private static final Logger log = LoggerFactory.getLogger(PainterBehavior.class);

    protected static String KEY_LAST_MATERIAL_REF = PainterBehavior.class.getSimpleName() + ".LAST_MATERIAL_REF";

    protected static Cursor CURSOR_PAINT = Cursors.createCursorPaint();
    protected static Cursor CURSOR_PICK_MATERIAL = Cursors.createCursorPickMaterial();

    protected interface ISurfaceAction {
        void set(ObjectSurface surface);
        void reset(ObjectSurface surface);
        Cursor getMouseCursor();
        void commit();
    }

    protected class PaintSurfaceAction implements ISurfaceAction {
        protected final AssetRefPath mMaterial;
        protected AssetRefPath mOriginalMaterial = null;

        public PaintSurfaceAction(AssetRefPath materialRef) {
            mMaterial = materialRef;
        }

        @Override
        public void set(ObjectSurface surface) {
            mOriginalMaterial = surface.getMaterialRef();
            if (mMaterial != null) {
                if (!surface.assignMaterial(mMaterial)) {
                    setUserHint(MessageFormat.format(Strings.THREE_D_PAINTER_BEHAVIOR_UNABLE_TO_ASSIGN_MATERIAL, BaseObjectUIRepresentation.getShortName(surface.getOwnerRepr().getModelObject())));
                }
            }
        }

        @Override
        public void reset(ObjectSurface surface) {
            surface.assignMaterial(mOriginalMaterial);
        }

        @Override
        public Cursor getMouseCursor() {
            return CURSOR_PAINT;
        }

        @Override
        public void commit() {
            // Nothing to do
        }
    }

    protected class MaterialPipetAction implements ISurfaceAction {
        protected AssetRefPath mSelectedMaterial = null;

        @Override
        public void set(ObjectSurface surface) {
            mSelectedMaterial = surface.getMaterialRef();
        }

        @Override
        public void reset(ObjectSurface surface) {
            // Nothing to do
        }

        @Override
        public Cursor getMouseCursor() {
            return CURSOR_PICK_MATERIAL;
        }

        @Override
        public void commit() {
            mChoosePainterMaterialControl.setMaterial(mSelectedMaterial);
        }
    }

    protected class MouseAndKeyState {
        protected final boolean mMouseOverView;
        protected final boolean mControlDown;

        public MouseAndKeyState(boolean mouseOverView, boolean controlDown) {
            mMouseOverView = mouseOverView;
            mControlDown = controlDown;
        }

        public boolean isMouseOverView() {
            return mMouseOverView;
        }

        public boolean isControlDown() {
            return mControlDown;
        }
    }

    /**
     * Contains the state of the highlighted object surface.
     * The highlight mode/action can be switched between paint and pipet.
     */
    protected class SurfaceHighlight {
        protected final ObjectSurface mSurface;
        protected ISurfaceAction mAction = null;
        protected ChangeListener<MouseAndKeyState> mMouseAndKeyStateListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends MouseAndKeyState> observable, MouseAndKeyState oldValue, MouseAndKeyState newValue) {
                checkActionAndCursor();
            }
        };

        public SurfaceHighlight(ObjectSurface surface) {
            mSurface = surface;
            checkActionAndCursor();
            mMouseAndKeyStateProperty.addListener(mMouseAndKeyStateListener);
        }

        protected void dispose() {
            mMouseAndKeyStateProperty.removeListener(mMouseAndKeyStateListener);
            resetMouseCursor();
        }

        public void setAction(ISurfaceAction action) {
            if (mAction != null) {
                mAction.reset(mSurface);
            }
            mAction = action;
            mAction.set(mSurface);
            updateMouseCursor();
        }

        public void reset() {
            mAction.reset(mSurface);
            dispose();
        }

        protected void updateMouseCursor() {
            if (getMouseAndKeyState().isMouseOverView()) {
                getScene().setCursor(mAction.getMouseCursor());
            } else {
                resetMouseCursor();
            }
        }

        protected void resetMouseCursor() {
            getScene().setCursor(Cursor.DEFAULT);
        }

        protected void checkActionAndCursor() {
            if (getMouseAndKeyState().isControlDown()) {
                // Control is pressed - action should be (switched to) MaterialPipetAction
                if (!(mAction instanceof MaterialPipetAction)) {
                    setAction(new MaterialPipetAction());
                }
            } else {
                // Control is not pressed - action should be (switched to) PaintSurfaceAction
                if (!(mAction instanceof PaintSurfaceAction)) {
                    setAction(new PaintSurfaceAction(getSelectedMaterial()));
                }
            }
            updateMouseCursor();
        }

        public void commit() {
            mAction.commit();
            dispose();
        }
    }

    protected EventHandler<MouseEvent> SET_MOUSE_OVER_EVENT_HANDLER = event -> {
        setMouseAndKeyState(true, event.isControlDown());
    };

    protected EventHandler<MouseEvent> RESET_MOUSE_OVER_EVENT_HANDLER = event -> {
        setMouseAndKeyState(false, event.isControlDown());
    };

    protected EventHandler<KeyEvent> CHECK_CONTROL_EVENT_HANDLER = event -> {
        setMouseAndKeyState(getMouseAndKeyState().isMouseOverView(), event.isControlDown());
    };

    protected ChangeListener<ObjectSurface> MOUSE_OVER_SURFACE_CHANGE_LISTENER = new ChangeListener<>() {
        @Override
        public void changed(ObservableValue<? extends ObjectSurface> observable, ObjectSurface oldValue, ObjectSurface newValue) {
            highlightSurface(newValue);
        }
    };

    protected SurfaceHighlight mHighlightedSurface = null;
    protected SimpleObjectProperty<MouseAndKeyState> mMouseAndKeyStateProperty = new SimpleObjectProperty<>();
    protected ChoosePainterMaterialControl mChoosePainterMaterialControl = null;
    protected AssetLoader mAssetLoader = null;

    public PainterBehavior(AbstractUiMode<Abstract3DRepresentation, Abstract3DAncillaryObject> parentMode) {
        super(parentMode);
    }

    protected void highlightSurface(ObjectSurface surface) {
        String surfaceTypeId = surface == null ? null : surface.getSurfaceTypeId();
        if (surfaceTypeId == null) {
            setDefaultUserHint();
        } else {
            @SuppressWarnings("null")
            Abstract3DRepresentation ownerRepr = surface.getOwnerRepr();
            setUserSurfaceHint(ownerRepr, surfaceTypeId);
        }
        if (mHighlightedSurface != null) {
            mHighlightedSurface.reset();
        }
        mHighlightedSurface = null;
        if (surface != null) {
            mHighlightedSurface = new SurfaceHighlight(surface);
        }
    }

    protected void setUserSurfaceHint(Abstract3DRepresentation ownerRepr, String surfaceId) {
        setUserHint(MessageFormat.format(Strings.THREE_D_PAINTER_BEHAVIOR_SURFACE_HINT, BaseObjectUIRepresentation.getShortName(ownerRepr.getModelObject()), surfaceId));
    }

    protected void commitHighlightedSurface() {
        if (mHighlightedSurface != null) {
            mHighlightedSurface.commit();
        }
        mHighlightedSurface = null;
    }

    protected boolean isMouseOverView() {
        CombinedTransformGroup transformedRoot = getView().getTransformedRoot();
        return transformedRoot.isHover();
    }

    protected void initMouseAndKeyState() {
        mMouseAndKeyStateProperty.set(new MouseAndKeyState(isMouseOverView(), false));
    }

    protected void setMouseAndKeyState(boolean mouseOverView, boolean controlDown) {
        mMouseAndKeyStateProperty.set(new MouseAndKeyState(mouseOverView, controlDown));
    }

    protected MouseAndKeyState getMouseAndKeyState() {
        return mMouseAndKeyStateProperty.get();
    }

    protected AssetRefPath getSelectedMaterial() {
        return mChoosePainterMaterialControl.getSelectedMaterial();
    }

    @Override
    protected void configureObject(Abstract3DRepresentation repr) {
        repr.mouseOverSurfaceProperty().removeListener(MOUSE_OVER_SURFACE_CHANGE_LISTENER);
        repr.mouseOverSurfaceProperty().addListener(MOUSE_OVER_SURFACE_CHANGE_LISTENER);
        repr.setOnMouseClicked(new EventHandler<>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                    commitHighlightedSurface();
                }
            }
        });
    }

    @Override
    protected void unconfigureObject(Abstract3DRepresentation repr, boolean objectRemoved) {
        repr.mouseOverSurfaceProperty().removeListener(MOUSE_OVER_SURFACE_CHANGE_LISTENER);
        repr.setOnMouseClicked(null);
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    protected void setDefaultUserHint() {
        setUserHint(Strings.THREE_D_PAINTER_BEHAVIOR_USER_HINT);
    }

    @Override
    public String getTitle() {
        return Strings.THREE_D_PAINTER_BEHAVIOR_TITLE;
    }

    @Override
    public ThreeDView getView() {
        return (ThreeDView) mView;
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        if (selectedObjects.size() == 1) {
//            BaseObject selectedObject = selectedObjects.get(0);

        }

        actions.add(createResetSupportObjectBehaviorAction());

        mActionsList.setAll(actions);
    }

    protected IContextAction createResetSupportObjectBehaviorAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_THREE_D_RESET_SUPPORT_OBJECT_SURFACES;
            }

            @Override
            public void execute() {
                mParentMode.setBehavior(new ResetSupportObjectSurfacesBehavior(mParentMode));
            }
        };
    }

    protected void createInteractionsTab() {
        Tab tab = new Tab(Strings.THREE_D_PAINTER_BEHAVIOR_MATERIAL_TAB);
        mChoosePainterMaterialControl = new ChoosePainterMaterialControl(getUiController());
        tab.setContent(mChoosePainterMaterialControl);
        InteractionsTab interactionsTab = new InteractionsTab(tab, true);
        setInteractionsTab(interactionsTab);
    }

    @Override
    public void install(AbstractPlanView<Abstract3DRepresentation, Abstract3DAncillaryObject> view) {
        super.install(view);
        initMouseAndKeyState();
        mAssetLoader = getUiController().getAssetManager().buildAssetLoader();
        createInteractionsTab();
        IConfig configuration = getUiController().getConfiguration();
        String lastMaterialRefStr = configuration.getString(KEY_LAST_MATERIAL_REF, null);
        if (lastMaterialRefStr != null) {
            try {
                AssetRefPath materialRef = AssetRefPath.parse(lastMaterialRefStr);
                mChoosePainterMaterialControl.setMaterial(materialRef);
            } catch (Exception e) {
                log.warn("Error reading last material ref", e);
            }
        }
        ThreeDView threeDView = getView();
        CombinedTransformGroup transformedRoot = threeDView.getTransformedRoot();
        transformedRoot.addEventHandler(MouseEvent.MOUSE_ENTERED, SET_MOUSE_OVER_EVENT_HANDLER);
        transformedRoot.addEventHandler(MouseEvent.MOUSE_EXITED, RESET_MOUSE_OVER_EVENT_HANDLER);
        Scene scene = getScene();
        scene.addEventHandler(KeyEvent.KEY_PRESSED, CHECK_CONTROL_EVENT_HANDLER);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, CHECK_CONTROL_EVENT_HANDLER);
    }

    @Override
    public void uninstall() {
        ThreeDView threeDView = getView();
        CombinedTransformGroup transformedRoot = threeDView.getTransformedRoot();
        transformedRoot.removeEventHandler(MouseEvent.MOUSE_ENTERED, SET_MOUSE_OVER_EVENT_HANDLER);
        transformedRoot.removeEventHandler(MouseEvent.MOUSE_EXITED, RESET_MOUSE_OVER_EVENT_HANDLER);
        Scene scene = getScene();
        scene.removeEventHandler(KeyEvent.KEY_PRESSED, CHECK_CONTROL_EVENT_HANDLER);
        scene.removeEventHandler(KeyEvent.KEY_RELEASED, CHECK_CONTROL_EVENT_HANDLER);

        IConfig configuration = getUiController().getConfiguration();
        AssetRefPath selectedMaterial = mChoosePainterMaterialControl.getSelectedMaterial();
        if (selectedMaterial != null) {
            configuration.setString(KEY_LAST_MATERIAL_REF, selectedMaterial.toPathString());
        }

        super.uninstall();
        highlightSurface(null);
    }
}