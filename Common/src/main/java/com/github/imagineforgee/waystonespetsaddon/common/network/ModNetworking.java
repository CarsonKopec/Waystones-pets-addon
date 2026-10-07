package com.github.imagineforgee.waystonespetsaddon.common.network;

import com.github.imagineforgee.waystonespetsaddon.common.Constants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public class ModNetworking {
    public static void initialize() {
        BalmNetworking networking = Balm.getNetworking();

        networking.registerServerboundPacket(id("request_pet_list"), RequestPetListMessage.class,
                RequestPetListMessage::encode, RequestPetListMessage::decode, RequestPetListMessage::handle);
        networking.registerClientboundPacket(id("pet_list"), PetListMessage.class,
                PetListMessage::encode, PetListMessage::decode, PetListMessage::handle);
        networking.registerServerboundPacket(id("set_pet_follows"), SetPetFollowsMessage.class,
                SetPetFollowsMessage::encode, SetPetFollowsMessage::decode, SetPetFollowsMessage::handle);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Constants.MOD_ID, path);
    }
}
