package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.Collection;

/**
 * Test-only mod that registers the shared game tests on Forge. Forge finds tests through {@link GameTestHolder},
 * which it reads from the class declaring the generator, so the generator is declared again here.
 */
@Mod(ForgeGameTests.MOD_ID)
@GameTestHolder(Constants.MOD_ID)
public class ForgeGameTests extends PetTeleportGameTests {
    public static final String MOD_ID = "waystonespetsaddon_gametest";

    @Override
    @GameTestGenerator
    public Collection<TestFunction> tests() {
        return super.tests();
    }
}
