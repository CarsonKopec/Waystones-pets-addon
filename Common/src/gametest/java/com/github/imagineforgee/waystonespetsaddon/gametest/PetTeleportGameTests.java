package com.github.imagineforgee.waystonespetsaddon.gametest;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.PetFollowData;
import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportConfig;
import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportHandler;
import com.github.imagineforgee.waystonespetsaddon.common.network.SetPetFollowsMessage;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.WaystoneStyles;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.api.WaystoneTeleportError;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.blay09.mods.waystones.core.WarpMode;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * End-to-end tests that teleport a player through real Waystones waystones, so they cover Waystones' own pet
 * transport and event flow as well as this mod. Each test but the Nether one finishes within its first tick.
 */
public class PetTeleportGameTests {
    private static final String STRUCTURE = Constants.MOD_ID + ":pets_platform";
    // High above the platform: farther than the default teleport radius, and clear of other tests' platforms.
    private static final BlockPos DESTINATION = new BlockPos(2, 40, 2);
    private static final BlockPos OWNER_POS = new BlockPos(2, 1, 2);

    private static boolean cancelNextTeleport;

    /** Creates the tests' fake players. A loader whose rules vanilla's mock player breaks can swap in its own. */
    protected static Function<GameTestHelper, ServerPlayer> mockPlayerFactory = GameTestHelper::makeMockServerPlayerInLevel;

    static {
        // Lets a test deny a teleport the way an XP cost or another mod would, after this mod's Pre handler ran.
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Pre.class, event -> {
            if (cancelNextTeleport) {
                event.setCanceled(true);
            }
        }, EventPriority.Lowest);
    }

    @GameTestGenerator
    public Collection<TestFunction> tests() {
        return List.of(
                test("following_pet_teleports_with_owner", PetTeleportGameTests::followingPetTeleportsWithOwner),
                test("staying_pet_stays_behind_and_sits", PetTeleportGameTests::stayingPetStaysBehindAndSits),
                test("sitting_pet_stays_behind", PetTeleportGameTests::sittingPetStaysBehind),
                test("pet_outside_radius_stays_behind", PetTeleportGameTests::petOutsideRadiusStaysBehind),
                test("other_players_pet_stays_behind", PetTeleportGameTests::otherPlayersPetStaysBehind),
                test("max_pets_takes_the_closest", PetTeleportGameTests::maxPetsTakesTheClosest),
                test("cancelled_teleport_leaves_pets_alone", PetTeleportGameTests::cancelledTeleportLeavesPetsAlone),
                test("pet_follows_into_another_dimension", PetTeleportGameTests::petFollowsIntoAnotherDimension),
                test("switching_to_follows_stands_pet_up", PetTeleportGameTests::switchingToFollowsStandsPetUp),
                test("stay_choice_survives_save_and_load", PetTeleportGameTests::stayChoiceSurvivesSaveAndLoad),
                test("pet_list_is_closest_first_and_owned_only", PetTeleportGameTests::petListIsClosestFirstAndOwnedOnly));
    }

    private static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction(Constants.MOD_ID, Constants.MOD_ID + "." + name, STRUCTURE, 100, 0, true, body);
    }

    private static void followingPetTeleportsWithOwner(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        Wolf pet = spawnPet(helper, owner, 3, 3);

        teleport(owner, placeWaystone(helper.getLevel(), helper.absolutePos(DESTINATION)));

        assertNextTo(pet, owner, "A following pet should arrive next to its owner");
        finish(helper, owner, pet);
    }

    private static void stayingPetStaysBehindAndSits(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        Wolf pet = spawnPet(helper, owner, 3, 3);
        PetFollowData.setFollows(pet, false);
        Vec3 start = pet.position();

        teleport(owner, placeWaystone(helper.getLevel(), helper.absolutePos(DESTINATION)));

        assertStayed(pet, start, "A pet set to Stays should not teleport, not even through Waystones' own pet transport");
        check(pet.isOrderedToSit(), "A pet set to Stays should be told to sit when its owner teleports");
        finish(helper, owner, pet);
    }

    private static void sittingPetStaysBehind(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        Wolf pet = spawnPet(helper, owner, 3, 3);
        pet.setOrderedToSit(true);
        pet.setInSittingPose(true);
        Vec3 start = pet.position();

        teleport(owner, placeWaystone(helper.getLevel(), helper.absolutePos(DESTINATION)));

        assertStayed(pet, start, "A sitting pet should stay behind while teleportSittingPets is off");
        finish(helper, owner, pet);
    }

    private static void petOutsideRadiusStaysBehind(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        Wolf pet = spawnPet(helper, owner, 14, 14); // 12 blocks away on both axes, outside the default radius of 10
        Vec3 start = pet.position();

        teleport(owner, placeWaystone(helper.getLevel(), helper.absolutePos(DESTINATION)));

        assertStayed(pet, start, "A pet outside the teleport radius should stay behind");
        finish(helper, owner, pet);
    }

    private static void otherPlayersPetStaysBehind(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        ServerPlayer otherPlayer = spawnOwner(helper);
        otherPlayer.moveTo(helper.absoluteVec(new Vec3(10.5, 1, 10.5)));
        Wolf othersPet = spawnPet(helper, otherPlayer, 3, 3);
        Vec3 start = othersPet.position();

        teleport(owner, placeWaystone(helper.getLevel(), helper.absolutePos(DESTINATION)));

        assertStayed(othersPet, start, "Another player's pet should never be taken along");
        removePlayer(otherPlayer);
        finish(helper, owner, othersPet);
    }

    private static void maxPetsTakesTheClosest(GameTestHelper helper) {
        PetTeleportConfig.ConfigValues original = PetTeleportConfig.values;
        PetTeleportConfig.values = copyOf(original);
        PetTeleportConfig.values.maxPetsToTeleport = 2;
        try {
            ServerPlayer owner = spawnOwner(helper);
            Wolf closest = spawnPet(helper, owner, 3, 2);
            Wolf second = spawnPet(helper, owner, 5, 2);
            Wolf farthest = spawnPet(helper, owner, 8, 2);
            Vec3 farthestStart = farthest.position();

            teleport(owner, placeWaystone(helper.getLevel(), helper.absolutePos(DESTINATION)));

            assertNextTo(closest, owner, "The closest pet should teleport");
            assertNextTo(second, owner, "The second closest pet should teleport");
            assertStayed(farthest, farthestStart, "With maxPetsToTeleport = 2, the farthest of three pets should stay");
            finish(helper, owner, closest, second, farthest);
        } finally {
            PetTeleportConfig.values = original;
        }
    }

    private static void cancelledTeleportLeavesPetsAlone(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        Wolf follower = spawnPet(helper, owner, 3, 3);
        Wolf stayer = spawnPet(helper, owner, 4, 3);
        PetFollowData.setFollows(stayer, false);
        Vec3 followerStart = follower.position();
        Vec3 stayerStart = stayer.position();
        IWaystone destination = placeWaystone(helper.getLevel(), helper.absolutePos(DESTINATION));

        cancelNextTeleport = true;
        WaystoneTeleportError error;
        try {
            error = tryTeleport(owner, destination);
        } finally {
            cancelNextTeleport = false;
        }

        check(error != null, "The teleport should have been cancelled");
        assertStayed(follower, followerStart, "A denied teleport should not move pets");
        assertStayed(stayer, stayerStart, "A denied teleport should not move pets");
        check(!stayer.isOrderedToSit(), "A denied teleport should not make Stays pets sit");
        finish(helper, owner, follower, stayer);
    }

    private static void petFollowsIntoAnotherDimension(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        check(nether != null, "The test server should have a Nether");
        // The middle of a chunk, so the owner and the pet both land in that chunk.
        BlockPos netherPos = new ChunkPos(helper.absolutePos(OWNER_POS)).getMiddleBlockPosition(100);
        ChunkPos chunk = new ChunkPos(netherPos);
        // A real player arriving keeps the chunk loaded; the mock player does not, so load it the same way.
        nether.setChunkForced(chunk.x, chunk.z, true);
        // A chunk's entities only show up once every chunk within two of it has generated. Left to the worker threads,
        // that sometimes took longer than the whole test, as the game test server runs ticks as fast as it can.
        generateChunksAround(nether, chunk, 2);
        buildSealedRoom(nether, netherPos);

        ServerPlayer owner = spawnOwner(helper);
        Wolf pet = spawnPet(helper, owner, 3, 3);

        teleport(owner, placeWaystone(nether, netherPos));

        check(owner.level() == nether, "The owner should now be in the Nether");
        check(pet.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION,
                "The pet should have left the Overworld (removed=" + pet.isRemoved() + ", reason=" + pet.getRemovalReason() + ")");
        // A pet moved into a chunk whose entities are still loading only shows up once they are, so wait for it the
        // way a player would, rather than checking within the same tick.
        helper.startSequence()
                .thenWaitUntil(() -> {
                    Entity arrived = nether.getEntity(pet.getUUID());
                    check(arrived instanceof Wolf, "The pet should have followed its owner into the Nether (wolves near the owner there: "
                            + nether.getEntitiesOfClass(Wolf.class, owner.getBoundingBox().inflate(16)).size()
                            + ", chunk entities loaded: " + nether.areEntitiesLoaded(chunk.toLong()) + ")");
                    assertNextTo(arrived, owner, "The pet should arrive next to its owner in the Nether");
                })
                .thenExecute(() -> {
                    Entity arrived = nether.getEntity(pet.getUUID());
                    if (arrived != null) {
                        arrived.discard();
                    }
                    removePlayer(owner);
                    nether.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenSucceed();
    }

    private static void generateChunksAround(ServerLevel level, ChunkPos center, int radius) {
        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int z = center.z - radius; z <= center.z + radius; z++) {
                level.getChunk(x, z);
            }
        }
    }

    private static void switchingToFollowsStandsPetUp(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        Wolf pet = spawnPet(helper, owner, 3, 3);
        PetFollowData.setFollows(pet, false);
        pet.setOrderedToSit(true);
        pet.setInSittingPose(true);

        SetPetFollowsMessage.handle(owner, new SetPetFollowsMessage(pet.getUUID(), true));

        check(PetFollowData.follows(pet), "Switching to Follows should save the choice");
        check(!pet.isOrderedToSit() && !pet.isInSittingPose(), "Switching to Follows should stand the pet up");

        SetPetFollowsMessage.handle(owner, new SetPetFollowsMessage(pet.getUUID(), false));

        check(!PetFollowData.follows(pet), "Switching to Stays should save the choice");
        check(!pet.isOrderedToSit(), "Switching to Stays should not sit the pet until its owner teleports");
        finish(helper, owner, pet);
    }

    private static void stayChoiceSurvivesSaveAndLoad(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        Wolf pet = spawnPet(helper, owner, 3, 3);
        PetFollowData.setFollows(pet, false);

        CompoundTag saved = pet.saveWithoutId(new CompoundTag());
        Wolf reloaded = EntityType.WOLF.create(helper.getLevel());
        check(reloaded != null, "Could not create a wolf to load into");
        reloaded.load(saved);

        check(!PetFollowData.follows(reloaded), "A pet set to Stays should still stay after being saved and loaded");
        finish(helper, owner, pet);
    }

    private static void petListIsClosestFirstAndOwnedOnly(GameTestHelper helper) {
        ServerPlayer owner = spawnOwner(helper);
        ServerPlayer otherPlayer = spawnOwner(helper);
        Wolf far = spawnPet(helper, owner, 7, 2);
        Wolf near = spawnPet(helper, owner, 3, 2);
        Wolf othersPet = spawnPet(helper, otherPlayer, 4, 2);
        Wolf outOfRange = spawnPet(helper, owner, 14, 14);

        List<TamableAnimal> pets = PetTeleportHandler.findNearbyPets(owner);

        check(pets.equals(List.of(near, far)),
                "The pet list should hold only the owner's pets in range, closest first, but was " + names(pets));
        removePlayer(otherPlayer);
        finish(helper, owner, far, near, othersPet, outOfRange);
    }

    // --- helpers ---

    private static ServerPlayer spawnOwner(GameTestHelper helper) {
        ServerPlayer player = mockPlayerFactory.apply(helper);
        // Creative skips Waystones' XP and item costs, which are not what these tests are about.
        player.setGameMode(GameType.CREATIVE);
        player.moveTo(helper.absoluteVec(Vec3.atBottomCenterOf(OWNER_POS)));
        return player;
    }

    private static Wolf spawnPet(GameTestHelper helper, ServerPlayer owner, int x, int z) {
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(x, 1, z));
        wolf.tame(owner);
        return wolf;
    }

    private static IWaystone placeWaystone(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        return WaystonesAPI.placeWaystone(level, pos, WaystoneStyles.DEFAULT)
                .orElseThrow(() -> new GameTestAssertException("Could not place a waystone at " + pos));
    }

    /**
     * Builds a sealed stone room around {@code center}. Game tests run at random coordinates, so the Nether around
     * them differs every run; an open pocket sometimes let lava in, which killed the pet and failed the test.
     */
    private static void buildSealedRoom(ServerLevel level, BlockPos center) {
        BlockPos min = center.offset(-3, -1, -3);
        BlockPos max = center.offset(3, 4, 3);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            boolean wall = pos.getX() == min.getX() || pos.getX() == max.getX()
                    || pos.getY() == min.getY() || pos.getY() == max.getY()
                    || pos.getZ() == min.getZ() || pos.getZ() == max.getZ();
            level.setBlockAndUpdate(pos, wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
    }

    private static void teleport(ServerPlayer player, IWaystone destination) {
        WaystoneTeleportError error = tryTeleport(player, destination);
        if (error != null) {
            throw new GameTestAssertException("The waystone teleport failed: " + describe(error));
        }
    }

    /** Teleports the way a player using one waystone to reach another does. */
    private static WaystoneTeleportError tryTeleport(ServerPlayer player, IWaystone destination) {
        IWaystone origin = placeWaystone(player.serverLevel(), player.blockPosition().offset(2, 0, -2));
        return WaystonesAPI.createDefaultTeleportContext(player, destination, WarpMode.WAYSTONE_TO_WAYSTONE, origin)
                .flatMap(context -> WaystonesAPI.tryTeleport(context))
                .right()
                .orElse(null);
    }

    private static void assertNextTo(Entity pet, ServerPlayer owner, String message) {
        check(pet.level() == owner.level() && pet.distanceTo(owner) < 3,
                message + " (pet at " + pet.position() + " in " + pet.level().dimension().location()
                        + ", owner at " + owner.position() + " in " + owner.level().dimension().location() + ")");
    }

    private static void assertStayed(Entity pet, Vec3 start, String message) {
        check(!pet.isRemoved() && pet.position().distanceTo(start) < 1, message + " (pet moved to " + pet.position() + ")");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    /** Removes everything a test created outside its platform, then passes it. */
    private static void finish(GameTestHelper helper, ServerPlayer owner, Entity... pets) {
        for (Entity pet : pets) {
            pet.discard();
        }
        removePlayer(owner);
        helper.succeed();
    }

    private static void removePlayer(ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
    }

    private static PetTeleportConfig.ConfigValues copyOf(PetTeleportConfig.ConfigValues values) {
        PetTeleportConfig.ConfigValues copy = new PetTeleportConfig.ConfigValues();
        copy.maxPetsToTeleport = values.maxPetsToTeleport;
        copy.teleportRadius = values.teleportRadius;
        copy.teleportSittingPets = values.teleportSittingPets;
        copy.forceUnsitPets = values.forceUnsitPets;
        copy.randomizeTeleportOffset = values.randomizeTeleportOffset;
        copy.teleportDelayTicks = values.teleportDelayTicks;
        return copy;
    }

    private static String describe(WaystoneTeleportError error) {
        return error.getClass().getSimpleName();
    }

    private static List<String> names(List<TamableAnimal> pets) {
        List<String> names = new ArrayList<>();
        for (TamableAnimal pet : pets) {
            names.add(pet.getUUID().toString().substring(0, 8) + " @ " + pet.blockPosition());
        }
        return names;
    }
}
