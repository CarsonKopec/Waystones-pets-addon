package com.github.imagineforgee.waystonespetsaddon.common.network;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class ModNetworking {
    public static void initialize() {
        BalmNetworking networking = Balm.getNetworking();

        networking.registerServerboundPacket(RequestPetListMessage.TYPE, RequestPetListMessage.class,
                RequestPetListMessage::encode, RequestPetListMessage::decode, RequestPetListMessage::handle);
        networking.registerClientboundPacket(PetListMessage.TYPE, PetListMessage.class,
                PetListMessage::encode, PetListMessage::decode, PetListMessage::handle);
        networking.registerServerboundPacket(SetPetFollowsMessage.TYPE, SetPetFollowsMessage.class,
                SetPetFollowsMessage::encode, SetPetFollowsMessage::decode, SetPetFollowsMessage::handle);
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path));
    }
}
