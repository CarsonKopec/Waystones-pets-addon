package com.github.imagineforgee.waystonespetsaddon.common.handlers;

import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportHandler;
import net.blay09.mods.balm.api.event.BalmEvents;
import net.blay09.mods.waystones.api.IWaystoneTeleportContext;
import net.blay09.mods.waystones.api.TeleportDestination;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ModEventHandlers {
    // Pets are picked in Pre, while the player is still next to them, but only moved in Post. Waystones can still
    // deny a teleport after Pre (XP cost, leash rules, another mod), and then the pets must stay where they are.
    // Weak keys, because a denied teleport never reaches Post to clean up its entry.
    private static final Map<IWaystoneTeleportContext, List<TamableAnimal>> petsByTeleport = new WeakHashMap<>();

    public static void initialize(BalmEvents events) {
        events.onEvent(WaystoneTeleportEvent.Pre.class, event -> {
            IWaystoneTeleportContext context = event.getContext();
            if (!(context.getEntity() instanceof ServerPlayer player)) return;

            // Waystones' own transportPets option has already queued every standing pet nearby, including ones told
            // to stay. This mod decides which pets go, so take them back out of Waystones' hands.
            context.getAdditionalEntities().removeIf(entity -> entity instanceof TamableAnimal pet
                    && player.getUUID().equals(pet.getOwnerUUID()));

            // Leashed pets already travel with the player through Waystones' lead handling.
            List<TamableAnimal> pets = PetTeleportHandler.findNearbyPets(player).stream()
                    .filter(pet -> !context.getLeashedEntities().contains(pet))
                    .toList();
            petsByTeleport.put(context, pets);
        });

        events.onEvent(WaystoneTeleportEvent.Post.class, event -> {
            IWaystoneTeleportContext context = event.getContext();
            List<TamableAnimal> pets = petsByTeleport.remove(context);
            if (pets == null || !(context.getEntity() instanceof ServerPlayer player)) return;

            TeleportDestination destination = context.getDestination();
            PetTeleportHandler.handleTeleport(player, pets, destination.getLocation(), destination.getLevel());
        });
    }
}
