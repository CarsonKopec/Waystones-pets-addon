package com.github.imagineforgee.waystonespetsaddon.common;

import com.github.imagineforgee.waystonespetsaddon.common.handlers.ModEventHandlers;
import com.github.imagineforgee.waystonespetsaddon.common.network.ModNetworking;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.BalmEvents;

public class Common {
	public static void initialize() {
        BalmEvents events = Balm.getEvents();
        ModEventHandlers.initialize(events);
        ModNetworking.initialize();
    }

}
