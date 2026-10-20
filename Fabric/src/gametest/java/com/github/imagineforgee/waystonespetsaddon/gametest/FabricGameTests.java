package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Test-only mod that registers the shared game tests on Fabric. Their test instances come with its data. */
public class FabricGameTests implements ModInitializer {
    @Override
    public void onInitialize() {
        PetTeleportGameTests.TESTS.forEach((name, test) -> Registry.register(BuiltInRegistries.TEST_FUNCTION,
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), test));
    }
}
