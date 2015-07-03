package com.dannysu.sqlitefutures;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class FutureDBTask<T> extends FutureTask<T> {

    public FutureDBTask(Callable<T> callable) {
        super(callable);
    }

    @Override
    public T get() throws ExecutionException, InterruptedException {
        return super.get();
    }

    public void onDoneAsync(AsyncDone<T> response) {
        final HandlerThread handlerThread = new HandlerThread("FutureDBTask", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        // Should be UI thread if called from UI thread
        final Handler currentThreadHandler = new Handler();

        // Create a weak ref for use with the non-UI thread handler below
        final WeakReference<AsyncDone<T>> weakRef = new WeakReference<>(response);

        final FutureDBTask<T> self = this;
        handler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    final T value = self.get();

                    currentThreadHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            AsyncDone<T> response = weakRef.get();
                            if (response == null) {
                                return;
                            }

                            response.onResponse(value);
                        }
                    });
                }
                catch (CancellationException e) {
                    currentThreadHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            AsyncDone<T> response = weakRef.get();
                            if (response == null) {
                                return;
                            }

                            response.onCancel();
                        }
                    });
                }
                catch (final Exception e) {
                    currentThreadHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            AsyncDone<T> response = weakRef.get();
                            if (response == null) {
                                return;
                            }

                            response.onException(e);
                        }
                    });
                }
                finally {
                    handlerThread.quit();
                }
            }
        });
    }
}