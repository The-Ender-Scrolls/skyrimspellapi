package com.ryankshah.skyrimspellapi.event;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.network.spell.UpdateShoutCooldown;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.api.Dispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;

@EventBusSubscriber(modid = Constants.MOD_ID)
public class PlayerEvents
{
    public static boolean flag = false;
    private static int magickaTickCounter = 0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player playerEntity = event.getEntity();
        if (!playerEntity.isAlive() || playerEntity == null)
            return;

        SpellData character = SpellData.get(playerEntity);
        if(playerEntity.level().isClientSide()) {
            if (!character.getSpellsOnCooldown().isEmpty()) {
                for (Map.Entry<Spell, Float> entry : character.getSpellsOnCooldown().entrySet()) {
                    if (entry.getValue() <= 0f) {
                        final UpdateShoutCooldown updateShoutCooldown = new UpdateShoutCooldown(SpellRegistry.SPELLS_REGISTRY.getResourceKey(entry.getKey()).get(), 0f);
                        Dispatcher.sendToServer(updateShoutCooldown);
//                            PacketDistributor.SERVER.noArg().send(updateShoutCooldown);
                    }
                    if (entry.getValue() > 0f) {
                        float cooldown = character.getSpellCooldown(entry.getKey());
                        final UpdateShoutCooldown updateShoutCooldown = new UpdateShoutCooldown(SpellRegistry.SPELLS_REGISTRY.getResourceKey(entry.getKey()).get(), cooldown - 0.05f);
                        Dispatcher.sendToServer(updateShoutCooldown);
//                            PacketDistributor.SERVER.noArg().send(updateShoutCooldown);
                    }
                }
            }
        }

        if (character.getMagicka() < character.getMaxMagicka()) {
            if (playerEntity.tickCount % 20 == 0) {
                // If in combat, regenerate 1% of max magicka, else 3%
                if (playerEntity.getCombatTracker().lastDamageTime > 20 * 3)
                    character.setMagicka(character.getMagicka() + ((0.01f * character.getMaxMagicka()) * character.getMagickaRegenModifier()));
                else
                    character.setMagicka(character.getMagicka() + ((0.03f * character.getMaxMagicka()) * character.getMagickaRegenModifier()));
            }
        }

        if(Minecraft.getInstance().getConnection() == null)
            return;
    }
}