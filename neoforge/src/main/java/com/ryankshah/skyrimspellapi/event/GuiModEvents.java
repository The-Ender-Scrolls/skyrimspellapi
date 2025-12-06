package com.ryankshah.skyrimspellapi.event;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.client.SkyrimGuiOverlayNeo;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class GuiModEvents
{
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event){
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "current_spells"), new SkyrimGuiOverlayNeo.SkyrimSpells());
    }
}