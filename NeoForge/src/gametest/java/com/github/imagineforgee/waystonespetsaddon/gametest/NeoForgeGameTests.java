package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;

/**
 * Registers the shared game tests on NeoForge. The game test run adds this class to the mod, and NeoForge finds tests
 * through {@link GameTestHolder}, which it reads from the class declaring the generator, so it is declared again here.
 */
@GameTestHolder(Constants.MOD_ID)
public class NeoForgeGameTests extends PetTeleportGameTests {

    @Override
    @GameTestGenerator
    public Collection<TestFunction> tests() {
        return super.tests();
    }
}
