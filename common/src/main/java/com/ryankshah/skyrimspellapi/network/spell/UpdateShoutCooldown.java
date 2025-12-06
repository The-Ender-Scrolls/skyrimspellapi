package com.ryankshah.skyrimspellapi.network.spell;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.api.Dispatcher;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record UpdateShoutCooldown(ResourceKey<Spell> spell, float cooldown)
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "updateshoutcooldown");

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateShoutCooldown> CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(SpellRegistry.SPELLS_KEY),
            UpdateShoutCooldown::spell,
            ByteBufCodecs.FLOAT,
            UpdateShoutCooldown::cooldown,
            UpdateShoutCooldown::new
    );

    public UpdateShoutCooldown(final FriendlyByteBuf buffer) {
        this(buffer.readResourceKey(SpellRegistry.SPELLS_KEY), buffer.readFloat());
    }

    public static void handle(PacketContext<UpdateShoutCooldown> context) {
        if(context.side() == Side.CLIENT)
            handleClient(context);
        else
            handleServer(context);
    }

    public static void handleServer(PacketContext<UpdateShoutCooldown> context) {
        ServerPlayer serverPlayer = context.sender();
        UpdateShoutCooldown data = context.message();
        SpellData spellData = SpellData.get(serverPlayer);

        spellData.addSpellAndCooldown(SpellRegistry.SPELLS_REGISTRY.get(data.spell).orElseThrow().value(), data.cooldown);

        final UpdateShoutCooldown sendToClient = new UpdateShoutCooldown(data.spell, data.cooldown);
        Dispatcher.sendToClient(sendToClient, serverPlayer);
    }

    public static void handleClient(PacketContext<UpdateShoutCooldown> context) {
        Minecraft minecraft = Minecraft.getInstance();
        UpdateShoutCooldown data = context.message();
        minecraft.execute(() -> {
            Player player = Minecraft.getInstance().player;
            SpellData spellData = SpellData.get(player);
            spellData.addSpellAndCooldown(SpellRegistry.SPELLS_REGISTRY.get(data.spell).orElseThrow().value(), data.cooldown);
        });
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(TYPE);
    }
}
