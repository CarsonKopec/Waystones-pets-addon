package com.github.imagineforgee.waystonespetsaddon;


import com.github.imagineforgee.waystonespetsaddon.common.Common;
import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.api.PlatformAbstractions;
import com.github.imagineforgee.waystonespetsaddon.common.client.CommonClient;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.forge.ForgeLoadContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class Waystonespetsaddon {
    
    public Waystonespetsaddon(FMLJavaModLoadingContext context) {
        ForgeLoadContext loadContext = new ForgeLoadContext(context.getModEventBus());
        Balm.initialize(Constants.MOD_ID, loadContext, Common::initialize);
        if (FMLEnvironment.dist.isClient()) {
            BalmClient.initialize(Constants.MOD_ID, loadContext, CommonClient::initialize);
        }
        context.registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC);
        context.getModEventBus().addListener((ModConfigEvent e) -> {
            if (e.getConfig().getSpec() == ForgeConfig.SPEC) {
                ForgeConfig.CONFIG.applyToCommon();
            }
        });
        PlatformAbstractions.delayedTaskFactory = DelayedTaskImpl::new;
    }

}
