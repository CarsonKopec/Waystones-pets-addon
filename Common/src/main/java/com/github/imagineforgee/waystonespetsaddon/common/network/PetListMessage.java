package com.github.imagineforgee.waystonespetsaddon.common.network;

import com.github.imagineforgee.waystonespetsaddon.common.client.gui.PetSelectionScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The server's answer to {@link RequestPetListMessage}: the player's pets in teleport range, closest first,
 * plus the server config the screen needs to explain what will happen.
 */
public record PetListMessage(List<Pet> pets, double teleportRadius, int maxPetsToTeleport,
                             boolean teleportSittingPets) implements CustomPacketPayload {
    public static final Type<PetListMessage> TYPE = ModNetworking.type("pet_list");

    /** {@code entityId} lets the client find its own copy of the pet to draw its model. */
    public record Pet(UUID id, int entityId, Component name, Component typeName, boolean hasCustomName,
                      float distance, boolean sitting, boolean follows) {
    }

    @Override
    public Type<PetListMessage> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buf, PetListMessage message) {
        buf.writeVarInt(message.pets.size());
        for (Pet pet : message.pets) {
            buf.writeUUID(pet.id);
            buf.writeVarInt(pet.entityId);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, pet.name);
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, pet.typeName);
            buf.writeBoolean(pet.hasCustomName);
            buf.writeFloat(pet.distance);
            buf.writeBoolean(pet.sitting);
            buf.writeBoolean(pet.follows);
        }
        buf.writeDouble(message.teleportRadius);
        buf.writeVarInt(message.maxPetsToTeleport);
        buf.writeBoolean(message.teleportSittingPets);
    }

    public static PetListMessage decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Pet> pets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pets.add(new Pet(
                    buf.readUUID(),
                    buf.readVarInt(),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readBoolean()));
        }
        return new PetListMessage(pets, buf.readDouble(), buf.readVarInt(), buf.readBoolean());
    }

    // Only ever runs on the client, so referencing the screen here is safe on dedicated servers.
    public static void handle(Player player, PetListMessage message) {
        PetSelectionScreen.handlePetList(message);
    }
}
