package com.dannysu.sqlitefutures;

public abstract class AsyncDone<T> {
    public abstract void onResponse(T value);
    public abstract void onCancel();
    public void onException(Exception e) {
    }
}