package com.sjm.core.util.misc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Promise<T> {
    private boolean isDone;
    private T result;
    private Throwable error;

    public synchronized T get() throws InterruptedException, ExecutionException {
        while (!isDone) {
            wait(1000);
        }
        if (error != null)
            throw new ExecutionException(error);
        return result;
    }

    public synchronized T get(long timeout)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (timeout < 0)
            throw new IllegalArgumentException();
        long endTime = System.currentTimeMillis() + timeout;
        while (!isDone) {
            if (System.currentTimeMillis() > endTime)
                throw new TimeoutException();
            wait(1000);
        }
        if (error != null)
            throw new ExecutionException(error);
        return result;
    }

    public boolean isDone() {
        return isDone;
    }

    public synchronized void set(T result, Throwable error) {
        this.isDone = true;
        this.result = result;
        this.error = error;
        notifyAll();
    }
}
