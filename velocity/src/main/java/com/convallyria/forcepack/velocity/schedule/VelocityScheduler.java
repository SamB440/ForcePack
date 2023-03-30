package com.convallyria.forcepack.velocity.schedule;

import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class VelocityScheduler extends PlatformScheduler<ForcePackVelocity> {

    public VelocityScheduler(ForcePackVelocity api) {
        super(api);
    }

    @Override
    public void executeOnMain(Runnable runnable) {
        api.getServer().getScheduler().buildTask(api, runnable).schedule();
    }

    @Override
    public ForcePackTask executeRepeating(Runnable runnable, long delay, long period) {
        final ScheduledTask task = api.getServer().getScheduler().buildTask(api, runnable)
                .delay(delay * 50, TimeUnit.MILLISECONDS)
                .repeat(period * 50, TimeUnit.MILLISECONDS)
                .schedule();
        return task::cancel;
    }

    @Override
    public void executeDelayed(Runnable runnable, long delay) {
        api.getServer().getScheduler().buildTask(api, runnable)
                .delay(delay * 50, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Override
    public void registerInitTask(Runnable runnable) {
        api.getServer().getScheduler().buildTask(api, runnable).schedule();
    }
}
