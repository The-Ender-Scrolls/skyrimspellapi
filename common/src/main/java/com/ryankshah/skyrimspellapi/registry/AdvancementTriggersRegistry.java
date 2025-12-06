package com.ryankshah.skyrimspellapi.registry;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.advancement.LearnSpellTrigger;
import com.ryankshah.skyrimspellapi.registration.RegistrationProvider;
import com.ryankshah.skyrimspellapi.registration.RegistryObject;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;

public class AdvancementTriggersRegistry
{
    public static void init() {}

    public static final RegistrationProvider<CriterionTrigger<?>> TRIGGERS = RegistrationProvider.get(Registries.TRIGGER_TYPE, Constants.MOD_ID);
    public static final RegistryObject<CriterionTrigger<?>, LearnSpellTrigger> LEARN_SPELL = TRIGGERS.register("learn_spell", LearnSpellTrigger::new);
}
