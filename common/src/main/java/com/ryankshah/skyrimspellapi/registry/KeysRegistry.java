package com.ryankshah.skyrimspellapi.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.client.screen.MagicScreen;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.network.spell.CastSpell;
import com.ryankshah.skyrimspellapi.platform.Services;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.api.Dispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class KeysRegistry
{
    public static void init() {}

    public static final Lazy<KeyMapping> MENU_KEY = Lazy.lazy(() -> new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_menu", // Will be localized using this translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KeyMapping.Category.GAMEPLAY
    ));
    public static final Lazy<KeyMapping> SPELL_SLOT_1_KEY = Lazy.lazy(() -> new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_spell_1", // Will be localized using this translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.GAMEPLAY
    ));
    public static final Lazy<KeyMapping> SPELL_SLOT_2_KEY = Lazy.lazy(() -> new KeyMapping(
            "key." + Constants.MOD_ID + ".toggle_spell_2", // Will be localized using this translation key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KeyMapping.Category.GAMEPLAY
    ));

    public static void onKeyInput(int key, int scanCode, int action, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        if (mc.screen != null)
            return;

        while (MENU_KEY.get().consumeClick()) {
            mc.setScreen(new MagicScreen(Services.PLATFORM.getSpellData(mc.player).getKnownSpells()));
            return;
        }
        while (SPELL_SLOT_1_KEY.get().consumeClick()) { // TODO: Check if `isDown` for continuous cast?
            SpellData character = SpellData.get(mc.player);
            Spell spell = character.getSelectedSpell1();
            if (spell != null && spell.getID() != SpellRegistry.EMPTY_SPELL.get().getID()) {
                final CastSpell castSpell = new CastSpell(SpellRegistry.SPELLS_REGISTRY.getResourceKey(spell).get());
                Dispatcher.sendToServer(castSpell);
//                    PacketDistributor.SERVER.noArg().send(castSpell);
            } else
                mc.player.displayClientMessage(Component.translatable("skyrimspellapi.spell.noselect"), false);
            return;
        }
        while (SPELL_SLOT_2_KEY.get().consumeClick()) {
            SpellData character = SpellData.get(mc.player);
            Spell spell = character.getSelectedSpell2();
            if (spell != null && spell.getID() != SpellRegistry.EMPTY_SPELL.get().getID()) {
                final CastSpell castSpell = new CastSpell(SpellRegistry.SPELLS_REGISTRY.getResourceKey(spell).get());
                Dispatcher.sendToServer(castSpell);
//                    PacketDistributor.SERVER.noArg().send(castSpell);
            } else
                mc.player.displayClientMessage(Component.translatable("skyrimspellapi.spell.noselect"), false);
            return;
        }
    }

//    private static boolean didPress(int key, int scanCode, int action, KeyMapping keyBinding) {
//        return action == GLFW.GLFW_PRESS && isKey(key,scanCode, keyBinding);
//    }
//    private static boolean isKey(int key, int scanCode, KeyMapping keyBinding) {
//        return keyBinding.matches(key, scanCode);
//    }
}