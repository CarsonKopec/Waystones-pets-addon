package com.github.imagineforgee.waystonespetsaddon.client;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.client.CommonClient;
import net.blay09.mods.balm.client.BalmClient;
import net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext;
import net.fabricmc.api.ClientModInitializer;

public class WaystonespetsaddonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BalmClient.initializeMod(Constants.MOD_ID, FabricLoadContext.INSTANCE, registrars -> CommonClient.initialize());
    }
}
