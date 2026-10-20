package com.github.imagineforgee.waystonespetsaddon.common;

import com.github.imagineforgee.waystonespetsaddon.common.handlers.ModEventHandlers;
import com.github.imagineforgee.waystonespetsaddon.common.network.ModNetworking;

public class Common {
	public static void initialize() {
        ModEventHandlers.initialize();
        ModNetworking.initialize();
    }

}
