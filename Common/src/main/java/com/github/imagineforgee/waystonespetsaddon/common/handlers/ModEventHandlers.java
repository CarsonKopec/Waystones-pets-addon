package com.github.imagineforgee.waystonespetsaddon.common.handlers;

import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportHandler;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.event.WaystoneTeleportEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ModEventHandlers {
    // Pets are picked in Before, while the player is still next to them, but only moved in After. Waystones can still
    // deny a teleport after Before (XP cost, leash rules, another mod), and then the pets must stay where they are.
    // Weak keys, because a denied teleport never reaches After to clean up its entry.
    private static final Map<WaystoneTeleportContext, List<TamableAnimal>> petsByTeleport = new WeakHashMap<>();

    public static void initialize() {
        WaystoneTeleportEvent.Before.EVENT.register(event -> {
            WaystoneTeleportContext context = event.getContext();
            if (!(context.getEntity() instanceof ServerPlayer player)) return;

            // Waystones' own transportPets option has already queued every standing pet nearby, including ones told
            // to stay. This mod decides which pets go, so take them back out of Waystones' hands.
            context.getAdditionalEntities().removeIf(entity -> entity instanceof TamableAnimal pet
                    && PetTeleportHandler.isOwnedBy(pet, player));

            // Leashed pets already travel with the player through Waystones' lead handling.
            List<TamableAnimal> pets = PetTeleportHandler.findNearbyPets(player).stream()
                    .filter(pet -> !context.getLeashedEntities().contains(pet))
                    .toList();
            petsByTeleport.put(context, pets);
        });

        WaystoneTeleportEvent.After.EVENT.register(event -> {
            WaystoneTeleportContext context = event.getContext();
            List<TamableAnimal> pets = petsByTeleport.remove(context);
            if (pets == null || !(context.getEntity() instanceof ServerPlayer player)) return;

            // After fires once the player has arrived, so where they now stand is the destination.
            PetTeleportHandler.handleTeleport(player, pets, player.position(), player.level());
        });
    }
}
