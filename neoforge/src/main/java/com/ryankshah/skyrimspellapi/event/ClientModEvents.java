package com.ryankshah.skyrimspellapi.event;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.registry.KeysRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT) // GAME or MOD event bus??
public class ClientModEvents
{
    @SubscribeEvent
    public static void onKeyBinds(RegisterKeyMappingsEvent event) {
        event.register(KeysRegistry.MENU_KEY.get());
        event.register(KeysRegistry.SPELL_SLOT_1_KEY.get());
        event.register(KeysRegistry.SPELL_SLOT_2_KEY.get());
    }
}