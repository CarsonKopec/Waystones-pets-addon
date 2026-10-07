package com.github.imagineforgee.waystonespetsaddon;

import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final Config CONFIG;
    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");
        CONFIG = new Config(BUILDER);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static class Config {
        public final ModConfigSpec.IntValue maxPetsToTeleport;
        public final ModConfigSpec.DoubleValue teleportRadius;
        public final ModConfigSpec.BooleanValue teleportSittingPets;
        public final ModConfigSpec.BooleanValue forceUnsitPets;
        public final ModConfigSpec.BooleanValue randomizeTeleportOffset;
        public final ModConfigSpec.IntValue teleportDelayTicks;

        public Config(ModConfigSpec.Builder builder) {
            teleportSittingPets = builder
                    .comment("If true, sitting pets will be teleported.")
                    .define("teleportSittingPets", false);

            forceUnsitPets = builder
                    .comment("Force pets to stand up before teleporting, even if they are sitting. Only applies if teleportSittingPets is true.")
                    .define("forceUnsitPets", false);

            teleportRadius = builder
                    .comment("The radius (in blocks) around the player to detect tamed pets.")
                    .defineInRange("teleportRadius", 10.0, 1.0, 100.0);

            maxPetsToTeleport = builder
                    .comment("Maximum number of pets to teleport at once.")
                    .defineInRange("maxPetsToTeleport", 5, 1, 1000);

            randomizeTeleportOffset = builder
                    .comment("Randomize the offset of teleported pets to avoid stacking.")
                    .define("randomizeTeleportOffset", true);

            teleportDelayTicks = builder
                    .comment("Delay (in ticks) before pets are teleported. 0 = instant.")
                    .defineInRange("teleportDelayTicks", 0, 0, 200);
        }

        public void applyToCommon() {
            PetTeleportConfig.values = new PetTeleportConfig.ConfigValues();
            PetTeleportConfig.values.maxPetsToTeleport = maxPetsToTeleport.get();
            PetTeleportConfig.values.teleportRadius = teleportRadius.get();
            PetTeleportConfig.values.teleportSittingPets = teleportSittingPets.get();
            PetTeleportConfig.values.forceUnsitPets = forceUnsitPets.get();
            PetTeleportConfig.values.randomizeTeleportOffset = randomizeTeleportOffset.get();
            PetTeleportConfig.values.teleportDelayTicks = teleportDelayTicks.get();
        }
    }
}
