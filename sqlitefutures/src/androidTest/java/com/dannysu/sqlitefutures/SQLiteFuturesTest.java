package com.dannysu.sqlitefutures;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import static com.google.common.truth.Truth.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class SQLiteFuturesTest {

    final static TestSQLiteOpenHelper testDb = new TestSQLiteOpenHelper(
            InstrumentationRegistry.getContext());

    @BeforeClass public static void onlyOnce() {
        SQLiteFutures.initialize(testDb);
    }

    @Test public void testSynchronousGet() throws ExecutionException, InterruptedException {
        SQLiteFutures asyncSqlite = SQLiteFutures.getInstance();
        FutureDBTask<String> future = asyncSqlite.execute(new DBCallable<String>() {
            @Override
            public String call(SQLiteDatabase database) throws Exception {
                Cursor cursor = null;
                try {
                    cursor = database.query(TestSQLiteOpenHelper.TABLE_EMPLOYEE,
                            new String[]{TestSQLiteOpenHelper.EmployeeTable.USERNAME},
                            TestSQLiteOpenHelper.EmployeeTable.ID + " = ?",
                            new String[]{String.valueOf(testDb.bobId)},
                            null, null, null);

                    cursor.moveToNext();
                    return cursor.getString(0);
                } catch (Exception e) {
                    throw e;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });

        // Immediately call get() and block. Basically the same as synchronous call to DB on same
        // thread.
        String name = future.get();

        assertThat(name).isEqualTo("bob");
    }

    @Test public void testAsynchronousGet() {
        SQLiteFutures asyncSqlite = SQLiteFutures.getInstance();
        final FutureDBTask<String> future = asyncSqlite.execute(new DBCallable<String>() {
            @Override
            public String call(SQLiteDatabase database) throws Exception {
                Cursor cursor = null;
                try {
                    cursor = database.query(TestSQLiteOpenHelper.TABLE_EMPLOYEE,
                            new String[]{TestSQLiteOpenHelper.EmployeeTable.USERNAME},
                            TestSQLiteOpenHelper.EmployeeTable.ID + " = ?",
                            new String[]{String.valueOf(testDb.bobId)},
                            null, null, null);

                    cursor.moveToNext();
                    return cursor.getString(0);
                } catch (Exception e) {
                    throw e;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });

        final MockAsyncDone asyncDone = new MockAsyncDone() {
            @Override
            public void onResponse(String name) {
                super.onResponse(name);
                synchronized (this) {
                    notifyAll();
                }
            }

            @Override
            public void onCancel() {
                Assert.fail("Shouldn't be cancelled");
            }

            @Override
            public void onException(Exception e) {
                Assert.fail("Shouldn't have exception " + e.getMessage());
            }
        };

        // Simulate running something on UI thread
        HandlerThread handlerThread = new HandlerThread("SimulateUIThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                future.onDoneAsync(asyncDone);
            }
        });

        try {
            synchronized (asyncDone) {
                asyncDone.wait(2000);
            }

            assertThat(asyncDone.name).isEqualTo("bob");
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    abstract class MockAsyncDone extends AsyncDone<String> {

        public String name;

        @Override
        public void onResponse(String name) {
            this.name = name;
        }
    }
}
