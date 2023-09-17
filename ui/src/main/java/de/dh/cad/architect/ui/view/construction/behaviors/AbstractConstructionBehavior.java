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
package de.dh.cad.architect.ui.view.construction.behaviors;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import de.dh.cad.architect.model.changes.IModelChange;
import de.dh.cad.architect.model.coords.Dimensions2D;
import de.dh.cad.architect.model.coords.Length;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.model.wallmodel.WallDockEnd;
import de.dh.cad.architect.ui.Constants;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.controller.UiController;
import de.dh.cad.architect.ui.controller.UiController.DockConflictStrategy;
import de.dh.cad.architect.ui.dialogs.DivideWallLengthDialog;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.objects.BaseObjectUIRepresentation;
import de.dh.cad.architect.ui.objects.CeilingConstructionRepresentation;
import de.dh.cad.architect.ui.objects.WallReconciler;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.AbstractViewBehavior;
import de.dh.cad.architect.ui.view.DragControl;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.feedback.wall.endings.DockedWallEnding;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.shape.Shape;

public abstract class AbstractConstructionBehavior extends AbstractViewBehavior<Abstract2DRepresentation, Abstract2DAncillaryObject> {
    protected EventHandler<ScrollEvent> mZoomEventHandler = null;

    public AbstractConstructionBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode) {
        super(parentMode);
    }

    @Override
    public ConstructionView getView() {
        return (ConstructionView) mView;
    }

    /**
     * Returns all visible objects whose shape intersects the given shape.
     * The returned collection only contains objects which are not hidden and which are visible regarding the {@link #getUIElementFilter() UI element filter}.
     */
    public Collection<Abstract2DRepresentation> getIntersectingObjects(Shape checkShape) {
        ConstructionView view = getView();

        Collection<Abstract2DRepresentation> objs = new ArrayList<>(200);
        for (Abstract2DRepresentation repr : view.getAllRepresentations()) {
            if (!mUIElementFilter.isUIElementVisible(repr) || mUIElementFilter.isUIElementMouseTransparent(repr)) {
                continue;
            }
            if (!repr.intersects(checkShape)) {
                continue;
            }
            objs.add(repr);
        }
        return objs;
    }

    // To be overridden
    @Override
    public void setDefaultUserHint() {
        int numSelectedObjects = getUiController().selectedObjectIds().size();
        if (numSelectedObjects == 0) {
            setUserHint(Strings.CONSTRUCTION_BEHAVIOR_USER_HINT);
        } else {
            setUserHint(MessageFormat.format(Strings.CONSTRUCTION_BEHAVIOR_USER_HINT_N_OBJECTS_SELECTED, numSelectedObjects));
        }
    }

    @Override
    protected void configureDefaultObjectHandlers(Abstract2DRepresentation repr) {
        repr.enableMouseOverSpot();
        repr.objectSpottedProperty().removeListener(OBJECT_SPOTTED_LISTENER);
        repr.objectSpottedProperty().addListener(OBJECT_SPOTTED_LISTENER);
        repr.setOnMouseClicked(MOUSE_CLICK_HANDLER_SELECT_OBJECT);
    }

    @Override
    protected void unconfigureDefaultObjectHandlers(Abstract2DRepresentation repr) {
        repr.disableMouseOverSpot();
        repr.objectSpottedProperty().removeListener(OBJECT_SPOTTED_LISTENER);
        repr.setOnMouseClicked(null);
    }

    @Override
    protected void installDefaultViewHandlers() {
        super.installDefaultViewHandlers();
        // Extracted as extra method to enable sub classes to prevent default handlers
        enableMouseZoomPlanGesture();
        enableMouseMovePlanGesture();
        getView().setOnMouseClicked(mouseEvent -> {
            // Check if primary button...
            MouseButton button = mouseEvent.getButton();
            if (button != MouseButton.PRIMARY) {
                return;
            }
            // ... and not moving
            if (!mouseEvent.isStillSincePress()) {
                return;
            }
            mouseEvent.consume();
            getUiController().selectedObjectIds().clear();
        });
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
        installDefaultDeleteObjectsKeyHandler();
    }

    @Override
    protected void uninstallDefaultViewHandlers() {
        super.uninstallDefaultViewHandlers();
        uninstallDefaultDeleteObjectsKeyHandler();
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        getView().setOnMouseClicked(null);
        disableMouseMovePlanGesture();
        disableMouseZoomPlanGesture();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods supporting default use-cases like adding default event handlers etc.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // To be called in #install()
    protected void enableMouseMovePlanGesture() {
        DragControl dragControl = new DragControl();
        ConstructionView view = getView();

        view.setOnMousePressed(event -> {
            if (!event.isMiddleButtonDown()) {
                return;
            }
            view.getScene().setCursor(Cursor.MOVE);
            // Record the last mouse event's position
            dragControl.setPoint(new Point2D(event.getSceneX(), event.getSceneY()));
            event.consume();
        });
        view.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.MIDDLE) {
                return;
            }
            view.getScene().setCursor(Cursor.DEFAULT);
        });
        view.setOnMouseDragged(event -> {
            if (!event.isMiddleButtonDown()) {
                return;
            }

            Point2D oldDragPoint = dragControl.getPoint();
            double deltaX = event.getSceneX() - oldDragPoint.getX();
            double deltaY = event.getSceneY() - oldDragPoint.getY();
            dragControl.setPoint(new Point2D(event.getSceneX(), event.getSceneY()));

            try {
                Point2D transform = view.getRootTransform().inverseDeltaTransform(deltaX, deltaY);
                view.translate(transform.getX(), transform.getY());
            } catch (Exception e) {
                throw new RuntimeException("Unable to translate plan coordinates", e);
            }
            event.consume();
        });
    }

    // To be called in #uninstall()
    protected void disableMouseMovePlanGesture() {
        ConstructionView view = getView();
        view.setOnMousePressed(null);
        view.setOnMouseReleased(null);
        view.setOnMouseDragged(null);
    }

    // To be called in #install()
    protected void enableMouseZoomPlanGesture() {
        if (mZoomEventHandler == null) {
            ConstructionView view = getView();
            mZoomEventHandler = new EventHandler<>() {
                @Override
                public void handle(ScrollEvent event) {
                    double coefficient = event.getDeltaY() < 0 ? 1.1 : 0.9;
                    view.scale(coefficient, event.getSceneX(), event.getSceneY());
                }
            };
            view.addEventHandler(ScrollEvent.SCROLL, mZoomEventHandler);
        }
    }

    // To be called in #uninstall()
    protected void disableMouseZoomPlanGesture() {
        if (mZoomEventHandler != null) {
            ConstructionView view = getView();
            view.removeEventHandler(ScrollEvent.SCROLL, mZoomEventHandler);
            mZoomEventHandler = null;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Default actions
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected IContextAction createAddWallAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_CREATE_WALL_TITLE;
            }

            @Override
            public void execute() {
                mParentMode.setBehavior(new GroundPlanStartWallBehavior(mParentMode));
            }
        };
    }

    protected IContextAction createAddWallAction(Anchor startWallHandle) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_CONTINUE_WALL_TITLE;
            }

            @Override
            public void execute() {
                mParentMode.setBehavior(GroundPlanAddWallBehavior.startingWithStartWallEnding(new DockedWallEnding(startWallHandle),
                        Constants.DEFAULT_WALL_THICKNESS, mParentMode));
            }
        };
    }

    protected IContextAction createAddDimensioningAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_ADD_DIMENSIONING_TITLE;
            }

            @Override
            public void execute() {
                mParentMode.setBehavior(new GroundPlanStartDimensioningBehavior(mParentMode));
            }
        };
    }

    protected IContextAction createAddFloorAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_ADD_FLOOR_TITLE;
            }

            @Override
            public void execute() {
                mParentMode.setBehavior(new GroundPlanAddFloorBehavior(mParentMode));
            }
        };
    }

    protected IContextAction createAddCeilingAction() {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_ADD_CEILING_TITLE;
            }

            @Override
            public void execute() {
                mParentMode.setBehavior(new GroundPlanAddCeilingBehavior(mParentMode));
            }
        };
    }

    protected IContextAction createAddCeilingAction(Anchor anchor3D1, Anchor anchor3D2, Anchor anchor3D3) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return MessageFormat.format(Strings.ACTION_GROUND_PLAN_ADD_CEILING_BETWEEN_ANCHORS_TITLE,
                    BaseObjectUIRepresentation.getShortName(anchor3D1),
                    BaseObjectUIRepresentation.getShortName(anchor3D2),
                    BaseObjectUIRepresentation.getShortName(anchor3D3));
            }

            @Override
            public void execute() {
                List<IModelChange> changeTrace = new ArrayList<>();
                Ceiling ceiling = Ceiling.create(
                    BaseObjectUIRepresentation.generateSimpleName(getPlan().getCeilings().values(), Ceiling.class),
                    anchor3D1.getPosition3D(Length.ZERO),
                    anchor3D2.getPosition3D(Length.ZERO),
                    anchor3D3.getPosition3D(Length.ZERO),
                    getPlan(), changeTrace);
                UiController uiController = getUiController();
                uiController.doDock(ceiling.getAnchorA(), anchor3D1, DockConflictStrategy.SkipDock, changeTrace);
                uiController.doDock(ceiling.getAnchorB(), anchor3D2, DockConflictStrategy.SkipDock, changeTrace);
                uiController.doDock(ceiling.getAnchorC(), anchor3D3, DockConflictStrategy.SkipDock, changeTrace);
                uiController.notifyChange(changeTrace, Strings.CEILING_ADD_CHANGE);

                uiController.setSelectedObjectId(ceiling.getId());
            }
        };
    }

    protected IContextAction createAddCoveringAction(Anchor anchor3D1, Anchor anchor3D2, Anchor anchor3D3) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return MessageFormat.format(Strings.ACTION_GROUND_PLAN_ADD_COVERING_BETWEEN_ANCHORS_TITLE,
                    BaseObjectUIRepresentation.getShortName(anchor3D1),
                    BaseObjectUIRepresentation.getShortName(anchor3D2),
                    BaseObjectUIRepresentation.getShortName(anchor3D3));
            }

            @Override
            public void execute() {
                List<IModelChange> changeTrace = new ArrayList<>();
                Covering covering = Covering.create(
                    BaseObjectUIRepresentation.generateSimpleName(getPlan().getCoverings().values(), Covering.class),
                    anchor3D1.requirePosition3D(),
                    anchor3D2.requirePosition3D(),
                    anchor3D3.requirePosition3D(),
                    getPlan(), changeTrace);
                UiController uiController = getUiController();
                uiController.doDock(covering.getAnchorA(), anchor3D1, DockConflictStrategy.SkipDock, changeTrace);
                uiController.doDock(covering.getAnchorB(), anchor3D2, DockConflictStrategy.SkipDock, changeTrace);
                uiController.doDock(covering.getAnchorC(), anchor3D3, DockConflictStrategy.SkipDock, changeTrace);
                uiController.notifyChange(changeTrace, Strings.COVERING_ADD_CHANGE);

                uiController.setSelectedObjectId(covering.getId());
            }
        };
    }

    protected IContextAction createEditCeilingAnchorsAction(Ceiling ceiling) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_EDIT_CEILING_ANCHORS_TITLE;
            }

            @Override
            public void execute() {
                CeilingConstructionRepresentation ceilingRepr = (CeilingConstructionRepresentation) getView().getRepresentationByModelId(ceiling.getId());
                mParentMode.setBehavior(EditCeilingAnchorsBehavior.create(ceilingRepr, mParentMode));
            }
        };
    }

    protected IContextAction createAddWallHoleAction(Wall ownerWall) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_ADD_WALL_HOLE_TITLE;
            }

            @Override
            public void execute() {
                UiController uiController = getUiController();
                Length wallSideLength = ownerWall.calculateBaseLength();
                Length windowWidth;
                Length distanceFromEnd;
                Length windowHeight = Length.ofM(1.1);
                if (wallSideLength.gt(Length.ofM(1.3))) {
                    windowWidth = Length.ofM(1.1);
                    distanceFromEnd = wallSideLength.minus(windowWidth).divideBy(2);
                } else {
                    windowWidth = wallSideLength.minus(Length.ofCM(20));
                    distanceFromEnd = Length.ofCM(10);
                }
                List<IModelChange> changeTrace = new ArrayList<>();
                WallHole wallHole = WallHole.createFromParameters(
                    BaseObjectUIRepresentation.generateSimpleName(ownerWall.getWallHoles(), WallHole.class),
                    Length.ofCM(100), new Dimensions2D(windowWidth, windowHeight),
                    WallDockEnd.A, distanceFromEnd, ownerWall, changeTrace);
                uiController.notifyChange(changeTrace, Strings.WALL_HOHE_ADD_CHANGE);
                uiController.setSelectedObjectId(wallHole.getId());
            }
        };
    }

    protected IContextAction createRemoveWallHoleAction(WallHole wallHole) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_REMOVE_WALL_HOLE_TITLE;
            }

            @Override
            public void execute() {
                UiController uiController = getUiController();
                uiController.setSelectedObjectId(wallHole.getWall().getId());
                uiController.removeObject(wallHole);
            }
        };
    }

    protected IContextAction createDivideWallLengthAction(Wall wall) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return Strings.ACTION_GROUND_PLAN_DIVIDE_WALL_LENGTH_TITLE;
            }

            @Override
            public void execute() {
                DivideWallLengthDialog dialog = new DivideWallLengthDialog(wall.calculateBaseLength());
                dialog.showAndWait().ifPresent(divideLength -> {
                    WallReconciler.divideWall(wall, dialog.getBreakPointDistance(), WallDockEnd.A, getUiController());
                });
            }
        };
    }

    /**
     * Returns whether the given walls are docked at a handle anchor, i.e. we can provide
     * the "join walls" action to make a single wall from the two walls by removing the connecting anchor.
     */
    protected boolean canJoinWalls(Wall wall1, Wall wall2) {
        Optional<Anchor> joiningAnchor = WallReconciler.getJoiningAnchor(wall1, wall2);
        return joiningAnchor.isPresent() && WallReconciler.canStraightenWallBendPoint(joiningAnchor.get());
    }

    protected void tryStraightenWallBendPoint(Anchor joiningAnchor) {
        /* Find anchors of the wall bend point which are docked - notify the user to avoid losing important docks?
        SingleWallBendPoint.fromAnchorDock(joiningAnchor).map(wd -> {
            Collection<Anchor> cornerAnchors = new ArrayList<>();
            // Find all corner anchors which could be docked
            for (Anchor dockedWallAnchor : wallDock.getAnchors()) {
                Wall ownerWall = (Wall) dockedWallAnchor.getOwner();
                if (Wall.isWallHandleAnchorA(dockedWallAnchor)) {
                    cornerAnchors.addAll(ownerWall.getWallEndView(WallDockEnd.A).getAllCornerAnchors());
                } else if (Wall.isWallHandleAnchorB(dockedWallAnchor)) {
                    cornerAnchors.addAll(ownerWall.getWallEndView(WallDockEnd.B).getAllCornerAnchors());
                }
            }
            // Remove undocked anchors
            for (Iterator<Anchor> ia = cornerAnchors.iterator(); ia.hasNext();) {
                Anchor cornerAnchorAtBendPoint = ia.next();
                if (cornerAnchorAtBendPoint.getAllDockedAnchors().size() > 1) {
                    // Bendpoint anchor is docked, retain it in collection
                } else {
                    ia.remove();
                }
            }
            return cornerAnchors;
        }).ifPresent(cornerAnchors -> {
            // Notify the user?
        });
        */

        UiController uiController = getUiController();
        WallReconciler.straightenWallBendPoint(joiningAnchor, uiController);
    }

    protected IContextAction createJoinWallsAction(Wall wall1, Wall wall2) {
        return new IContextAction() {
            @Override
            public String getTitle() {
                return MessageFormat.format(Strings.ACTION_GROUND_PLAN_JOIN_WALLS_TITLE, BaseObjectUIRepresentation.getShortName(wall1), BaseObjectUIRepresentation.getShortName(wall2));
            }

            @Override
            public void execute() {
                Optional<Anchor> oJoiningAnchor = WallReconciler.getJoiningAnchor(wall1, wall2);
                oJoiningAnchor.ifPresent(joiningAnchor -> {
                    tryStraightenWallBendPoint(joiningAnchor);
                });
            }
        };
    }
}