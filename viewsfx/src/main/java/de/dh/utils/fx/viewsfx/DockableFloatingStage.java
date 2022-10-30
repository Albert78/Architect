package de.dh.utils.fx.viewsfx;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.Window;

public class DockableFloatingStage extends Stage implements IDockableUIRepresentation {
    protected static int MIN_WIDTH = 200;
    protected static int MIN_HEIGHT = 200;

    protected final Dockable<?> mDockable;
    protected final Tab mTab;
    protected final Label mTitleLabel;
    protected final ChangeListener<? super String> mTitleListener;

    public DockableFloatingStage(Dockable<?> dockable, Window ownerStage) {
        initOwner(ownerStage);
        mDockable = dockable;
        mTab = new Tab();
        mTitleLabel = new Label(dockable.getDockableTitle());
        mTab.setGraphic(mTitleLabel); // We use our own Label to enable adding our drag & drop event handlers
        DockSystem.installDragSourceEventHandlers(dockable, mTitleLabel);
        mTab.setContent(mDockable.getView());
        mTitleListener = (observable, oldValue, newValue) -> {
            mTitleLabel.setText(newValue);
        };
        mDockable.titleProperty().addListener(mTitleListener);
        // TODO: Graphic, ToolTip

        mTab.setClosable(false);
        focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                Node focusControl = mDockable.getFocusControl();
                if (focusControl != null) {
                    focusControl.requestFocus();
                }
            }
        });
        setOnCloseRequest(event -> {
            dockable.close();
            event.consume();
        });

        TabPane tabPane = new TabPane(mTab) {
            // Hack to let our dockable get the focus at once instead of focusing the tab pane
            @Override
            public void requestFocus() {
                Node focusControl = mDockable.getFocusControl();
                if (focusControl != null) {
                    focusControl.requestFocus();
                } else {
                    super.requestFocus();
                }
            }
        };

        setOnShown(event -> {
            Platform.runLater(() -> {
                tabPane.requestFocus();
            });
        });

        Scene scene = new Scene(tabPane, MIN_WIDTH, MIN_HEIGHT);
        setScene(scene);

        // After scene was set:
        DockSystem.notifyNewStage(this);
    }

    public Dockable<?> getDockable() {
        return mDockable;
    }

    public void show(double x, double y, double width, double height) {
        width = Math.max(width, MIN_WIDTH);
        height = Math.max(height, MIN_HEIGHT);
        setWidth(width);
        setHeight(height);
        setX(x);
        setY(y);
        show();
    }

    @Override
    public Node dispose() {
        mDockable.titleProperty().removeListener(mTitleListener);
        mTab.setContent(null);
        close();
        DockSystem.notifyStageClosed(this);
        return mDockable.getView();
    }
}
