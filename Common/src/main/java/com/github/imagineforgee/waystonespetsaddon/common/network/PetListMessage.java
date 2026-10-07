package com.github.imagineforgee.waystonespetsaddon.common.network;

import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetSelectionScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * The server's answer to {@link RequestPetListMessage}: the player's pets in teleport range, closest first,
 * plus the server config the screen needs to explain what will happen.
 */
public record PetListMessage(List<Pet> pets, double teleportRadius, int maxPetsToTeleport, boolean teleportSittingPets) {

    /** {@code entityId} lets the client find its own copy of the pet to draw its model. */
    public record Pet(UUID id, int entityId, Component name, Component typeName, boolean hasCustomName,
                      float distance, boolean sitting, boolean follows) {
    }

    public static void encode(PetListMessage message, FriendlyByteBuf buf) {
        buf.writeCollection(message.pets, (b, pet) -> {
            b.writeUUID(pet.id);
            b.writeVarInt(pet.entityId);
            b.writeComponent(pet.name);
            b.writeComponent(pet.typeName);
            b.writeBoolean(pet.hasCustomName);
            b.writeFloat(pet.distance);
            b.writeBoolean(pet.sitting);
            b.writeBoolean(pet.follows);
        });
        buf.writeDouble(message.teleportRadius);
        buf.writeVarInt(message.maxPetsToTeleport);
        buf.writeBoolean(message.teleportSittingPets);
    }

    public static PetListMessage decode(FriendlyByteBuf buf) {
        List<Pet> pets = buf.readList(b -> new Pet(
                b.readUUID(),
                b.readVarInt(),
                b.readComponent(),
                b.readComponent(),
                b.readBoolean(),
                b.readFloat(),
                b.readBoolean(),
                b.readBoolean()));
        return new PetListMessage(pets, buf.readDouble(), buf.readVarInt(), buf.readBoolean());
    }

    // Only ever runs on the client, so referencing the screen here is safe on dedicated servers.
    public static void handle(Player player, PetListMessage message) {
        PetSelectionScreen.handlePetList(message);
    }
}
