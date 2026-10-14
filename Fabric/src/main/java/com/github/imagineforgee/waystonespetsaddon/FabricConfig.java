package com.github.imagineforgee.waystonespetsaddon;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportConfig;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;
import net.blay09.mods.balm.api.config.Config;
import net.blay09.mods.balm.api.event.ConfigReloadedEvent;
import net.minecraft.util.Mth;

/**
 * Fabric config, stored through Balm (as Waystones does) rather than Forge Config API Port, which has no build for
 * 1.20.3. Same settings, defaults and limits as the Forge and NeoForge configs.
 */
@Config(Constants.MOD_ID)
public class FabricConfig implements BalmConfigData {
    @Comment("If true, sitting pets will be teleported.")
    public boolean teleportSittingPets = false;

    @Comment("Force pets to stand up before teleporting, even if they are sitting. Only applies if teleportSittingPets is true.")
    public boolean forceUnsitPets = false;

    @Comment("The radius (in blocks) around the player to detect tamed pets. Range: 1.0 ~ 100.0")
    public double teleportRadius = 10.0;

    @Comment("Maximum number of pets to teleport at once. Range: 1 ~ 1000")
    public int maxPetsToTeleport = 5;

    @Comment("Randomize the offset of teleported pets to avoid stacking.")
    public boolean randomizeTeleportOffset = true;

    @Comment("Delay (in ticks) before pets are teleported. 0 = instant. Range: 0 ~ 200")
    public int teleportDelayTicks = 0;

    public static void initialize() {
        // Only the server uses these settings, and the pet screen gets them from the server, so no client sync.
        Balm.getConfig().registerConfig(FabricConfig.class, null);
        Balm.getEvents().onEvent(ConfigReloadedEvent.class, event -> applyToCommon());
        applyToCommon();
    }

    // Balm doesn't enforce ranges, so clamp to the same limits the Forge config spec enforces.
    private static void applyToCommon() {
        FabricConfig config = Balm.getConfig().getActive(FabricConfig.class);
        PetTeleportConfig.ConfigValues values = new PetTeleportConfig.ConfigValues();
        values.teleportSittingPets = config.teleportSittingPets;
        values.forceUnsitPets = config.forceUnsitPets;
        values.teleportRadius = Mth.clamp(config.teleportRadius, 1.0, 100.0);
        values.maxPetsToTeleport = Mth.clamp(config.maxPetsToTeleport, 1, 1000);
        values.randomizeTeleportOffset = config.randomizeTeleportOffset;
        values.teleportDelayTicks = Mth.clamp(config.teleportDelayTicks, 0, 200);
        PetTeleportConfig.values = values;
    }
}
