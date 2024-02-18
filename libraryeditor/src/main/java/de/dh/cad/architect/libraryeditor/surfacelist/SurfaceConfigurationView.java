package de.dh.cad.architect.libraryeditor.surfacelist;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dh.cad.architect.libraryeditor.Constants;
import de.dh.cad.architect.libraryeditor.Strings;
import de.dh.cad.architect.libraryeditor.SurfaceConfigurationData;
import de.dh.cad.architect.model.assets.AssetRefPath;
import de.dh.cad.architect.ui.assets.AssetLoader;
import de.dh.cad.architect.ui.view.libraries.ImageLoadOptions;
import de.dh.utils.Vector2D;
import de.dh.utils.io.obj.RawMaterialData;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Represents the view of a single entry in the surface list.
 */
public class SurfaceConfigurationView extends BorderPane {
    private static final Logger log = LoggerFactory.getLogger(SurfaceConfigurationView.class);

    protected static final int EXHIBIT_SIZE = 50;

    protected final String mSurfaceTypeId;
    protected final Box mExhibit;
    protected final Text mText = new Text();
    protected final Text mMaterialText = new Text();

    protected final Font mDefaultFont;
    protected final Font mItalicFont;
    protected final Font mBoldFont;
    protected final Paint mDefaultTextFill;

    protected final ChangeListener<? super Boolean> mFocusListener = (prop, oldVal, newVal) -> {
        setSurfaceFocus(newVal);
    };
    protected final ChangeListener<? super AssetRefPath> mMaterialRefListener = (prop, oldVal, newVal) -> {
        setMaterialRef(newVal);
    };
    protected final ChangeListener<? super Boolean> mInUseListener = (prop, oldVal, newVal) -> {
        updateInUse(newVal);
    };

    protected final AssetLoader mAssetLoader;
    protected SurfaceConfigurationData mBoundItem = null;
    protected AssetRefPath mMaterialRefPath = null;

    protected SurfaceConfigurationView(String surfaceTypeId, AssetLoader assetLoader) {
        mAssetLoader = assetLoader;
        mSurfaceTypeId = surfaceTypeId;
        mExhibit = new Box(EXHIBIT_SIZE, EXHIBIT_SIZE, 1);
        mExhibit.setTranslateX(EXHIBIT_SIZE / 2);
        mExhibit.setTranslateY(EXHIBIT_SIZE / 2);
        SubScene subScene = new SubScene(new Group(mExhibit), EXHIBIT_SIZE, EXHIBIT_SIZE); // Without the subscene, the 3D shape will extend over the borders of the listview
        setLeft(subScene);

        mDefaultFont = mText.getFont();
        double defaultFontSize = mDefaultFont.getSize();
        mItalicFont = Font.font(null, FontPosture.ITALIC, mDefaultFont.getSize());
        mBoldFont = Font.font(null, FontWeight.BOLD, defaultFontSize);
        mDefaultTextFill = mText.getFill();

        mText.setFont(mBoldFont);
        mText.setText(mSurfaceTypeId);
        setMaterialRef(null);
        VBox center = new VBox(mText, mMaterialText);
        BorderPane.setMargin(center, new Insets(5));
        setCenter(center);
        setOnMouseEntered(event -> {
            if (mBoundItem != null) {
                mBoundItem.setFocused(true);
            }
        });
        setOnMouseExited(event -> {
            if (mBoundItem != null) {
                mBoundItem.setFocused(false);
            }
        });
    }

    public String getSurfaceTypeId() {
        return mSurfaceTypeId;
    }

    public void attach(SurfaceConfigurationData surfaceConfigData) {
        if (mBoundItem != null) {
            detach();
        }
        mBoundItem = surfaceConfigData;

        setMaterialRef(mBoundItem.getMaterialRef());
        setSurfaceFocus(mBoundItem.isFocused());
        updateInUse(mBoundItem.isInUse());

        mBoundItem.getFocusedProperty().addListener(mFocusListener);
        mBoundItem.getMaterialRefProperty().addListener(mMaterialRefListener);
        mBoundItem.getInUseProperty().addListener(mInUseListener);
    }

    public void detach() {
        mBoundItem.getFocusedProperty().removeListener(mFocusListener);
        mBoundItem.getMaterialRefProperty().removeListener(mMaterialRefListener);
        mBoundItem.getInUseProperty().removeListener(mInUseListener);
    }

    protected void setSurfaceFocus(boolean value) {
        if (value) {
            setBackground(new Background(new BackgroundFill(Constants.FOCUSED_COLOR, null, null)));
        } else {
            setBackground(null);
        }
    }

    protected void setMaterialRef(AssetRefPath materialRefPath) {
        try {
            mMaterialRefPath = materialRefPath;
            if (materialRefPath == null) {
                mMaterialText.setText(Strings.NO_MATERIAL_TEXT);
                mMaterialText.setFont(mItalicFont);

                PhongMaterial mat = new PhongMaterial(Color.WHITE);
                Image materialPlaceholderTextureImage = AssetLoader.loadMaterialPlaceholderTextureImage(Optional.of(new ImageLoadOptions(mExhibit.getWidth(), mExhibit.getHeight())));
                mat.setDiffuseMap(materialPlaceholderTextureImage);
                mExhibit.setMaterial(mat);
                return;
            }
            RawMaterialData materialData = mAssetLoader.loadMaterialData(materialRefPath);
            mMaterialText.setText(MessageFormat.format(Strings.MATERIAL_TEXT, materialData.getName()));
            mMaterialText.setFont(mDefaultFont);
            mAssetLoader.configureMaterial_Lax(mExhibit, materialData, Optional.of(new Vector2D(mExhibit.getWidth(), mExhibit.getHeight())));
        } catch (IOException e) {
            log.warn("Error loading material", e);
        }
    }

    protected void updateInUse(boolean inUse) {
        double opacity = 1;
        if (mMaterialRefPath == null) {
            opacity *= 0.8;
        }
        if (inUse) {
            mText.setFill(mDefaultTextFill);
            mMaterialText.setFill(mDefaultTextFill);
        } else {
            opacity *= 0.8;
            mText.setFill(Color.GRAY);
            mMaterialText.setFill(Color.GRAY);
        }
        PhongMaterial material = (PhongMaterial) mExhibit.getMaterial();
        Color color = material.getDiffuseColor();
        material.setDiffuseColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
    }
}