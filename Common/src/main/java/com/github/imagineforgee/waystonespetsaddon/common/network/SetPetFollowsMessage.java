package com.github.imagineforgee.waystonespetsaddon.common.network;

import com.github.imagineforgee.waystonespetsaddon.common.PetFollowData;
import com.github.imagineforgee.waystonespetsaddon.common.PetTeleportHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.UUID;

/** Sent by the client when the player toggles whether a pet follows them. */
public record SetPetFollowsMessage(UUID petId, boolean follows) implements CustomPacketPayload {
    public static final Type<SetPetFollowsMessage> TYPE = ModNetworking.type("set_pet_follows");

    @Override
    public Type<SetPetFollowsMessage> type() {
        return TYPE;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SetPetFollowsMessage message) {
        buf.writeUUID(message.petId);
        buf.writeBoolean(message.follows);
    }

    public static SetPetFollowsMessage decode(RegistryFriendlyByteBuf buf) {
        return new SetPetFollowsMessage(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(ServerPlayer player, SetPetFollowsMessage message) {
        Entity entity = player.level().getEntity(message.petId);
        if (entity instanceof TamableAnimal pet && pet.isOwnedBy(player)) {
            PetFollowData.setFollows(pet, message.follows);
            // Pets told to stay are sat down when their owner teleports, so following again means getting back up.
            if (message.follows) {
                PetTeleportHandler.standUp(pet);
            }
        }
    }
}
