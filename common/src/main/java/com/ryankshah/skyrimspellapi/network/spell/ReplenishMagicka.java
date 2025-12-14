package com.ryankshah.skyrimspellapi.network.spell;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import commonnetwork.api.Dispatcher;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record ReplenishMagicka(float amount)
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "replenishmagicka");

    public static final StreamCodec<FriendlyByteBuf, ReplenishMagicka> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            ReplenishMagicka::amount,
            ReplenishMagicka::new
    );

    public ReplenishMagicka(final FriendlyByteBuf buffer) {
        this(buffer.readFloat());
    }

    public static void handle(PacketContext<ReplenishMagicka> context) {
        if(context.side() == Side.CLIENT)
            handleClient(context);
        else
            handleServer(context);
    }

    public static void handleServer(PacketContext<ReplenishMagicka> context) {
        ServerPlayer player = context.sender();
        SpellData character = SpellData.get(player);

        float newMagicka = character.getMagicka() + character.getMaxMagicka();
        newMagicka = Math.min(newMagicka, character.getMaxMagicka());
        character.setMagicka(newMagicka);

        final ReplenishMagicka sendToClient = new ReplenishMagicka(newMagicka);
        Dispatcher.sendToClient(sendToClient, player);
    }

    public static void handleClient(PacketContext<ReplenishMagicka> context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Player player = Minecraft.getInstance().player;
            SpellData character = SpellData.get(player);
            character.setMagicka(context.message().amount);
        });
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(TYPE);
    }
}