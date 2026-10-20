package com.github.imagineforgee.waystonespetsaddon;

import com.github.imagineforgee.waystonespetsaddon.common.Common;
import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.api.PlatformAbstractions;
import com.github.imagineforgee.waystonespetsaddon.common.client.CommonClient;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class Waystonespetsaddon {

    public Waystonespetsaddon(IEventBus modEventBus, ModContainer modContainer) {
        NeoForgeLoadContext loadContext = new NeoForgeLoadContext(modEventBus);
        Balm.initializeMod(Constants.MOD_ID, loadContext, registrars -> Common.initialize());
        if (FMLEnvironment.getDist().isClient()) {
            BalmClient.initializeMod(Constants.MOD_ID, loadContext, registrars -> CommonClient.initialize());
        }
        modContainer.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        modEventBus.addListener((ModConfigEvent e) -> {
            if (e.getConfig().getSpec() == NeoForgeConfig.SPEC) {
                NeoForgeConfig.CONFIG.applyToCommon();
            }
        });
        PlatformAbstractions.delayedTaskFactory = DelayedTaskImpl::new;
    }
}
