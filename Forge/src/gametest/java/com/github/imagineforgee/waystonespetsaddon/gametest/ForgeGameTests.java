package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.gametest.GameTestHolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Test-only mod that registers the shared game tests on Forge. Forge finds tests through {@link GameTestHolder},
 * which it reads from the class declaring the generator, so the generator is declared again here.
 */
@Mod(ForgeGameTests.MOD_ID)
@GameTestHolder(Constants.MOD_ID)
public class ForgeGameTests extends PetTeleportGameTests {
    public static final String MOD_ID = "waystonespetsaddon_gametest";

    static {
        // Static, because the test registry constructs this class again later, outside mod loading.
        MinecraftForge.EVENT_BUS.addListener(ForgeGameTests::loadServerConfigs);

        if (FMLEnvironment.dist.isClient()) {
            PetScreensClientCheck.startIfRequested();
        }
    }

    @Override
    @GameTestGenerator
    public Collection<TestFunction> tests() {
        return super.tests();
    }

    /**
     * Forge's game test server skips the hook that loads server configs on a real server, so reading Forge's own
     * server config (which it does whenever an entity errors) crashed the test server. This loads them the same way,
     * before the first tick, and Forge unloads them as usual when the server stops.
     */
    private static void loadServerConfigs(ServerStartedEvent event) {
        if (!(event.getServer() instanceof GameTestServer server)) {
            return;
        }
        Path configDir = server.getWorldPath(new LevelResource("serverconfig"));
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, configDir);
    }
}
