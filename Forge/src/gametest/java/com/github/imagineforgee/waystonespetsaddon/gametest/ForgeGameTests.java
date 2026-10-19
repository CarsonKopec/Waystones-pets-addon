package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Test-only mod that registers the shared game tests on Forge. Their test instances come with its data. */
@Mod(ForgeGameTests.MOD_ID)
public class ForgeGameTests {
    public static final String MOD_ID = "waystonespetsaddon_gametest";

    public ForgeGameTests(FMLJavaModLoadingContext context) {
        DeferredRegister<Consumer<GameTestHelper>> tests = DeferredRegister.create(Registries.TEST_FUNCTION, Constants.MOD_ID);
        PetTeleportGameTests.TESTS.forEach((name, test) -> tests.register(name, () -> test));
        tests.register(context.getModEventBus());

        MinecraftForge.EVENT_BUS.addListener(ForgeGameTests::startLikeARealServer);
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
