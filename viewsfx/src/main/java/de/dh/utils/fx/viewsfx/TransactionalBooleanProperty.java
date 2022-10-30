package de.dh.utils.fx.viewsfx;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 * Boolean property with beginChange - endChange support.
 */
public class TransactionalBooleanProperty implements ReadOnlyProperty<Boolean> {
    protected final SimpleBooleanProperty mValue;
    protected boolean mUncommittedValue;
    protected int mTransactionLevel = 0;

    public TransactionalBooleanProperty(boolean value) {
        mValue = new SimpleBooleanProperty(value);
    }

    /**
     * Sets this propertie's value. If we are in a transaction, i.e. {@link #beginChange()} had been called before,
     * the given value will be stored to an interim value field and won't be set until {@link #endChange()} is called.
     */
    public void set(boolean value) {
        if (mTransactionLevel > 0) {
            mUncommittedValue = value;
            return;
        }
        mValue.set(value);
    }

    /**
     * Starts a transaction or increases the transaction level.
     * If this method is called more than once until {@link #endChange()} is called, the transaction level is increased,
     * so the same number of {@link #endChange()} calls is necessary to commit the transaction.
     */
    public void beginChange() {
        mUncommittedValue = mValue.get();
        mTransactionLevel++;
    }

    /**
     * Decreases the transaction level. This will commit the transaction, when the number of calls to this method
     * is the same as the number of calls to {@link #beginChange()} before.
     *
     * @throws IllegalStateException If this method is called more often than {@link #beginChange()} was called before.
     */
    public void endChange() {
        if (mTransactionLevel == 0) {
            throw new IllegalStateException("endChange() must not be called more often than beginChange() was called");
        }
        mTransactionLevel--;
        if (mTransactionLevel == 0) {
            mValue.set(mUncommittedValue);
        }
    }

    @Override
    public Object getBean() {
        return mValue.getBean();
    }

    @Override
    public String getName() {
        return mValue.getName();
    }

    @Override
    public Boolean getValue() {
        return mValue.getValue();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        mValue.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        mValue.removeListener(listener);
    }

    @Override
    public void addListener(ChangeListener<? super Boolean> listener) {
        mValue.addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Boolean> listener) {
        mValue.removeListener(listener);
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return mValue.equals(obj);
    }

    @Override
    public String toString() {
        return mValue.toString();
    }
}
