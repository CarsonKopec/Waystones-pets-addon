package com.github.imagineforgee.waystonespetsaddon.events;

import com.github.imagineforgee.waystonespetsaddon.common.api.TickDelayedTaskManager;
import net.minecraftforge.event.TickEvent;

public class ForgeTickHandler {
    public static void register() {
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> TickDelayedTaskManager.tick());
    }
}
