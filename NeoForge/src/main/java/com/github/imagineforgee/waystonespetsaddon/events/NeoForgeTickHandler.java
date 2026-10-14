package com.github.imagineforgee.waystonespetsaddon.events;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.api.TickDelayedTaskManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoForgeTickHandler {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TickDelayedTaskManager.tick();
    }
}
