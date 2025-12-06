package com.ryankshah.skyrimspellapi.network.spell;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.networking.data.PacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record CastSpell(ResourceKey<Spell> spell)
{
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "castspell");

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSpell> CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(SpellRegistry.SPELLS_KEY),
            CastSpell::spell,
            CastSpell::new
    );

    public CastSpell(final RegistryFriendlyByteBuf buffer) {
        this(buffer.readResourceKey(SpellRegistry.SPELLS_KEY));
    }

    public static void handle(final PacketContext<CastSpell> context) {
        if (context.sender() instanceof ServerPlayer serverPlayer) {
            Spell spellInstance = SpellRegistry.SPELLS_REGISTRY.get(context.message().spell()).orElseThrow().value();
            spellInstance.setCaster(serverPlayer);
            spellInstance.cast();
        }
    }

    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(TYPE);
    }
}
