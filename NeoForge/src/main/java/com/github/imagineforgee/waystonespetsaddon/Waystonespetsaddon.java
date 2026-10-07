package com.github.imagineforgee.waystonespetsaddon;

import com.github.imagineforgee.waystonespetsaddon.common.Common;
import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.api.PlatformAbstractions;
import net.blay09.mods.balm.api.Balm;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class Waystonespetsaddon {

    public Waystonespetsaddon() {
        Balm.initialize(Constants.MOD_ID, Common::initialize);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener((ModConfigEvent e) -> {
            if (e.getConfig().getSpec() == NeoForgeConfig.SPEC) {
                NeoForgeConfig.CONFIG.applyToCommon();
            }
        });
        PlatformAbstractions.delayedTaskFactory = DelayedTaskImpl::new;
    }
}
