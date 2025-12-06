package com.ryankshah.skyrimspellapi.platform;

import com.ryankshah.skyrimspellapi.SpellApiNeo;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.platform.services.IPlatformHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public SpellData getSpellData(Player player) {
        return player == null ? new SpellData() : player.getData(SpellApiNeo.SPELL_DATA);
    }

    @Override
    public void setSpellData(Player player, SpellData characterData) {
        player.setData(SpellApiNeo.SPELL_DATA, characterData);
    }
}