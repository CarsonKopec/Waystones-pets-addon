package com.github.imagineforgee.waystonespetsaddon.common.network;

import com.github.imagineforgee.waystonespetsaddon.common.PetFollowData;
import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportConfig;
import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportHandler;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Sent by the client when the pet selection screen opens. */
public class RequestPetListMessage {
    public static void encode(RequestPetListMessage message, FriendlyByteBuf buf) {
    }

    public static RequestPetListMessage decode(FriendlyByteBuf buf) {
        return new RequestPetListMessage();
    }

    public static void handle(ServerPlayer player, RequestPetListMessage message) {
        List<PetListMessage.Pet> pets = PetTeleportHandler.findNearbyPets(player).stream()
                .map(pet -> new PetListMessage.Pet(
                        pet.getUUID(),
                        pet.getId(),
                        pet.getName(),
                        pet.getType().getDescription(),
                        pet.hasCustomName(),
                        pet.distanceTo(player),
                        pet.isInSittingPose(),
                        PetFollowData.follows(pet)))
                .toList();

        Balm.getNetworking().sendTo(player, new PetListMessage(pets,
                PetTeleportConfig.values.teleportRadius,
                PetTeleportConfig.values.maxPetsToTeleport,
                PetTeleportConfig.values.teleportSittingPets));
    }
}
