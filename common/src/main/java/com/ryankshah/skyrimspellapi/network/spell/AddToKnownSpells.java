package com.ryankshah.skyrimspellapi.network.spell;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.registry.AdvancementTriggersRegistry;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.api.Dispatcher;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public record AddToKnownSpells(ResourceKey<Spell> spell)
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "addtoknownspells");

    public static final StreamCodec<RegistryFriendlyByteBuf, AddToKnownSpells> CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(SpellRegistry.SPELLS_KEY),
            AddToKnownSpells::spell,
            AddToKnownSpells::new
    );

    public AddToKnownSpells(final FriendlyByteBuf buffer) {
        this(buffer.readResourceKey(SpellRegistry.SPELLS_KEY));
    }

    public static void handle(PacketContext<AddToKnownSpells> context) {
        if(context.side() == Side.CLIENT)
            handleClient(context);
        else
            handleServer(context);
    }

    public static void handleServer(PacketContext<AddToKnownSpells> context) {
        ServerPlayer player = context.sender();
        SpellData spellData = SpellData.get(player);
        AddToKnownSpells data = context.message();
        spellData.addNewSpell(SpellRegistry.SPELLS_REGISTRY.get(data.spell).orElseThrow().value());

        AdvancementTriggersRegistry.LEARN_SPELL.get().trigger(player, Holder.direct(SpellRegistry.SPELLS_REGISTRY.get(data.spell).orElseThrow().value()));

        final AddToKnownSpells sendToClient = new AddToKnownSpells(data.spell);
        Dispatcher.sendToClient(sendToClient, player);
//        PacketDistributor.PLAYER.with(player).send(sendToClient);
    }

    public static void handleClient(PacketContext<AddToKnownSpells> context) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Player player = Minecraft.getInstance().player;
            SpellData spellData = SpellData.get(player);
            spellData.addNewSpell(SpellRegistry.SPELLS_REGISTRY.get(context.message().spell()).orElseThrow().value());
        });
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(TYPE);
    }
}

