package com.ryankshah.skyrimspellapi;

import com.ryankshah.skyrimspellapi.data.SpellData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class SpellApiFabric implements ModInitializer
{
    public static AttachmentType<SpellData> SPELL_DATA =
            AttachmentRegistryImpl.<SpellData>builder()
                    .initializer(SpellData::new)
                    .persistent(SpellData.CODEC.codec())
                    .buildAndRegister(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "spelldata"));
    @Override
    public void onInitialize() {
        SpellAPI.init();
        initAttachments();
    }

    public static void initAttachments() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SpellData.playerJoinWorld(handler.player);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if(entity instanceof Player player) {
                SpellData.playerDeath(player);
            }
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            SpellData.playerChangedDimension(player);
        });
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            SpellData.playerClone(alive, newPlayer, oldPlayer);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            SpellData.playerClone(alive, newPlayer, oldPlayer);
        });

    }
}
