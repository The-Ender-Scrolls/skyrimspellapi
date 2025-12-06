package com.ryankshah.skyrimspellapi.platform;

import com.ryankshah.skyrimspellapi.SpellApiFabric;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public SpellData getSpellData(Player player) {
        return player == null ? new SpellData() : ((AttachmentTarget)player).getAttachedOrCreate(SpellApiFabric.SPELL_DATA, SpellData::new);
    }

    @Override
    public void setSpellData(Player player, SpellData characterData) {
        ((AttachmentTarget)player).setAttached(SpellApiFabric.SPELL_DATA, characterData);
    }
}
