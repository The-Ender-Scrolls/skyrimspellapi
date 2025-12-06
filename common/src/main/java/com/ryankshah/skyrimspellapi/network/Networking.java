package com.ryankshah.skyrimspellapi.network;

import com.ryankshah.skyrimspellapi.network.data.UpdateSpellData;
import com.ryankshah.skyrimspellapi.network.spell.AddToKnownSpells;
import com.ryankshah.skyrimspellapi.network.spell.CastSpell;
import com.ryankshah.skyrimspellapi.network.spell.UpdateSelectedSpell;
import com.ryankshah.skyrimspellapi.network.spell.UpdateShoutCooldown;
import commonnetwork.api.Network;

public class Networking
{
    public static void load() {
        Network.registerPacket(AddToKnownSpells.type(), AddToKnownSpells.class,  AddToKnownSpells.CODEC, AddToKnownSpells::handle);
        Network.registerPacket(UpdateSelectedSpell.type(), UpdateSelectedSpell.class,  UpdateSelectedSpell.CODEC, UpdateSelectedSpell::handle);
        Network.registerPacket(UpdateShoutCooldown.type(), UpdateShoutCooldown.class,  UpdateShoutCooldown.CODEC, UpdateShoutCooldown::handle);
        Network.registerPacket(CastSpell.type(), CastSpell.class, CastSpell.CODEC, CastSpell::handle);
        Network.registerPacket(UpdateSpellData.type(), UpdateSpellData.class, UpdateSpellData.CODEC, UpdateSpellData::handle);
    }
}