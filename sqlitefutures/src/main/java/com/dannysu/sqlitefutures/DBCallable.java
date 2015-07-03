package com.dannysu.sqlitefutures;

import android.database.sqlite.SQLiteDatabase;

public abstract class DBCallable<T> {
    public abstract T call(SQLiteDatabase database) throws Exception;
}