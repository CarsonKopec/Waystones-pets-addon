package com.github.imagineforgee.waystonespetsaddon.common.client;

import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetsButton;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.client.screen.ScreenInitEvent;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;

public class CommonClient {
    public static void initialize() {
        // Fires on every init, including when returning from the pet screen or resizing.
        // Minecraft clears a screen's widgets before each init, so the button is never added twice.
        Balm.getEvents().onEvent(ScreenInitEvent.Post.class, event -> {
            if (event.getScreen() instanceof WaystoneSelectionScreenBase screen) {
                PetsButton.addTo(screen);
            }
        });
    }
}
