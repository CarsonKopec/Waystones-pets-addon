package com.github.imagineforgee.waystonespetsaddon.common.network;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.network.BalmNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ModNetworking {
    public static void initialize() {
        BalmNetworking networking = Balm.networking();

        networking.registerServerboundPacket(RequestPetListMessage.TYPE, RequestPetListMessage.class,
                StreamCodec.of(RequestPetListMessage::encode, RequestPetListMessage::decode), RequestPetListMessage::handle);
        networking.registerClientboundPacket(PetListMessage.TYPE, PetListMessage.class,
                StreamCodec.of(PetListMessage::encode, PetListMessage::decode), PetListMessage::handle);
        networking.registerServerboundPacket(SetPetFollowsMessage.TYPE, SetPetFollowsMessage.class,
                StreamCodec.of(SetPetFollowsMessage::encode, SetPetFollowsMessage::decode), SetPetFollowsMessage::handle);
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, path));
    }
}
