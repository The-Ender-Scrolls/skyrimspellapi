package com.ryankshah.skyrimspellapi;

import com.ryankshah.skyrimspellapi.network.Networking;
import com.ryankshah.skyrimspellapi.registry.AdvancementTriggersRegistry;
import com.ryankshah.skyrimspellapi.registry.KeysRegistry;

public class SpellAPI
{
    public static void init() {
        KeysRegistry.init();
        AdvancementTriggersRegistry.init();
        Networking.load();
    }
}