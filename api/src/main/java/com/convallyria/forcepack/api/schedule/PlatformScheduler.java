package com.convallyria.forcepack.api.schedule;

import com.convallyria.forcepack.api.ForcePackAPI;

public abstract class PlatformScheduler<T extends ForcePackAPI> {

    protected final T api;

    public PlatformScheduler(T api) {
        this.api = api;
    }

    public abstract void executeOnMain(Runnable runnable);

    public abstract ForcePackTask executeRepeating(Runnable runnable, long delay, long period);

    public abstract void executeDelayed(Runnable runnable, long delay);

    public abstract void registerInitTask(Runnable runnable);

    @FunctionalInterface
    public interface ForcePackTask {
        void cancel();
    }
}
