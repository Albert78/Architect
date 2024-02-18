package de.dh.cad.architect.libraryeditor.surfacelist;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.dh.cad.architect.libraryeditor.SurfaceConfigurationData;
import de.dh.cad.architect.ui.assets.AssetLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

public class SurfaceConfigurationsListControl extends BorderPane {
    protected class SurfaceConfigurationCell extends ListCell<SurfaceConfigurationData> {
        protected SurfaceConfigurationView mView = null;

        public SurfaceConfigurationCell() {
            setText(null);
        }

        @Override
        public void updateItem(SurfaceConfigurationData item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                if (mView != null) {
                    mView.detach();
                }
                mView = null;
                setGraphic(null);
                return;
            }
            String surfaceTypeId = item.getSurfaceTypeId();
            mView = new SurfaceConfigurationView(surfaceTypeId, mAssetLoader);
            mView.attach(item);
            setGraphic(mView);
        }
    }

    protected final ListView<SurfaceConfigurationData> mListView = new ListView<>();
    protected final AssetLoader mAssetLoader;
    protected final ObservableList<SurfaceConfigurationData> mSurfaceList = FXCollections.observableArrayList();

    public SurfaceConfigurationsListControl(AssetLoader assetLoader) {
        mAssetLoader = assetLoader;
        initialize();
    }

    protected void initialize() {
        mListView.setCellFactory(slv -> {
            return new SurfaceConfigurationCell();
        });
        mListView.setItems(mSurfaceList);
        setCenter(mListView);
    }

    public ObservableList<SurfaceConfigurationData> getSurfaceList() {
        return mSurfaceList;
    }

    public void updateList(Map<String, SurfaceConfigurationData> surfaceConfigurations) {
        mSurfaceList.removeIf(scd -> !surfaceConfigurations.containsKey(scd.getSurfaceTypeId()));
        List<SurfaceConfigurationData> scSorted = surfaceConfigurations.values()
                .stream()
                .sorted(Comparator.comparing(SurfaceConfigurationData::getSurfaceTypeId))
                .toList();

        scSorted.forEach(sc -> {
            if (!mSurfaceList.contains(sc)) {
                mSurfaceList.add(sc);
            }
        });
    }
}
