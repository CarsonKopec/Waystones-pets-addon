package com.github.imagineforgee.waystonespetsaddon;

import com.github.imagineforgee.waystonespetsaddon.common.api.IDelayedTask;

public class DelayedTaskImpl implements IDelayedTask {
    private int ticksRemaining;
    private final Runnable task;

    public DelayedTaskImpl(int delayTicks, Runnable task) {
        this.ticksRemaining = delayTicks;
        this.task = task;
    }

    @Override
    public boolean tick() {
        return --ticksRemaining <= 0;
    }

    @Override
    public void run() {
        task.run();
    }
}
