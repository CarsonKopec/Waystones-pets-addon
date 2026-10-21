package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.PetFollowData;
import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetSelectionScreen;
import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetsButton;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import net.blay09.mods.waystones.api.MutableWaystone;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneTypes;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.blay09.mods.waystones.client.gui.widget.WaystoneButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Plays through the pet screens in a real client on any loader, the way a player would: creates a flat world, uses a
 * waystone, clicks the Pets button, switches a pet between Follows and Stays, then teleports. The loaders' test mods
 * start it when the client runs with -Dwaystonespetsaddon.clientcheck=true. The game exits with 0 once every step
 * has passed and with 1 when one fails. Each step is saved as a screenshot in the run folder's screenshots folder.
 */
public final class PetScreensClientCheck {
    public static final String PROPERTY = "waystonespetsaddon.clientcheck";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long STEP_TIMEOUT_MS = 60_000;
    private static final long WHOLE_CHECK_TIMEOUT_MS = 5 * 60_000;
    private static final String HOME = "Home";
    private static final String LOOKOUT = "Lookout";

    private record Setup(BlockPos home, BlockPos lookout, UUID wolf, UUID cat) {
    }

    private PetScreensClientCheck() {
    }

    /** Starts the check on its own thread if the client was asked to run it; it waits there for the title screen. */
    public static void startIfRequested() {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        Thread check = new Thread(PetScreensClientCheck::run, "Pet screens client check");
        check.setDaemon(true);
        check.start();
        // A step that never finishes, like a screen that never opens, would otherwise leave the game running forever.
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(WHOLE_CHECK_TIMEOUT_MS);
            } catch (InterruptedException e) {
                return;
            }
            LOGGER.error("Pet screens client check timed out");
            Runtime.getRuntime().halt(2);
        }, "Pet screens client check watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void run() {
        try {
            check();
            LOGGER.info("Pet screens client check passed");
            onClient(client -> {
                client.stop();
                return null;
            });
        } catch (Throwable failure) {
            LOGGER.error("Pet screens client check failed", failure);
            Runtime.getRuntime().halt(1);
        }
    }

    private static void check() {
        waitUntil("the game to start", () -> Minecraft.getInstance() != null);
        waitForScreen(TitleScreen.class);
        createFlatWorld();

        MinecraftServer server = onClient(Minecraft::getSingleplayerServer);
        Setup setup = onServer(server, PetScreensClientCheck::setUp);
        waitUntil("the pets to reach the client", () -> onClient(client ->
                client.level.getEntity(setup.wolf()) != null && client.level.getEntity(setup.cat()) != null));
        pause(1000);

        // A player's first click on a waystone they've activated opens its menu.
        onClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(setup.home()), Direction.UP, setup.home(), false)));
        waitForScreen(WaystoneSelectionScreenBase.class);
        pause(200);
        PetsButton petsButton = findOnly(PetsButton.class, "the Pets button on the waystone menu");
        assertClear(petsButton);
        screenshot("waystone_menu");

        click(petsButton);
        waitForScreen(PetSelectionScreen.class);
        waitUntil("both pets in the pet list", () -> followButtons().size() == 2);
        pause(200);
        screenshot("pet_list");

        // The server lists the closest pet first: the wolf, which starts out sitting and set to Stays.
        CycleButton<?> wolfButton = followButtons().getFirst();
        check(Boolean.FALSE.equals(wolfButton.getValue()), "The wolf's row should start out on Stays");
        check(Boolean.TRUE.equals(followButtons().get(1).getValue()), "The cat's row should start out on Follows");

        click(wolfButton);
        check(Boolean.TRUE.equals(wolfButton.getValue()), "Clicking Stays should switch the wolf's row to Follows");
        waitUntil("the wolf to follow and stand up", () -> onServer(server, s ->
                PetFollowData.follows(wolf(s, setup)) && !wolf(s, setup).isInSittingPose()));
        waitUntil("the client to see the wolf stand up", () -> onClient(client ->
                !((TamableAnimal) client.level.getEntity(setup.wolf())).isInSittingPose()));
        pause(200);
        screenshot("pet_list_wolf_follows");

        click(wolfButton);
        check(Boolean.FALSE.equals(wolfButton.getValue()), "Clicking Follows should switch the wolf's row back to Stays");
        waitUntil("the wolf to be set to Stays", () -> onServer(server, s -> !PetFollowData.follows(wolf(s, setup))));
        check(onServer(server, s -> !wolf(s, setup).isOrderedToSit()),
                "Switching to Stays should leave the wolf standing until its owner teleports");
        pause(200);
        screenshot("pet_list_wolf_stays");

        click(find(widget -> isTranslation(widget.getMessage(), "gui.done"), "the Done button"));
        waitForScreen(WaystoneSelectionScreenBase.class);
        pause(200);
        assertClear(findOnly(PetsButton.class, "the Pets button after returning from the pet screen"));
        screenshot("waystone_menu_after_pet_list");

        // Teleporting through the menu: the wolf is set to Stays, so it sits and stays behind.
        click(find(widget -> widget instanceof WaystoneButton && widget.getMessage().getString().contains(LOOKOUT),
                "the " + LOOKOUT + " waystone in the menu"));
        waitUntil("the player to arrive at the " + LOOKOUT, () -> onServer(server, s ->
                player(s).blockPosition().closerThan(setup.lookout(), 5)));
        // Standing, the wolf may have wandered a little, but it should be nowhere near the Lookout, 40 blocks away.
        String wolfState = onServer(server, s -> {
            TamableAnimal wolf = wolf(s, setup);
            return wolf.isOrderedToSit() && wolf.distanceTo(player(s)) > 20 ? null
                    : "sitting " + wolf.isOrderedToSit() + ", at " + wolf.blockPosition() + ", owner at " + player(s).blockPosition();
        });
        check(wolfState == null, "The wolf, set to Stays, should sit and stay behind when its owner teleports (" + wolfState + ")");
        waitForScreen(null);
        pause(500);
        screenshot("after_teleport");
    }

    /** Creates a new creative flat world through the Create New World screen, and waits until the player is in it. */
    private static void createFlatWorld() {
        onClient(client -> {
            CreateWorldScreen.openFresh(client, () -> client.gui.setScreen(new TitleScreen()));
            return null;
        });
        waitForScreen(CreateWorldScreen.class);
        onClient(client -> {
            WorldCreationUiState state = ((CreateWorldScreen) client.gui.screen()).getUiState();
            state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            state.setWorldType(new WorldCreationUiState.WorldTypeEntry(state.getSettings().worldgenLoadContext()
                    .lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)));
            state.setGenerateStructures(false);
            return null;
        });
        pause(200);
        // Pressed rather than clicked: on this screen, the click goes to the tab contents in front of the button.
        AbstractWidget create = find(widget -> isTranslation(widget.getMessage(), "selectWorld.create"), "the Create New World button");
        onClient(client -> {
            ((AbstractButton) create).onPress(new MouseButtonEvent(0, 0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)));
            return null;
        });
        waitUntil("the new world to load", () -> onClient(client -> client.level != null && client.player != null
                && client.getSingleplayerServer() != null && client.gui.screen() == null));
        pause(2000);
    }

    /** Puts two waystones the player has activated in the world, a sitting wolf on Stays and a named, sitting cat. */
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

    private static void waitForScreen(Class<? extends Screen> type) {
        waitUntil(type == null ? "no screen" : "the " + type.getSimpleName(), () -> onClient(client -> {
            Screen screen = client.gui.screen();
            return type == null ? screen == null : type.isInstance(screen);
        }));
    }

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

    /** Whether the listener is the widget, or contains it, like a list holding the row a button is in. */
    private static boolean holds(GuiEventListener listener, AbstractWidget widget) {
        if (listener == widget) {
            return true;
        }
        if (listener instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                if (holds(child, widget)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<CycleButton<?>> followButtons() {
        return onClient(client -> {
            List<CycleButton<?>> buttons = new ArrayList<>();
            for (AbstractWidget widget : widgets(client)) {
                if (widget instanceof CycleButton<?> button) {
                    buttons.add(button);
                }
            }
            return buttons;
        });
    }

    private static <T extends AbstractWidget> T findOnly(Class<T> type, String description) {
        List<AbstractWidget> matches = onClient(client -> widgets(client).stream().filter(type::isInstance).toList());
        check(matches.size() == 1, "Expected exactly one " + description + ", found " + matches.size());
        return type.cast(matches.getFirst());
    }

    private static AbstractWidget find(Predicate<AbstractWidget> predicate, String description) {
        return onClient(client -> widgets(client).stream().filter(predicate).findFirst()
                .orElseThrow(() -> new AssertionError("Could not find " + description)));
    }

    private static boolean isTranslation(Component text, String key) {
        return text.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key);
    }

    /** Fails if the widget is off the screen, or another widget covers part of it and would take some of its clicks. */
    private static void assertClear(AbstractWidget widget) {
        String overlapping = onClient(client -> {
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

    /**
     * Moves the mouse to the middle of a widget and clicks there, through the game's own mouse handling, the way a
     * player does. Fails if something else on the screen is under that spot and would take the click.
     */
    private static void click(AbstractWidget widget) {
        moveMouseTo(widget);
        String target = onClient(client -> {
            double x = widget.getX() + widget.getWidth() / 2.0;
            double y = widget.getY() + widget.getHeight() / 2.0;
            String under = client.gui.screen().getChildAt(x, y)
                    .map(child -> holds(child, widget) ? "" : child.getClass().getName())
                    .orElse("nothing");
            long window = client.getWindow().handle();
            client.mouseHandler.onButton(window, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.PRESS);
            client.mouseHandler.onButton(window, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.RELEASE);
            return under;
        });
        check(target.isEmpty(), "A click on " + widget.getClass().getSimpleName() + " at " + bounds(widget) + " went to " + target);
        pause(100);
    }

    /** Moves the mouse over a widget, and waits for the screen to draw it hovered. */
    private static void moveMouseTo(AbstractWidget widget) {
        onClient(client -> {
            Window window = client.getWindow();
            client.mouseHandler.onMove(window.handle(),
                    (widget.getX() + widget.getWidth() / 2.0) * window.getScreenWidth() / window.getGuiScaledWidth(),
                    (widget.getY() + widget.getHeight() / 2.0) * window.getScreenHeight() / window.getGuiScaledHeight(), 0, 0);
            return null;
        });
        pause(150);
    }

    /** Moves the mouse into the corner, off the widgets and away from the pet models, which turn to follow it. */
    private static void moveMouseAway() {
        onClient(client -> {
            client.mouseHandler.onMove(client.getWindow().handle(), 0, 0, 0, 0);
            return null;
        });
        pause(150);
    }

    private static void screenshot(String name) {
        moveMouseAway();
        onClient(client -> {
            Screenshot.grab(client.gameDirectory, name + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> {
            });
            return null;
        });
        pause(200);
    }

    // --- threads ---

    private static <T> T onClient(Function<Minecraft, T> action) {
        Minecraft client = Minecraft.getInstance();
        return client.submit(() -> action.apply(client)).join();
    }

    private static <T> T onServer(MinecraftServer server, Function<MinecraftServer, T> action) {
        return server.submit(() -> action.apply(server)).join();
    }

    private static void waitUntil(String what, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + STEP_TIMEOUT_MS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                String showing = Minecraft.getInstance() == null ? "the game hadn't started"
                        : onClient(client -> client.gui.screen() == null ? "no screen" : client.gui.screen().getClass().getName());
                throw new AssertionError("Timed out waiting for " + what + " (showing " + showing + ")");
            }
            pause(50);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted", e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
