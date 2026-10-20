package com.github.imagineforgee.waystonespetsaddon;


import com.github.imagineforgee.waystonespetsaddon.common.Common;
import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.api.PlatformAbstractions;
import com.github.imagineforgee.waystonespetsaddon.common.client.CommonClient;
import com.github.imagineforgee.waystonespetsaddon.events.ForgeTickHandler;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.forge.platform.runtime.ForgeLoadContext;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class Waystonespetsaddon {
    
    public Waystonespetsaddon(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();
        ForgeLoadContext loadContext = new ForgeLoadContext(modBusGroup);
        Balm.initializeMod(Constants.MOD_ID, loadContext, registrars -> Common.initialize());
        if (FMLEnvironment.dist.isClient()) {
            BalmClient.initializeMod(Constants.MOD_ID, loadContext, registrars -> CommonClient.initialize());
        }
        context.registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC);
        ModConfigEvent.Loading.getBus(modBusGroup).addListener(e -> applyConfig(e.getConfig()));
        ModConfigEvent.Reloading.getBus(modBusGroup).addListener(e -> applyConfig(e.getConfig()));
        PlatformAbstractions.delayedTaskFactory = DelayedTaskImpl::new;
        ForgeTickHandler.register();
    }

    private static void applyConfig(ModConfig config) {
        if (config.getSpec() == ForgeConfig.SPEC) {
            ForgeConfig.CONFIG.applyToCommon();
        }
    }
}
