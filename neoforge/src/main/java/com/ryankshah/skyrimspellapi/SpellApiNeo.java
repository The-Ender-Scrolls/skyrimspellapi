package com.ryankshah.skyrimspellapi;


import com.ryankshah.skyrimspellapi.data.SpellData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public class SpellApiNeo
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);

    public static final Supplier<AttachmentType<SpellData>> SPELL_DATA = ATTACHMENT_TYPES.register(
            "spelldata", () -> AttachmentType.builder(SpellData::new).serialize(SpellData.CODEC).copyOnDeath().build());

    public SpellApiNeo(IEventBus eventBus) {
        SpellAPI.init();

        ATTACHMENT_TYPES.register(eventBus);
    }
}