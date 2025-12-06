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

public record UpdateSelectedSpell(int position, ResourceKey<Spell> spell)
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "updateselectedspells");

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSelectedSpell> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            UpdateSelectedSpell::position,
            ResourceKey.streamCodec(SpellRegistry.SPELLS_KEY),
            UpdateSelectedSpell::spell,
            UpdateSelectedSpell::new
    );

    public UpdateSelectedSpell(final FriendlyByteBuf buffer) {
        this(buffer.readInt(), buffer.readResourceKey(SpellRegistry.SPELLS_KEY));
    }

    public static void handle(PacketContext<UpdateSelectedSpell> context) {
        if(context.side() == Side.CLIENT)
            handleClient(context);
        else
            handleServer(context);
    }

    public static void handleServer(PacketContext<UpdateSelectedSpell> context) {
        ServerPlayer player = context.sender();
        UpdateSelectedSpell data = context.message();
        SpellData spellData = SpellData.get(player);
        if(!data.spell.equals(SpellRegistry.EMPTY_SPELL)) {
            Spell spell = SpellRegistry.SPELLS_REGISTRY.get(data.spell).orElseThrow().value();
            if(data.position == 1)
                spellData.setSelectedSpell1(spell);
            else if(data.position == 2)
                spellData.setSelectedSpell2(spell);

            final UpdateSelectedSpell sendToClient = new UpdateSelectedSpell(data.position, data.spell);
            Dispatcher.sendToClient(sendToClient, player);
        }
    }

    public static void handleClient(PacketContext<UpdateSelectedSpell> context) {
        Minecraft minecraft = Minecraft.getInstance();
        UpdateSelectedSpell data = context.message();
        minecraft.execute(() -> {
            Player player = Minecraft.getInstance().player;
            SpellData spellData = SpellData.get(player);
            if (!data.spell.equals(SpellRegistry.EMPTY_SPELL)) {
                Spell spell = SpellRegistry.SPELLS_REGISTRY.get(data.spell).orElseThrow().value();
                if (data.position == 1)
                    spellData.setSelectedSpell1(spell);
                else if (data.position == 2)
                    spellData.setSelectedSpell2(spell);
            }
        });
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(TYPE);
    }
}

