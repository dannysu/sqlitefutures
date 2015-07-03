package com.dannysu.sqlitefutures;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.Callable;

// Final class to ensure nobody consuming this library can get around the Singleton.
// SQLiteOpenHelper really should be used as a singleton always, so using this to enforce that.
public final class SQLiteFutures {

    private static SQLiteFutures instance;

    private SQLiteOpenHelper helper;
    private final SQLiteDatabase writableDatabase;

    public static synchronized void initialize(SQLiteOpenHelper helper) {
        if (instance == null) {
            instance = new SQLiteFutures(helper);
        }
        else {
            throw new IllegalStateException("Already initialized");
        }
    }

    public static synchronized SQLiteFutures getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Not initialized yet");
        }

        return instance;
    }

    private SQLiteFutures(SQLiteOpenHelper helper) {
        this.helper = helper;
        writableDatabase = helper.getWritableDatabase();
    }

    public <T> FutureDBTask<T> execute(final DBCallable<T> callable) {
        FutureDBTask<T> futureTask = new FutureDBTask<T>(
                new Callable<T>() {

                    @Override
                    public T call() throws Exception {
                        return callable.call(writableDatabase);
                    }
                });

        // Run on separate background thread
        new Thread(futureTask).start();

        return futureTask;
    }
}