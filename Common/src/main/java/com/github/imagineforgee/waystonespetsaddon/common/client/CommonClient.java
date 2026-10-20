package com.github.imagineforgee.waystonespetsaddon.common.client;

import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetsButton;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;

public class CommonClient {
    public static void initialize() {
        // Fires on every init, including when returning from the pet screen or resizing.
        // Minecraft clears a screen's widgets before each init, so the button is never added twice.
        ScreenCallback.Init.After.EVENT.register(screen -> {
            if (screen instanceof WaystoneSelectionScreenBase waystoneScreen) {
                PetsButton.addTo(waystoneScreen);
            }
        });
    }
}
