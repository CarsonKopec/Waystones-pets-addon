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
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.server.ServerLifecycleHooks;

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
        MinecraftForge.EVENT_BUS.addListener(ForgeGameTests::startLikeARealServer);
    }

    @Override
    @GameTestGenerator
    public Collection<TestFunction> tests() {
        return super.tests();
    }

    /**
     * Forge's game test server skips the hooks a real server runs while starting, so this does their part before the
     * first tick. Without server configs, reading Forge's own (which it does whenever an entity errors) crashed the
     * test server; without the permission API, every Waystones teleport failed its permission check. Forge unloads the
     * configs as usual when the server stops.
     */
    private static void startLikeARealServer(ServerStartedEvent event) {
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
        // Sets up the permission API, which only accepts the call from Forge's own hook.
        ServerLifecycleHooks.handleServerStarting(server);
    }
}
