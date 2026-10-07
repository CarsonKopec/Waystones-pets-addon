package com.github.imagineforgee.waystonespetsaddon;

import com.github.imagineforgee.waystonespetsaddon.common.Common;
import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.api.PlatformAbstractions;
import com.github.imagineforgee.waystonespetsaddon.common.client.CommonClient;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class Waystonespetsaddon {

    public Waystonespetsaddon() {
        Balm.initialize(Constants.MOD_ID, Common::initialize);
        if (FMLEnvironment.dist.isClient()) {
            BalmClient.initialize(Constants.MOD_ID, CommonClient::initialize);
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener((ModConfigEvent e) -> {
            if (e.getConfig().getSpec() == NeoForgeConfig.SPEC) {
                NeoForgeConfig.CONFIG.applyToCommon();
            }
        });
        PlatformAbstractions.delayedTaskFactory = DelayedTaskImpl::new;
    }
}
