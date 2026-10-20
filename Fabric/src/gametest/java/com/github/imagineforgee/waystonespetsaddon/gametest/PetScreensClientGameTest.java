package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.PetFollowData;
import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetSelectionScreen;
import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetsButton;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.blay09.mods.waystones.api.MutableWaystone;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTypes;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneButton;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Plays through the pet screens in a real client the way a player would: uses a waystone, clicks the Pets button,
 * and switches a pet between Follows and Stays with the mouse. Saves a screenshot of each step to
 * build/clientgametest/screenshots, so the screens can be looked over as well as checked.
 */
public class PetScreensClientGameTest implements FabricClientGameTest {
    private static final String HOME = "Home";
    private static final String LOOKOUT = "Lookout";

    private record Setup(BlockPos home, BlockPos lookout, UUID wolf, UUID cat) {
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            singleplayer.getConnection().waitForChunksRender();
            Setup setup = server.computeOnServer(PetScreensClientGameTest::setUp);
            context.waitFor(client -> client.level.getEntity(setup.wolf()) != null && client.level.getEntity(setup.cat()) != null);
            singleplayer.getConnection().waitForChunksRender();

            // A player's first click on a waystone they've activated opens its menu.
            context.getInput().lookAt(setup.home());
            context.getInput().pressKey(options -> options.keyUse);
            context.waitForScreen(WaystoneSelectionScreenBase.class);
            context.waitTicks(2);
            PetsButton petsButton = findOnly(context, PetsButton.class, "the Pets button on the waystone menu");
            context.takeScreenshot("waystone_menu");
            assertClear(context, petsButton);

            hover(context, petsButton);
            context.takeScreenshot("waystone_menu_pets_tooltip");
            click(context, petsButton);
            context.waitForScreen(PetSelectionScreen.class);
            context.waitFor(client -> followButtons(client).size() == 2);
            context.waitTicks(2);
            moveMouseAway(context);
            context.takeScreenshot("pet_list");

            // The server lists the closest pet first: the wolf, which starts out sitting and set to Stays.
            CycleButton<?> wolfButton = followButtons(context).getFirst();
            check(Boolean.FALSE.equals(wolfButton.getValue()), "The wolf's row should start out on Stays");
            check(Boolean.TRUE.equals(followButtons(context).get(1).getValue()), "The cat's row should start out on Follows");

            click(context, wolfButton);
            check(Boolean.TRUE.equals(wolfButton.getValue()), "Clicking Stays should switch the wolf's row to Follows");
            server.waitFor(s -> PetFollowData.follows(wolf(s, setup)) && !wolf(s, setup).isInSittingPose());
            context.waitFor(client -> !((TamableAnimal) client.level.getEntity(setup.wolf())).isInSittingPose());
            context.waitTicks(2);
            context.takeScreenshot("pet_list_wolf_follows_tooltip");

            click(context, wolfButton);
            check(Boolean.FALSE.equals(wolfButton.getValue()), "Clicking Follows should switch the wolf's row back to Stays");
            server.waitFor(s -> !PetFollowData.follows(wolf(s, setup)));
            check(server.computeOnServer(s -> !wolf(s, setup).isOrderedToSit()),
                    "Switching to Stays should leave the wolf standing until its owner teleports");
            moveMouseAway(context);
            context.waitTicks(2);
            context.takeScreenshot("pet_list_wolf_stays");

            // The pet screen keeps its choices when the window changes size, and so does the waystone menu's button.
            // At 640x480 the screen is as narrow as Minecraft makes it, so details that don't fit end in an ellipsis.
            resizeWindow(context, 640, 480);
            check(followButtons(context).size() == 2 && Boolean.FALSE.equals(followButtons(context).getFirst().getValue()),
                    "Resizing the window should keep the pet list and the wolf's Stays choice");
            context.takeScreenshot("pet_list_640x480");
            resizeWindow(context, 1280, 720);
            context.takeScreenshot("pet_list_1280x720");

            context.clickScreenButton("gui.done");
            context.waitForScreen(WaystoneSelectionScreenBase.class);
            context.waitTicks(2);
            findOnly(context, PetsButton.class, "the Pets button after returning from the pet screen");
            context.takeScreenshot("waystone_menu_1280x720");
            assertClear(context, findOnly(context, PetsButton.class, "the Pets button"));

            // Teleporting through the menu: the wolf is set to Stays, so it sits and stays behind.
            click(context, findWaystoneButton(context, LOOKOUT));
            server.waitFor(s -> player(s).blockPosition().closerThan(setup.lookout(), 5));
            check(server.computeOnServer(s -> wolf(s, setup).isOrderedToSit()
                            && wolf(s, setup).blockPosition().closerThan(setup.home(), 5)),
                    "The wolf, set to Stays, should sit and stay behind when its owner teleports");
            context.waitForScreen(null);
        }
    }

    /** Puts two waystones the player has activated in a flat world, a sitting wolf on Stays and a named, sitting cat. */
    private static Setup setUp(MinecraftServer server) {
        ServerPlayer player = player(server);
        // Creative skips Waystones' XP cost, so the destination isn't greyed out.
        player.setGameMode(GameType.CREATIVE);
        ServerLevel level = player.level();
        BlockPos feet = player.blockPosition();

        BlockPos home = feet.north(3);
        BlockPos lookout = feet.east(40);
        placeActivatedWaystone(player, home, HOME);
        placeActivatedWaystone(player, lookout, LOOKOUT);

        TamableAnimal wolf = spawnPet(level, EntityTypes.WOLF, feet.west(2), player);
        PetFollowData.setFollows(wolf, false);
        TamableAnimal cat = spawnPet(level, EntityTypes.CAT, feet.east(2).south(2), player);
        cat.setCustomName(Component.literal("Whiskers"));
        return new Setup(home, lookout, wolf.getUUID(), cat.getUUID());
    }

    private static void placeActivatedWaystone(ServerPlayer player, BlockPos pos, String name) {
        Waystone waystone = WaystonesAPI.placeWaystone(player.level(), pos, WaystoneTypes.ANDESITE)
                .orElseThrow(() -> new AssertionError("Could not place a waystone at " + pos));
        if (waystone instanceof MutableWaystone mutable) {
            mutable.setName(Component.literal(name));
        }
        WaystonesAPI.activateWaystone(player, waystone);
    }

    /** A tamed pet told to sit, so it stays where it is and the list order stays the same. */
    private static TamableAnimal spawnPet(ServerLevel level, EntityType<? extends TamableAnimal> type, BlockPos pos, ServerPlayer owner) {
        TamableAnimal pet = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (pet == null) {
            throw new AssertionError("Could not spawn a " + type + " at " + pos);
        }
        // Not tame(owner), which grants an advancement whose toast would cover the screens in the screenshots.
        pet.setTame(true, true);
        pet.setOwner(owner);
        pet.setOrderedToSit(true);
        pet.setInSittingPose(true);
        return pet;
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static TamableAnimal wolf(MinecraftServer server, Setup setup) {
        return (TamableAnimal) player(server).level().getEntity(setup.wolf());
    }

    // --- screens ---

    /** Every widget on the current screen, including those in lists, in the order they're offered clicks. */
    private static List<AbstractWidget> widgets(Minecraft client) {
        List<AbstractWidget> found = new ArrayList<>();
        Screen screen = client.gui.screen();
        if (screen != null) {
            collectWidgets(screen, found);
        }
        return found;
    }

    private static void collectWidgets(GuiEventListener listener, List<AbstractWidget> found) {
        if (listener instanceof AbstractWidget widget) {
            found.add(widget);
        }
        if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                collectWidgets(child, found);
            }
        }
    }

    private static List<CycleButton<?>> followButtons(Minecraft client) {
        List<CycleButton<?>> buttons = new ArrayList<>();
        for (AbstractWidget widget : widgets(client)) {
            if (widget instanceof CycleButton<?> button) {
                buttons.add(button);
            }
        }
        return buttons;
    }

    private static List<CycleButton<?>> followButtons(ClientGameTestContext context) {
        return context.computeOnClient(PetScreensClientGameTest::followButtons);
    }

    private static <T extends AbstractWidget> T findOnly(ClientGameTestContext context, Class<T> type, String description) {
        List<AbstractWidget> matches = context.computeOnClient(client -> widgets(client).stream().filter(type::isInstance).toList());
        check(matches.size() == 1, "Expected exactly one " + description + ", found " + matches.size());
        return type.cast(matches.getFirst());
    }

    private static AbstractWidget findWaystoneButton(ClientGameTestContext context, String name) {
        return find(context, widget -> widget instanceof AbstractWaystoneButton
                && widget.getMessage().getString().contains(name), "the " + name + " waystone in the menu");
    }

    private static AbstractWidget find(ClientGameTestContext context, Predicate<AbstractWidget> predicate, String description) {
        return context.computeOnClient(client -> widgets(client).stream().filter(predicate).findFirst()
                .orElseThrow(() -> new AssertionError("Could not find " + description)));
    }

    /** Fails if another widget covers part of this one, as it would take some of its clicks. */
    private static void assertClear(ClientGameTestContext context, AbstractWidget widget) {
        String overlapping = context.computeOnClient(client -> {
            Screen screen = client.gui.screen();
            check(widget.getX() >= 0 && widget.getY() >= 0
                            && widget.getX() + widget.getWidth() <= screen.width && widget.getY() + widget.getHeight() <= screen.height,
                    widget.getClass().getSimpleName() + " at " + bounds(widget) + " is off the " + screen.width + "x" + screen.height + " screen");
            return widgets(client).stream()
                    .filter(other -> other != widget && other.visible && overlap(widget, other))
                    .map(other -> other.getClass().getSimpleName() + " at " + bounds(other))
                    .toList()
                    .toString();
        });
        check(overlapping.equals("[]"), widget.getClass().getSimpleName() + " at " + bounds(widget) + " overlaps " + overlapping);
    }

    private static boolean overlap(AbstractWidget a, AbstractWidget b) {
        return a.getX() < b.getX() + b.getWidth() && b.getX() < a.getX() + a.getWidth()
                && a.getY() < b.getY() + b.getHeight() && b.getY() < a.getY() + a.getHeight();
    }

    private static String bounds(AbstractWidget widget) {
        return "(" + widget.getX() + ", " + widget.getY() + ", " + widget.getWidth() + "x" + widget.getHeight() + ")";
    }

    // --- mouse ---

    /** Clicks the middle of a widget with the mouse, so the click is routed the way a player's is. */
    private static void click(ClientGameTestContext context, AbstractWidget widget) {
        hover(context, widget);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTick();
    }

    private static void hover(ClientGameTestContext context, AbstractWidget widget) {
        double[] pos = context.computeOnClient(client -> {
            Window window = client.getWindow();
            return new double[] {
                    (widget.getX() + widget.getWidth() / 2.0) * window.getScreenWidth() / window.getGuiScaledWidth(),
                    (widget.getY() + widget.getHeight() / 2.0) * window.getScreenHeight() / window.getGuiScaledHeight()};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.waitTicks(2);
    }

    /** Resizes the window, then moves the mouse back off the screen's widgets and the pet models that follow it. */
    private static void resizeWindow(ClientGameTestContext context, int width, int height) {
        context.getInput().resizeWindow(width, height);
        context.waitTick();
        moveMouseAway(context);
        context.waitTicks(2);
    }

    private static void moveMouseAway(ClientGameTestContext context) {
        context.getInput().setCursorPos(0, 0);
        context.waitTick();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
