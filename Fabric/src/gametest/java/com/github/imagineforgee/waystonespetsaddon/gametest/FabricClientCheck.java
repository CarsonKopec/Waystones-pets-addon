package com.github.imagineforgee.waystonespetsaddon.gametest;

import net.fabricmc.api.ClientModInitializer;

/** Starts the shared client check of the pet screens when the client runs with -Dwaystonespetsaddon.clientcheck=true. */
public class FabricClientCheck implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PetScreensClientCheck.startIfRequested();
    }
}
