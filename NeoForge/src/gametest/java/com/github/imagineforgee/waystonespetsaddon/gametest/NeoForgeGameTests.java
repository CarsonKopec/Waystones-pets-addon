package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.Collection;
import java.util.UUID;

/**
 * Test-only mod that registers the shared game tests on NeoForge. NeoForge finds tests through {@link GameTestHolder},
 * which it reads from the class declaring the generator, so the generator is declared again here.
 */
@Mod(NeoForgeGameTests.MOD_ID)
@GameTestHolder(Constants.MOD_ID)
public class NeoForgeGameTests extends PetTeleportGameTests {
    public static final String MOD_ID = "waystonespetsaddon_gametest";

    static {
        // NeoForge only sends mod packets over connections that negotiated them, which vanilla's mock player never
        // does, so logging one in failed as soon as Balm synced Waystones' config to it.
        mockPlayerFactory = NeoForgeGameTests::makeMockPlayer;

        if (FMLEnvironment.dist.isClient()) {
            PetScreensClientCheck.startIfRequested();
        }
    }

    @Override
    @GameTestGenerator
    public Collection<TestFunction> tests() {
        return super.tests();
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
