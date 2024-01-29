package com.convallyria.forcepack.sponge.schedule;

import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.sponge.ForcePackSponge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;

public class SpongeScheduler extends PlatformScheduler<ForcePackSponge> {

    public SpongeScheduler(ForcePackSponge api) {
        super(api);
    }

    @Override
    public void executeOnMain(Runnable runnable) {
        Sponge.server().scheduler().submit(Task.builder().plugin(api.pluginContainer()).execute(runnable).build());
    }

    @Override
    public void executeAsync(Runnable runnable) {
        Sponge.asyncScheduler().submit(Task.builder().plugin(api.pluginContainer()).execute(runnable).build());
    }

    @Override
    public ForcePackTask executeRepeating(Runnable runnable, long delay, long period) {
        final ScheduledTask task = Sponge.server().scheduler().submit(Task.builder().plugin(api.pluginContainer())
                .delay(Ticks.of(delay))
                .interval(Ticks.of(period))
                .execute(runnable)
                .build());
        return task::cancel;
    }

    @Override
    public void executeDelayed(Runnable runnable, long delay) {
        Sponge.server().scheduler().submit(Task.builder().plugin(api.pluginContainer())
                .delay(Ticks.of(delay))
                .execute(runnable)
                .build());
    }

    @Override
    public void registerInitTask(Runnable runnable) {
        executeOnMain(runnable);
    }
}
