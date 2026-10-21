package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Starts the shared client check of the pet screens when the client runs with -Dwaystonespetsaddon.clientcheck=true.
 * The test runs add the test classes to the mod itself, so this subscribes to its setup rather than being a mod.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NeoForgeClientCheck {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PetScreensClientCheck.startIfRequested();
    }
}
