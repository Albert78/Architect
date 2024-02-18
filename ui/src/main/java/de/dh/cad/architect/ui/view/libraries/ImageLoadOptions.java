package de.dh.cad.architect.ui.view.libraries;

import java.util.Objects;

public class ImageLoadOptions {
    protected final int mWidth;
    protected final int mHeight;
    protected final boolean mPreserveRatio;
    protected final boolean mSmooth;

    public ImageLoadOptions(double width, double height) {
        this((int) width, (int) height, false, false);
    }

    public ImageLoadOptions(int width, int height) {
        this(width, height, false, false);
    }

    public ImageLoadOptions(int width, int height, boolean preserveRatio, boolean smooth) {
        mWidth = width;
        mHeight = height;
        mPreserveRatio = preserveRatio;
        mSmooth = smooth;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean isPreserveRatio() {
        return mPreserveRatio;
    }

    public boolean isSmooth() {
        return mSmooth;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hash(mHeight, mPreserveRatio, mSmooth, mWidth);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ImageLoadOptions other = (ImageLoadOptions) obj;
        return mHeight == other.mHeight && mPreserveRatio == other.mPreserveRatio && mSmooth == other.mSmooth && mWidth == other.mWidth;
    }

    @Override
    public String toString() {
        return "ImageLoadOptions [Width=" + mWidth + ", Height=" + mHeight + ", PreserveRatio=" + mPreserveRatio + ", Smooth=" + mSmooth + "]";
    }
}
