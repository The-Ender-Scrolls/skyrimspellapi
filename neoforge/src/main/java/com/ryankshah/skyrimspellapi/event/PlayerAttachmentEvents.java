package com.ryankshah.skyrimspellapi.event;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Constants.MOD_ID) // GAME or MOD event bus??
public class PlayerAttachmentEvents
{
    @SubscribeEvent
    public static void entityJoinLevel(EntityJoinLevelEvent event) {
        if(event.getEntity() instanceof Player player) {
            SpellData.entityJoinLevel(player);
        }
    }

    @SubscribeEvent
    public static void joinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.getEntity() instanceof Player player) {
            SpellData.playerJoinWorld(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.getEntity() instanceof Player player) {
            SpellData.playerChangedDimension(player);
        }
    }

//    @SubscribeEvent
//    public static void track(PlayerEvent.StartTracking event) {
//        if(event.getEntity() instanceof Player player) {
//            SpellData.playerStartTracking(player);
//        }
//    }

    @SubscribeEvent
    public static void playerDeath(LivingDeathEvent event) {
        if(event.getEntity() instanceof Player player) {
            SpellData.playerDeath(player);
        }
    }

    @SubscribeEvent
    public static void playerClone(PlayerEvent.Clone event) {
        SpellData.playerClone(event.isWasDeath(), event.getEntity(), event.getOriginal());
    }
}