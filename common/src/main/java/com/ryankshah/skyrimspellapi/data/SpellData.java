package com.ryankshah.skyrimspellapi.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ryankshah.skyrimspellapi.network.data.UpdateSpellData;
import com.ryankshah.skyrimspellapi.platform.Services;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.api.Dispatcher;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellData
{
    public static MapCodec<SpellData> CODEC = RecordCodecBuilder.mapCodec(SpellDataInstance -> SpellDataInstance.group(
            SpellRegistry.SPELLS_REGISTRY.byNameCodec().listOf().fieldOf("knownSpells").forGetter(SpellData::getKnownSpells),
            SpellRegistry.SPELLS_REGISTRY.byNameCodec().fieldOf("selectedSpell1").forGetter(SpellData::getSelectedSpell1),
            SpellRegistry.SPELLS_REGISTRY.byNameCodec().fieldOf("selectedSpell2").forGetter(SpellData::getSelectedSpell2),
            Codec.unboundedMap(SpellRegistry.SPELLS_REGISTRY.byNameCodec(), Codec.FLOAT).fieldOf("spellsOnCooldown").forGetter(SpellData::getSpellsOnCooldown)
    ).apply(SpellDataInstance, SpellData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(SpellRegistry.SPELLS_REGISTRY).apply(ByteBufCodecs.list()),
            SpellData::getKnownSpells,
            ByteBufCodecs.idMapper(SpellRegistry.SPELLS_REGISTRY),
            SpellData::getSelectedSpell1,
            ByteBufCodecs.idMapper(SpellRegistry.SPELLS_REGISTRY),
            SpellData::getSelectedSpell2,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.idMapper(SpellRegistry.SPELLS_REGISTRY), ByteBufCodecs.FLOAT),
            SpellData::getSpellsOnCooldown,
            SpellData::new
    );

    private List<Spell> knownSpells;
    private Spell selectedSpell1;
    private Spell selectedSpell2;
    private Map<Spell, Float> spellsOnCooldown;

    public SpellData() {
        this(
                new ArrayList<>(),
                SpellRegistry.EMPTY_SPELL.get(),
                SpellRegistry.EMPTY_SPELL.get(),
                new HashMap<>()
        );
    }

    public SpellData(
            List<Spell> spells, Spell selectedSpell1, Spell selectedSpell2, Map<Spell, Float> cooldowns
            ) {
        this.knownSpells = new ArrayList<>(spells);
        this.selectedSpell1 = selectedSpell1;
        this.selectedSpell2 = selectedSpell2;
        this.spellsOnCooldown = new HashMap<>(cooldowns);
    }

    public void addNewSpell(Spell spell) {
        if(!this.knownSpells.contains(spell)) {
            this.knownSpells.add(spell);
        }
    }

    public Spell getSelectedSpell1() { return selectedSpell1; }
    public Spell getSelectedSpell2() { return selectedSpell2; }

    public void setSelectedSpell1(Spell spell) {
        this.selectedSpell1 = spell;
    }

    public void setSelectedSpell2(Spell spell) {
        this.selectedSpell2 = spell;
    }

    public void addSpellAndCooldown(Spell spell, float cooldown) {
        this.spellsOnCooldown.put(spell, cooldown);
    }

    public float getSpellCooldown(Spell shout) {
        return spellsOnCooldown.getOrDefault(shout, 0f);
    }

    public void setKnownSpells(List<Spell> spells) {
        this.knownSpells = spells;
    }

    public void setSpellsOnCooldown(Map<Spell, Float> cooldowns) {
        this.spellsOnCooldown = cooldowns;
    }

    public Map<Spell, Float> getSpellsOnCooldown() {
        return spellsOnCooldown;
    }

    public List<Spell> getKnownSpells() {
        return knownSpells;
    }

    public static SpellData get(Player player) {
        return Services.PLATFORM.getSpellData(player);
    }

    private void syncToSelf(Player owner) {
        syncTo(owner);
    }

    protected void syncTo(Player player) {
        Dispatcher.sendToClient(new UpdateSpellData(this), (ServerPlayer) player);
    }

//    protected void syncTo(PacketDistributor.PacketTarget target) {
//        target.send(new UpdateSpellData(this));
//    }

    public static void entityJoinLevel(Player player) {
        if (player.level().isClientSide())
            return;
        get(player).syncToSelf(player);
    }

    public static void playerJoinWorld(Player player) {
        if (player.level().isClientSide())
            return;
        get(player).syncToSelf(player);
    }

    public static void playerChangedDimension(Player player) {
        if (player.level().isClientSide())
            return;
        get(player).syncToSelf(player);
    }

    public static void playerStartTracking(Player player) {
        if (player.level().isClientSide())
            return;
        get(player).syncToSelf(player);
    }

    public static void playerDeath(Player player) {
        var newHandler = get(player);

        Services.PLATFORM.setSpellData(player, Services.PLATFORM.getSpellData(player));
        Dispatcher.sendToClient(new UpdateSpellData(newHandler), (ServerPlayer) player); //.sendToPlayer((ServerPlayer) player, new UpdateSpellData(newHandler));
    }

    public static void playerClone(boolean isWasDeath, Player player, Player oldPlayer) {
        if(!isWasDeath)
            return;
//        oldPlayer.revive();
        SpellData oldHandler = SpellData.get(oldPlayer);
        Services.PLATFORM.setSpellData(player, oldHandler);
        SpellData newHandler = SpellData.get(player);
        Dispatcher.sendToClient(new UpdateSpellData(newHandler), (ServerPlayer) player);
    }
}