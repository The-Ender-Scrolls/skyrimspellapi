package com.ryankshah.skyrimspellapi.network.data;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.platform.Services;
import commonnetwork.api.Dispatcher;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record UpdateSpellData(SpellData character)
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "updatespelldata");

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSpellData> CODEC = StreamCodec.composite(
            SpellData.STREAM_CODEC,
            UpdateSpellData::character,
            UpdateSpellData::new
    );

    public UpdateSpellData(final RegistryFriendlyByteBuf buffer) {
        this(buffer.readLenientJsonWithCodec(SpellData.CODEC.codec()));
    }

    public static void handle(PacketContext<UpdateSpellData> context) {
        if(context.side() == Side.CLIENT)
            handleClient(context);
        else
            handleServer(context);
    }

    public static void handleServer(PacketContext<UpdateSpellData> context) {
        ServerPlayer player = context.sender();
        Services.PLATFORM.setSpellData(player, context.message().character);
        final UpdateSpellData sendToClient = new UpdateSpellData(Services.PLATFORM.getSpellData(player));
        Dispatcher.sendToClient(sendToClient, player);
//        PacketDistributor.PLAYER.with(player).send(sendToClient);
    }

    public static void handleClient(PacketContext<UpdateSpellData> context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Services.PLATFORM.setSpellData(minecraft.player, context.message().character);
        });
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(TYPE);
    }
}