package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Test-only mod that registers the shared game tests on NeoForge. Their test instances come with its data. */
@Mod(NeoForgeGameTests.MOD_ID)
public class NeoForgeGameTests {
    public static final String MOD_ID = "waystonespetsaddon_gametest";

    public NeoForgeGameTests(IEventBus modEventBus) {
        DeferredRegister<Consumer<GameTestHelper>> tests = DeferredRegister.create(Registries.TEST_FUNCTION, Constants.MOD_ID);
        PetTeleportGameTests.TESTS.forEach((name, test) -> tests.register(name, () -> test));
        tests.register(modEventBus);

        // NeoForge only sends mod packets over connections that negotiated them, which vanilla's mock player never
        // does, so logging one in failed as soon as Balm synced Waystones' config to it.
        PetTeleportGameTests.mockPlayerFactory = NeoForgeGameTests::makeMockPlayer;

        if (FMLEnvironment.getDist().isClient()) {
            PetScreensClientCheck.startIfRequested();
        }
    }

    /** Vanilla's makeMockServerPlayerInLevel, with a connection NeoForge treats as negotiated. */
    private static ServerPlayer makeMockPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
}
