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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.ui.Strings;
import de.dh.cad.architect.ui.objects.Abstract2DAncillaryObject;
import de.dh.cad.architect.ui.objects.Abstract2DRepresentation;
import de.dh.cad.architect.ui.view.AbstractPlanView;
import de.dh.cad.architect.ui.view.AbstractUiMode;
import de.dh.cad.architect.ui.view.DragControl;
import de.dh.cad.architect.ui.view.IContextAction;
import de.dh.cad.architect.ui.view.construction.ConstructionView;
import de.dh.cad.architect.ui.view.construction.SelectionUIElementFilter;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

public class SelectionBehavior extends AbstractConstructionBehavior {
    protected final boolean mAutoRelease;
    protected final DragControl mDragControl = new DragControl();
    protected Rectangle mSelectionRectangle = null;

    public SelectionBehavior(AbstractUiMode<Abstract2DRepresentation, Abstract2DAncillaryObject> parentMode, boolean autoRelease) {
        super(parentMode);
        mAutoRelease = autoRelease;
        setUIElementFilter(new SelectionUIElementFilter());
    }

    @Override
    protected void updateActionsList(List<BaseObject> selectedObjects, List<BaseObject> selectedRootObjects) {
        Collection<IContextAction> actions = new ArrayList<>();

        // Delete
        if (!selectedRootObjects.isEmpty()) {
            actions.add(createRemoveObjectsAction(selectedRootObjects));
        }

        if (selectedObjects.isEmpty()) {
            // No object selected
        } else if (selectedObjects.size() == 1) {
            // One object selected
            BaseObject selectedObject = selectedObjects.get(0);
            if (selectedObject instanceof ObjectsGroup group) {
                // Ungrouping
                actions.add(createUngroupAction(group));
            }

            if (selectedObject instanceof Anchor) {
                //Anchor anchor = (Anchor) selectedObject;
                // No actions provided at the moment
            } else {
                // Selected object is not an anchor
            }
        } else {
            // More than 1 object selected

            // Grouping
            actions.add(createGroupAction(selectedObjects));
        }

        mActionsList.setAll(actions);
    }

    @Override
    protected boolean canHandleSelectionChange() {
        return true;
    }

    @Override
    public ConstructionView getView() {
        return (ConstructionView) mView;
    }

    protected Rectangle createSelectionRectangle() {
        Rectangle result = new Rectangle();
        result.setStroke(Color.BLUE);
        result.setStrokeType(StrokeType.OUTSIDE);
        result.getStrokeDashArray().addAll(5d, 10d);
        result.setStrokeWidth(1);
        result.setFill(Color.BLUE.deriveColor(1, 1, 1, 0.1));
        return result;
    }

    protected void updateSelectionRectangle(Point2D sceneFrom, Point2D sceneTo) {
        ConstructionView view = getView();
        Pane topLayer = view.getTopLayer();
        Point2D fromLocalAbs = topLayer.sceneToLocal(sceneFrom);
        Point2D toLocalAbs = topLayer.sceneToLocal(sceneTo);
        double fromLocalAbsX = fromLocalAbs.getX();
        double fromLocalAbsY = fromLocalAbs.getY();
        double toLocalAbsX = toLocalAbs.getX();
        double toLocalAbsY = toLocalAbs.getY();
        mSelectionRectangle.setX(fromLocalAbsX);
        mSelectionRectangle.setY(fromLocalAbsY);
        mSelectionRectangle.setWidth(toLocalAbsX - fromLocalAbsX + 1);
        mSelectionRectangle.setHeight(toLocalAbsY - fromLocalAbsY + 1);
    }

    /**
     * Selects all elements which are located inside the given range in scene coordinates.
     */
    public void selectInsideRange(Point2D sceneFrom, Point2D sceneTo) {
        updateSelectionRectangle(sceneFrom, sceneTo);
        getUiController().setSelectedObjectIds(getIntersectingObjects(mSelectionRectangle)
            .stream()
            .map(repr -> repr.getModelId())
            .collect(Collectors.toList()));
    }

    public void startDragging(double sceneX, double sceneY) {
        Point2D dragStart = new Point2D(sceneX, sceneY);

        getUiController().setSelectedObjectIds(Collections.emptyList());
        mDragControl.setPoint(dragStart);
        ConstructionView view = getView();
        Pane topLayer = view.getTopLayer();
        if (mSelectionRectangle == null) {
            mSelectionRectangle = createSelectionRectangle();
            topLayer.getChildren().add(mSelectionRectangle);
        }

        selectInsideRange(dragStart, dragStart);
    }

    public void dragTo(double sceneX, double sceneY) {
        if (mSelectionRectangle == null) {
            return;
        }
        Point2D dragStart = mDragControl.getPoint();
        Point2D position = new Point2D(sceneX, sceneY);
        double minX = dragStart.getX();
        double minY = dragStart.getY();
        double maxX = position.getX();
        double maxY = position.getY();

        // If dragged to the opposite direction, the coords must be inverted to make the rectangle's width and height always positive:
        if (maxX < minX) {
            double temp = minX;
            minX = maxX;
            maxX = temp;
        }
        if (maxY < minY) {
            double temp = minY;
            minY = maxY;
            maxY = temp;
        }

        selectInsideRange(new Point2D(minX, minY), new Point2D(maxX, maxY));
    }

    public void finishDrag() {
        Pane topLayer = getView().getTopLayer();
        if (mSelectionRectangle != null) {
            topLayer.getChildren().remove(mSelectionRectangle);
            mSelectionRectangle = null;

            if (mAutoRelease) {
                getParentMode().resetBehavior();
            }
        }
    }

    @Override
    public void install(AbstractPlanView<Abstract2DRepresentation, Abstract2DAncillaryObject> view) {
        super.install(view);
        ConstructionView constructionView = getView();
        Pane topLayer = constructionView.getTopLayer();
        topLayer.setMouseTransparent(false);

        topLayer.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            startDragging(event.getSceneX(), event.getSceneY());
            event.consume();
        });
        topLayer.setOnMouseDragged(event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            dragTo(event.getSceneX(), event.getSceneY());
            event.consume();
        });
        topLayer.setOnMouseReleased(event -> {
            finishDrag();
        });
        installDefaultSpaceToggleObjectVisibilityKeyHandler();
        installDefaultDeleteObjectsKeyHandler();
    }

    @Override
    public void uninstall() {
        uninstallDefaultDeleteObjectsKeyHandler();
        uninstallDefaultSpaceToggleObjectVisibilityKeyHandler();
        Pane topLayer = getView().getTopLayer();
        if (mSelectionRectangle != null) {
            topLayer.getChildren().remove(mSelectionRectangle);
            mSelectionRectangle = null;
        }
        topLayer.setOnMousePressed(null);
        topLayer.setOnMouseDragged(null);
        topLayer.setOnMouseReleased(null);
        topLayer.setMouseTransparent(true);
        super.uninstall();
    }

    @Override
    public String getTitle() {
        return Strings.CONSTRUCTION_SELECTION_BEHAVIOR_TITLE;
    }
}