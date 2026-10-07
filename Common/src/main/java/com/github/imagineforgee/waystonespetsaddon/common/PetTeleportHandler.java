package com.github.imagineforgee.waystonespetsaddon.common;

import com.github.imagineforgee.waystonespetsaddon.common.api.PlatformAbstractions;
import com.github.imagineforgee.waystonespetsaddon.common.api.TickDelayedTaskManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class PetTeleportHandler {
    /** The player's pets within teleport range, closest first. Also what the pet selection screen lists. */
    public static List<TamableAnimal> findNearbyPets(ServerPlayer player) {
        List<TamableAnimal> pets = player.level().getEntitiesOfClass(TamableAnimal.class,
                player.getBoundingBox().inflate(PetTeleportConfig.values.teleportRadius),
                pet -> pet.isTame() && player.getUUID().equals(pet.getOwnerUUID()));
        pets.sort(Comparator.comparingDouble(pet -> pet.distanceToSqr(player)));
        return pets;
    }

    /**
     * Brings along the pets that follow and sits down the ones told to stay.
     *
     * @param nearbyPets the pets that were near the player before they teleported
     */
    public static void handleTeleport(ServerPlayer player, List<TamableAnimal> nearbyPets, Vec3 targetVec, ServerLevel targetLevel) {
        Vec3i targetVecI = new Vec3i(
                (int) Math.floor(targetVec.x),
                (int) Math.floor(targetVec.y),
                (int) Math.floor(targetVec.z)
        );
        BlockPos targetPos = new BlockPos(targetVecI);

        nearbyPets = nearbyPets.stream().filter(pet -> !pet.isRemoved()).toList();

        // A standing pet left behind would otherwise teleport itself to its owner through vanilla's
        // follow AI whenever the destination is in the same dimension and close enough to stay loaded.
        for (TamableAnimal pet : nearbyPets) {
            if (!PetFollowData.follows(pet) && !pet.isOrderedToSit()) {
                setOrderedToSit(pet, true);
            }
        }

        var pets = nearbyPets.stream()
                .filter(pet -> PetFollowData.follows(pet)
                        && (PetTeleportConfig.values.teleportSittingPets || !pet.isInSittingPose()))
                .toList();

        int count = 0;

        for (TamableAnimal pet : pets) {
            if (count >= PetTeleportConfig.values.maxPetsToTeleport) break;

            if (PetTeleportConfig.values.forceUnsitPets) {
                standUp(pet);
            }

            double offsetX = 0;
            double offsetZ = 0;

            if (PetTeleportConfig.values.randomizeTeleportOffset) {
                offsetX = (player.getRandom().nextDouble() - 0.5) * 2;
                offsetZ = (player.getRandom().nextDouble() - 0.5) * 2;
            }

            double finalX = targetPos.getX() + 0.5 + offsetX;
            double finalY = targetPos.getY();
            double finalZ = targetPos.getZ() + 0.5 + offsetZ;

            if (PetTeleportConfig.values.teleportDelayTicks > 0) {
                int delay = PetTeleportConfig.values.teleportDelayTicks;

                TickDelayedTaskManager.schedule(PlatformAbstractions.createDelayedTask(delay, () -> {
                    // The pet may have died or been unloaded while waiting; teleporting it across
                    // dimensions now would spawn a copy of it.
                    if (!pet.isRemoved()) {
                        teleportPet(pet, targetLevel, finalX, finalY, finalZ);
                    }
                }));
            } else {
                teleportPet(pet, targetLevel, finalX, finalY, finalZ);
            }

            count++;
        }
    }

    /**
     * Stands a sitting pet up straight away, so it can be teleported this tick. Clearing only the pose isn't
     * enough: the pet would sit right back down while still ordered to sit.
     */
    public static void standUp(TamableAnimal pet) {
        if (pet.isOrderedToSit() || pet.isInSittingPose()) {
            setOrderedToSit(pet, false);
            pet.setInSittingPose(false);
        }
    }

    // Same as the owner right-clicking the pet in vanilla.
    private static void setOrderedToSit(TamableAnimal pet, boolean sit) {
        pet.setOrderedToSit(sit);
        pet.setJumping(false);
        pet.getNavigation().stop();
        pet.setTarget(null);
    }

    // Unlike changeDimension, this lands the pet at the exact position in any dimension
    // instead of requiring a portal near the destination.
    private static void teleportPet(TamableAnimal pet, ServerLevel targetLevel, double x, double y, double z) {
        pet.teleportTo(targetLevel, x, y, z, Set.of(), pet.getYRot(), pet.getXRot());
    }
}
