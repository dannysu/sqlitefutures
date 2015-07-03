SQLiteFutures
=============

This is a set of helper classes to make using SQLite on Android more consistent and promote good habit of not blocking the UI thread.



Usage
-----

Initialize `SQLiteFutures` with your implementation of `SQLiteOpenHelper`.

```java
SQLiteFutures.initialize(new MySQLiteOpenHelper());
```

Whenever you want to run a query, obtain an instance of `SQLiteFutures` and use the execute method. You'll need to provide a DBCallable class and provide the type of result that should be returned and the implementation to retrieve that result. For example, querying the database for a string would look like the following:

```java
SQLiteFutures instance = SQLiteFutures.getInstance();
instance.execute(new DBCallable<String>() {
    @Override
    public String call(SQLiteDatabase database) throws Exception {
        // Do whatever you normally do with the database to start
        // transactions and retrieve results
    }
});
```

The `SQLiteFutures.execute` method returns a `FutureDBTask<T>`. This allows you to decide whether to handle the query synchronously or asynchronously as well as when to handle it.

For example, to obtain the result synchronously you'd do something like the following:

```java
FutureDBTask<String> futureTask = instance.execute(new DBCallable<String>() {
    @Override
    public String call(SQLiteDatabase database) throws Exception {
        // Do whatever you normally do with the database to start
        // transactions and retrieve results.
        // 
        // Do whatever the heavy liftings are while you're on this background
        // thread before results gets put back on the UI thread.
    }
});

String result = futureTask.get();
```

If you'd like to not block the UI thread while you're updating the database or fetching results, then you can do what you typically would do with FutureTask. Perhaps check the `isDone` method when you're ready for the results and then call `get` to obtain it. Alternatively, you can also use the `FutureDBTask.onDoneAsync` method to do something when the result is ready. The `onDoneAsync` method takes an `AsyncDone` instance where all of its required methods are invoked on the UI thread.

```java
FutureDBTask<String> futureTask = instance.execute(new DBCallable<String>() {
    @Override
    public String call(SQLiteDatabase database) throws Exception {
        // Heavy lifting
    }
});

futureTask.onDoneAsync(new AsyncDone<String>() {
    @Override
    public void onResponse(String value) {
        // This is on the UI thread
    }
});
```
