package com.ryankshah.skyrimspellapi.spell;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.registration.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class SpellRegistry
{
    /// -- Ticks in mc day (one day cooldown) = 24000 (1200 seconds)
    public static final int DAY_COOLDOWN = 1200;

    public static final ResourceKey<Registry<Spell>> SPELLS_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spells_key"));
    public static final RegistrationProvider<Spell> SPELLS = RegistrationProvider.get(SPELLS_KEY, Constants.MOD_ID);
    public static final Registry<Spell> SPELLS_REGISTRY = SPELLS.registryBuilder().build();

    public static Supplier<Spell> EMPTY_SPELL = SPELLS.register("empty_spell", EmptySpell::new);
}