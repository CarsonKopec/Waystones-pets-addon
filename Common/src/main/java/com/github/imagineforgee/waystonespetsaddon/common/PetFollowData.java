package com.github.imagineforgee.waystonespetsaddon.common;

import net.blay09.mods.balm.api.Balm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.TamableAnimal;

/**
 * Whether a pet should teleport with its owner, stored on the pet itself so it survives
 * saving, unloading and dimension changes. Pets follow unless they have been told to stay.
 */
public class PetFollowData {
    private static final String STAYS_KEY = Constants.MOD_ID + ":stays";

    public static boolean follows(TamableAnimal pet) {
        return !Balm.getHooks().getPersistentData(pet).getBooleanOr(STAYS_KEY, false);
    }

    public static void setFollows(TamableAnimal pet, boolean follows) {
        CompoundTag data = Balm.getHooks().getPersistentData(pet);
        if (follows) {
            data.remove(STAYS_KEY);
        } else {
            data.putBoolean(STAYS_KEY, true);
        }
        // Balm 21 hands out a tag that isn't saved yet while the pet has no Balm data, and only takes it over on the
        // next lookup once it holds something. Look it up again, so the choice is saved even if the world closes first.
        Balm.getHooks().getPersistentData(pet);
    }
}
