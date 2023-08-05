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
package de.dh.cad.architect.ui.objecttree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import de.dh.cad.architect.model.Plan;
import de.dh.cad.architect.model.objects.Anchor;
import de.dh.cad.architect.model.objects.BaseAnchoredObject;
import de.dh.cad.architect.model.objects.BaseObject;
import de.dh.cad.architect.model.objects.Ceiling;
import de.dh.cad.architect.model.objects.Covering;
import de.dh.cad.architect.model.objects.Dimensioning;
import de.dh.cad.architect.model.objects.Floor;
import de.dh.cad.architect.model.objects.GuideLine;
import de.dh.cad.architect.model.objects.ObjectsGroup;
import de.dh.cad.architect.model.objects.SupportObject;
import de.dh.cad.architect.model.objects.Wall;
import de.dh.cad.architect.model.objects.WallHole;
import de.dh.cad.architect.ui.Strings;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TreeItem;

public class RootTreeItem extends TreeItem<ITreeItemData> {
    protected final Plan mPlan;
    protected final MultiValuedMap<String, TreeItem<ITreeItemData>> mIdsToItems = new ArrayListValuedHashMap<>();
    protected final Map<String, TreeItem<ITreeItemData>> mCurrentTopLevelGroups = new TreeMap<>();
    protected final MultiValuedMap<String, String> mCurrentItemsToGroups = new ArrayListValuedHashMap<>();

    protected final TreeItem<ITreeItemData> mDimensioningsItem;
    protected final TreeItem<ITreeItemData> mFloorsItem;
    protected final TreeItem<ITreeItemData> mWallsItem;
    protected final TreeItem<ITreeItemData> mCeilingsItem;
    protected final TreeItem<ITreeItemData> mCoveringsItem;
    protected final TreeItem<ITreeItemData> mSupportObjectsItem;
    protected final TreeItem<ITreeItemData> mGuideLinesItem;
    protected final TreeItem<ITreeItemData> mObjectGroupsItem;

    private abstract class BaseObjectsCategoryTreeItemData extends CategoryTreeItemData {
        public BaseObjectsCategoryTreeItemData(String title) {
            super(title);
        }

        @Override
        public Optional<Boolean> isVisible() {
            Collection<? extends BaseObject> objects = getObjects();
            if (objects.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(objects.stream().filter(bo -> !bo.isHidden()).findAny().isPresent());
        }
    }

    public RootTreeItem(Plan plan) {
        mPlan = plan;
        ObservableList<TreeItem<ITreeItemData>> rootChildren = getChildren();
        mDimensioningsItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_DIMENSIONING_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getDimensionings().values();
            }
        });
        rootChildren.add(mDimensioningsItem);
        mFloorsItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_FLOOR_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getFloors().values();
            }
        });
        rootChildren.add(mFloorsItem);
        mWallsItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_WALL_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getWalls().values();
            }
        });
        rootChildren.add(mWallsItem);
        mCeilingsItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_CEILING_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getCeilings().values();
            }
        });
        rootChildren.add(mCeilingsItem);
        mCoveringsItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_COVERING_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getCoverings().values();
            }
        });
        rootChildren.add(mCoveringsItem);
        mSupportObjectsItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_SUPPORT_OBJECT_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getSupportObjects().values();
            }
        });
        rootChildren.add(mSupportObjectsItem);
        mGuideLinesItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_GUIDE_LINE_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getGuideLines().values();
            }
        });
        rootChildren.add(mGuideLinesItem);
        mObjectGroupsItem = new TreeItem<>(new BaseObjectsCategoryTreeItemData(Strings.OBJECT_TYPE_NAME_OBJECTS_GROUP_P) {
            @Override
            public Collection<? extends BaseObject> getObjects() {
                return mPlan.getGroups().values();
            }
        });
        rootChildren.add(mObjectGroupsItem);

        insertTreeItems(mPlan.getDimensionings().values());
        insertTreeItems(mPlan.getFloors().values());
        Collection<Wall> walls = mPlan.getWalls().values();
        insertTreeItems(walls);
        insertWallHoles(walls);
        insertTreeItems(mPlan.getCeilings().values());
        insertTreeItems(mPlan.getCoverings().values());
        insertTreeItems(mPlan.getSupportObjects().values());
        insertTreeItems(mPlan.getGuideLines().values());
        insertGroups();
        insertTreeItems(mPlan.getAnchors().values());
    }

    protected Collection<TreeItem<ITreeItemData>> getTreeItemsByObjectId(String id) {
        Collection<TreeItem<ITreeItemData>> result = mIdsToItems.get(id);
        return result == null ? Collections.emptyList() : result;
    }

    protected Optional<TreeItem<ITreeItemData>> getMainTreeItemByObjectId(String id) {
        return filterMainTreeItem(getTreeItemsByObjectId(id));
    }

    protected Optional<TreeItem<ITreeItemData>> filterMainTreeItem(Collection<TreeItem<ITreeItemData>> itemsOfObject) {
        return itemsOfObject.stream().filter(item -> (item.getValue() instanceof IObjectTreeItemData otid) && !otid.isShadowEntry()).findFirst();
    }

    public RootTreeItem create(Plan plan) {
        return new RootTreeItem(plan);
    }

    protected void insertTreeItems(Collection<? extends BaseObject> objs) {
        for (BaseObject obj : objs) {
            getOrCreateTreeItems(obj);
        }
    }

    protected void insertWallHoles(Collection<Wall> walls) {
        for (Wall wall : walls) {
            for (WallHole hole : wall.getWallHoles()) {
                getOrCreateTreeItems(hole);
            }
        }
    }

    protected void insertGroups() {
        checkGroups(mPlan.getTopLevelGroups().values()); // Ensure that all top-level group items are present
        Collection<ObjectsGroup> groups = mPlan.getGroups().values();
        checkGroups(groups);
        checkGroups(groups
            .stream()
            .flatMap(g -> g.getGroupedObjects().stream())
            .filter(o -> !(o instanceof ObjectsGroup))
            .collect(Collectors.toList()));
    }

    protected Collection<TreeItem<ITreeItemData>> getOrCreateTreeItems(BaseObject obj) {
        String objectId = obj.getId();
        Collection<TreeItem<ITreeItemData>> res = getTreeItemsByObjectId(objectId);
        if (!res.isEmpty()) {
            // Item already exists
            return res;
        }

        // Item doesn't exist yet, create item under all of its positions in the tree EXCEPT grouping tree.
        // The grouping tree is managed separately by method checkGroups which is called with the complete collection of
        // changed/added/removed objects to avoid redundant checks

        // We need to use different names (res, result) to make result effectively final
        Collection<TreeItem<ITreeItemData>> result = new ArrayList<>();
        if (obj instanceof Anchor a) {
            // Add under owner
            BaseAnchoredObject owner = a.getAnchorOwner();
            String ownerId = owner.getId();
            // Object addition changes arrive in an undefined order - thus we can get additions of anchors before we
            // are called for their owner object. So we try to create the owner object, if it is not present yet.
            getOrCreateTreeItems(owner);
            Optional<TreeItem<ITreeItemData>> oOwnerItem = getMainTreeItemByObjectId(ownerId);
            if (!oOwnerItem.isPresent()) {
                oOwnerItem = getMainTreeItemByObjectId(ownerId);
            }
            oOwnerItem.ifPresent(ownerItem -> {
                TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false)); // This is the main item
                result.add(item);
                ownerItem.getChildren().add(item);
            });
        } else if (obj instanceof Dimensioning) {
            TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
            result.add(item);
            mDimensioningsItem.getChildren().add(item);
        } else if (obj instanceof Floor) {
            TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
            result.add(item);
            mFloorsItem.getChildren().add(item);
        } else if (obj instanceof Wall) {
            TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
            result.add(item);
            mWallsItem.getChildren().add(item);
        } else if (obj instanceof WallHole hole) {
            Wall wall = hole.getWall();
            // Object addition changes arrive in an undefined order - thus we could potentially get additions of wall holes
            // before we are called for their owner wall. So we try to create the owner wall, if it is not present yet.
            getOrCreateTreeItems(wall);
            getMainTreeItemByObjectId(wall.getId()).ifPresent(wallItem -> {
                TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
                result.add(item);
                wallItem.getChildren().add(item);
            });
        } else if (obj instanceof Ceiling) {
            TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
            result.add(item);
            mCeilingsItem.getChildren().add(item);
        } else if (obj instanceof Covering) {
            TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
            result.add(item);
            mCoveringsItem.getChildren().add(item);
        } else if (obj instanceof SupportObject) {
            TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
            result.add(item);
            mSupportObjectsItem.getChildren().add(item);
        } else if (obj instanceof GuideLine) {
            TreeItem<ITreeItemData> item = new TreeItem<>(new BaseObjectTreeItemData(obj, false));
            result.add(item);
            mGuideLinesItem.getChildren().add(item);
        }
        if (!result.isEmpty()) {
            for (TreeItem<ITreeItemData> item : result) {
                mIdsToItems.put(objectId, item);
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * Checks the tree items of the owner group assignments of the given object.
     * Only the parents of the given child object are checked if their children are still up-to-date, so we assume to be called for each child object
     * whose owner groups are changed.
     */
    protected void checkGroups(BaseObject obj) {
        Set<String> newOwnerGroupIds = obj.getGroups()
                .stream()
                .map(BaseObject::getId)
                .collect(Collectors.toSet());
        String objId = obj.getId();
        // Check top-level group assignment
        if (obj instanceof ObjectsGroup) {
            if (newOwnerGroupIds.isEmpty()) {
                // We need a Top level group item
                if (!mCurrentTopLevelGroups.containsKey(objId)) {
                    TreeItem<ITreeItemData> item = new TreeItem<>(new GroupsTreeItemData(obj));
                    mObjectGroupsItem.getChildren().add(item);
                    mIdsToItems.put(objId, item);
                    mCurrentTopLevelGroups.put(objId, item);
                }
            } else {
                TreeItem<ITreeItemData> tlgItem = mCurrentTopLevelGroups.get(objId);
                if (tlgItem != null) {
                    tlgItem.getParent().getChildren().remove(tlgItem);
                    mIdsToItems.removeMapping(objId, tlgItem);
                    mCurrentTopLevelGroups.remove(objId);
                }
            }
        }

        // Check normal item/group positioning in parent

        // Remove item from former assigned groups which are not valid any more
        Collection<String> currentGroupIdsOfObj = mCurrentItemsToGroups.get(objId);
        for (String currentGroupId : safeCollection(currentGroupIdsOfObj, true)) {
            // For all groups where we are currently filed:

            if (newOwnerGroupIds.contains(currentGroupId)) {
                // Still valid
                continue;
            }

            // Group not valid any more; remove our item under that group
            ((Collection<TreeItem<ITreeItemData>>) new ArrayList<>(mIdsToItems.get(objId)))
                    .stream()
                    .filter(ti -> ti.getValue() instanceof GroupsTreeItemData) // Only consider items in groups tree, don't touch items at other places
                    .forEach(itemToRemoveFromGroup -> {
                        itemToRemoveFromGroup.getParent().getChildren().remove(itemToRemoveFromGroup);
                        mIdsToItems.removeMapping(objId, itemToRemoveFromGroup);
                    });
            mCurrentItemsToGroups.removeMapping(objId, currentGroupId);
        }

        // Add item to new groups which are not assigned yet
        currentGroupIdsOfObj = safeCollection(mCurrentItemsToGroups.get(objId), true); // Is this line necessary? Does our MultiValuedMap reflect changes in that collection?
        for (String newGroupId : newOwnerGroupIds) {
            if (currentGroupIdsOfObj.contains(newGroupId)) {
                // Already present
                continue;
            }

            // New group not present yet
            addItemsInObjectsGroupTreeRecursive(obj, newGroupId);
        }
    }

    /**
     * Adds the tree items for the given object under its group tree items.
     * This method will create an object's item under each tree item of the given group, if an item for the object does not exist
     * under that group item yet.
     * For each created object item, also the complete sub structure will be created recursively, if the item itself is a group.
     * @param obj The model object for which the tree items should be created.
     * @param newGroupId Id of the object's group under whose items the object items should be created.
     */
    protected void addItemsInObjectsGroupTreeRecursive(BaseObject obj, String newGroupId) {
        mCurrentItemsToGroups.put(obj.getId(), newGroupId);
        for (TreeItem<ITreeItemData> groupItem : safeCollection(getTreeItemsByObjectId(newGroupId), true)) {
            if (itemsContainObjItem(groupItem, obj)) {
                continue;
            }
            TreeItem<ITreeItemData> item = new TreeItem<>(new GroupsTreeItemData(obj));
            groupItem.getChildren().add(item);
            mIdsToItems.put(obj.getId(), item);
            if (obj instanceof ObjectsGroup subGroup) {
                String subGroupId = subGroup.getId();
                for (BaseObject childObject : subGroup.getGroupedObjects()) {
                    addItemsInObjectsGroupTreeRecursive(childObject, subGroupId);
                }
            }
        }
    }

    protected boolean itemsContainObjItem(TreeItem<ITreeItemData> groupItem, BaseObject obj) {
        ObservableList<TreeItem<ITreeItemData>> children = groupItem.getChildren();
        // We could establish an index structure in GroupsTreeItemData to avoid this loop, if necessary...
        for (TreeItem<ITreeItemData> item : children) {
            if (item instanceof IObjectTreeItemData otid) {
                if (obj.equals(otid.getObject())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected <T> Collection<T> safeCollection(Collection<T> col, boolean forceNewCollectionInstance) {
        Collection<T> result = col == null ? Collections.emptyList() : col;
        return forceNewCollectionInstance ? new ArrayList<>(result) : result;
    }

    protected void checkGroups(Collection<? extends BaseObject> checkObjects) {
        Set<BaseObject> checkedObjects = new TreeSet<>();
        for (BaseObject obj : checkObjects) {
            // Ensure that the tree items of the owner groups of the current object are up-to-date
            for (ObjectsGroup newOwnerGroup : obj.getGroups()) {
                if (checkedObjects.contains(newOwnerGroup)) { // Need to check each owner group only once
                    continue;
                }
                checkGroups(newOwnerGroup);
                checkedObjects.add(newOwnerGroup);
            }

            checkGroups(obj);
            checkedObjects.add(obj);
        }
    }

    protected void removeTreeItem(BaseObject obj) {
        String id = obj.getId();
        Collection<TreeItem<ITreeItemData>> items = getTreeItemsByObjectId(id);
        for (TreeItem<ITreeItemData> item : items) {
            TreeItem<ITreeItemData> parent = item.getParent();
            if (parent != null) {
                parent.getChildren().remove(item);
                fireChangeEvents(Arrays.asList(parent));
            }
        }
        mIdsToItems.remove(id);
    }

    public void objectsRemoved(Collection<BaseObject> removedObjects) {
        // This also handles toplevel groups, no special treatment necessary
        for (BaseObject obj : removedObjects) {
            removeTreeItem(obj);
        }
    }

    public void objectsChanged(Collection<BaseObject> changedObjects) {
        for (BaseObject obj : changedObjects) {
            Collection<TreeItem<ITreeItemData>> items = getTreeItemsByObjectId(obj.getId());
            if (items.isEmpty()) {
                continue;
            }
            fireChangeEvents(itemsAndParents(getOrCreateTreeItems(obj)));
        }
        checkGroups(changedObjects);
    }

    protected Collection<TreeItem<ITreeItemData>> parents(Collection<TreeItem<ITreeItemData>> treeItems) {
        Collection<TreeItem<ITreeItemData>> result = new ArrayList<>();
        for (TreeItem<ITreeItemData> treeItem : treeItems) {
            TreeItem<ITreeItemData> parent = treeItem.getParent();
            if (parent != null) {
                result.add(parent);
            }
        }
        return result;
    }

    protected Collection<TreeItem<ITreeItemData>> itemsAndParents(Collection<TreeItem<ITreeItemData>> treeItems) {
        Collection<TreeItem<ITreeItemData>> result = parents(treeItems);
        result.addAll(treeItems);
        return result;
    }

    protected void fireChangeEvents(Collection<TreeItem<ITreeItemData>> treeItems) {
        for (TreeItem<ITreeItemData> item : treeItems) {
            // Provoke an update of maybe changed values
            TreeModificationEvent<ITreeItemData> event = new TreeModificationEvent<>(TreeItem.valueChangedEvent(), item);
            Event.fireEvent(item, event);
        }
    }

    public void objectsAdded(Collection<BaseObject> addedObjects) {
        for (BaseObject obj : addedObjects) {
            fireChangeEvents(parents(getOrCreateTreeItems(obj)));
        }
        checkGroups(addedObjects);
    }
}