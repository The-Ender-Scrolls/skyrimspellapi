package com.ryankshah.skyrimspellapi;

import com.ryankshah.skyrimspellapi.client.screen.MagicScreen;
import com.ryankshah.skyrimspellapi.client.screen.SkyrimGuiOverlay;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.network.spell.CastSpell;
import com.ryankshah.skyrimspellapi.network.spell.ConsumeMagicka;
import com.ryankshah.skyrimspellapi.network.spell.UpdateShoutCooldown;
import com.ryankshah.skyrimspellapi.registry.KeysRegistry;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.api.Dispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class SpellApiFabricClient implements ClientModInitializer
{
    protected SkyrimGuiOverlay.SkyrimSpells spells = new SkyrimGuiOverlay.SkyrimSpells();

    private static int spell1TicksHeld = 0;
    private static int spell2TicksHeld = 0;
    private static final int TICK_INTERVAL = 20; // Magicka consumption and damage every 1 second (20 ticks)
    private static boolean isChargingShout = false;

    private static boolean spell1KeyWasDown = false;
    private static boolean spell2KeyWasDown = false;

    private static long lastCastTime1 = 0;
    private static long lastCastTime2 = 0;

    private static boolean canCastSpell1 = true;
    private static boolean canCastSpell2 = true;

    private Minecraft mc = Minecraft.getInstance();

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(KeysRegistry.MENU_KEY.get());
        KeyBindingHelper.registerKeyBinding(KeysRegistry.SPELL_SLOT_1_KEY.get());
        KeyBindingHelper.registerKeyBinding(KeysRegistry.SPELL_SLOT_2_KEY.get());

        HudRenderCallback.EVENT.register((guiGraphics, deltaTime) -> {
            spells.render(guiGraphics, deltaTime);
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::handleClientTicks);
    }

    public void handleClientTicks(Minecraft client) {
        if (client.player == null || !client.player.isAlive())
            return;

        SpellData spellData = SpellData.get(client.player);
        if(client.player.level().isClientSide()) {
            if (!spellData.getSpellsOnCooldown().isEmpty()) {
                for (Map.Entry<Spell, Float> entry : spellData.getSpellsOnCooldown().entrySet()) {
                    if (entry.getValue() <= 0f) {
                        final UpdateShoutCooldown updateShoutCooldown = new UpdateShoutCooldown(SpellRegistry.SPELLS_REGISTRY.getResourceKey(entry.getKey()).get(), 0f);
                        Dispatcher.sendToServer(updateShoutCooldown);
                    }
                    if (entry.getValue() > 0f) {
                        float cooldown = spellData.getSpellCooldown(entry.getKey());
                        final UpdateShoutCooldown updateShoutCooldown = new UpdateShoutCooldown(SpellRegistry.SPELLS_REGISTRY.getResourceKey(entry.getKey()).get(), cooldown - 0.05f);
                        Dispatcher.sendToServer(updateShoutCooldown);
                    }
                }
            }
        }

        while (KeysRegistry.MENU_KEY.get().consumeClick()) {
            mc.setScreen(new MagicScreen(spellData.getKnownSpells()));
            return;
        }

        if (mc.screen != null) return;

        handleSpellCasting(spellData, KeysRegistry.SPELL_SLOT_1_KEY.get(), 1);
        handleSpellCasting(spellData, KeysRegistry.SPELL_SLOT_2_KEY.get(), 2);
    }

    private static void handleSpellCasting(SpellData spellData, KeyMapping key, int spellSlot) {
        boolean keyIsDown = key.isDown();
        boolean keyWasDown = spellSlot == 1 ? spell1KeyWasDown : spell2KeyWasDown;
        Spell spell = spellSlot == 1 ? spellData.getSelectedSpell1() : spellData.getSelectedSpell2();
        boolean canCastSpell = spellSlot == 1 ? canCastSpell1 : canCastSpell2;

        if (keyIsDown && !keyWasDown) {
            processSpellStart(spell, spellData, spellSlot);
        } else if (keyWasDown && !keyIsDown) {
            processSpellFinish(spell, spellData, spellSlot);
        } else if (keyIsDown && canCastSpell) {
            processSpellContinue(spell, spellData, spellSlot);
        }

        if (spellSlot == 1) {
            spell1KeyWasDown = keyIsDown;
        } else {
            spell2KeyWasDown = keyIsDown;
        }
    }

    private static void processSpellStart(Spell spell, SpellData spellData, int spellSlot) {
        if (spell != null && spell.getID() != SpellRegistry.EMPTY_SPELL.get().getID()) {
            if (spell.getType() == Spell.SpellType.SHOUT) {
                startChargingShout(spell, spellData, spellSlot);
            } else if(spell.getType() == Spell.SpellType.POWERS) {
                castSpell(spell, spellData, true);
            } else if (hasSufficientMagicka(spell, spellData)) {
                castSpell(spell, spellData, true);
                consumeMagicka(spell, spellData);
            } else {
                displayInsufficientMagickaMessage();
                setCanCastSpell(spellSlot, false);
            }
        } else {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("skyrimspellapi.spell.noselect"), false);
        }
    }

    private static void processSpellContinue(Spell spell, SpellData spellData, int spellSlot) {
        int ticksHeld = spellSlot == 1 ? spell1TicksHeld : spell2TicksHeld;
        ticksHeld++;

        if (spell.isContinuous() && hasSufficientMagicka(spell, spellData)) {
            castSpell(spell, spellData, false);

            if (ticksHeld % TICK_INTERVAL == 0) {
                consumeMagicka(spell, spellData);
            }
        } else if (spell.getType() == Spell.SpellType.SHOUT) {
            updateShoutCharge(spell, spellData, spellSlot);
        } else if (!hasSufficientMagicka(spell, spellData)) {
            displayInsufficientMagickaMessage();
            setCanCastSpell(spellSlot, false);
        }

        if (spellSlot == 1) {
            spell1TicksHeld = ticksHeld;
        } else {
            spell2TicksHeld = ticksHeld;
        }
    }

    private static void processSpellFinish(Spell spell, SpellData spellData, int spellSlot) {
        if (spell.getType() == Spell.SpellType.SHOUT) {
            stopChargingShout(spell, spellData, spellSlot);
        }
        spell.onSpellCancel();
        resetTicksHeld(spellSlot);
        setCanCastSpell(spellSlot, true);
    }

    private static void startChargingShout(Spell spell, SpellData spellData, int spellSlot) {
        if (spellData.getSpellCooldown(spell) > 0f) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("skyrimspellapi.shout.cooldown"), false);
            return;
        }

        if (!isChargingShout) {
            isChargingShout = true;
            SkyrimGuiOverlay.setShowShoutBar(true);
            SkyrimGuiOverlay.setShoutLocation(spellSlot);
        }

        resetTicksHeld(spellSlot); // Reset charge time at the start
    }

    private static void updateShoutCharge(Spell spell, SpellData spellData, int spellSlot) {
        int ticksHeld = spellSlot == 1 ? spell1TicksHeld : spell2TicksHeld;
        ticksHeld++;

        // Calculate charge-up progress (0.0 - 1.0)
        float chargeupProgress = Math.min(ticksHeld / (spell.getChargeTime() * 20.0f), 1.0f);
        SkyrimGuiOverlay.setShoutChargeProgress(chargeupProgress);

        if (chargeupProgress >= 1.0f) {
            // Cast shout once charge is complete
            hideShoutBar();
            castSpell(spell, spellData, false);
            processSpellFinish(spell, spellData, spellSlot); // Reset after casting
        }

        // Store updated ticksHeld
        if (spellSlot == 1) {
            spell1TicksHeld = ticksHeld;
        } else {
            spell2TicksHeld = ticksHeld;
        }
    }

    private static void stopChargingShout(Spell spell, SpellData spellData, int spellSlot) {
        hideShoutBar();
        resetTicksHeld(spellSlot);
    }

    private static void castSpell(Spell spell, SpellData spellData, boolean isInitialCast) {
        final CastSpell castSpell = new CastSpell(SpellRegistry.SPELLS_REGISTRY.getResourceKey(spell).get());
        Dispatcher.sendToServer(castSpell);

        if (isInitialCast || !spell.isContinuous()) {
            consumeMagicka(spell, spellData);
        }
    }

    private static void consumeMagicka(Spell spell, SpellData spellData) {
        float cost = spell.getCost();
        if (spellData.getMagicka() >= cost) {
            final ConsumeMagicka consumeMagicka = new ConsumeMagicka(cost);
            Dispatcher.sendToServer(consumeMagicka);
        }
    }

    private static boolean hasSufficientMagicka(Spell spell, SpellData spellData) {
        return spellData.getMagicka() >= spell.getCost();
    }

    private static void displayInsufficientMagickaMessage() {
        Minecraft.getInstance().player.displayClientMessage(Component.translatable("skyrimspellapi.spell.no_magicka"), false);
    }

    private static void setCanCastSpell(int spellSlot, boolean canCast) {
        if (spellSlot == 1) {
            canCastSpell1 = canCast;
        } else {
            canCastSpell2 = canCast;
        }
    }

    private static void hideShoutBar() {
        SkyrimGuiOverlay.setShoutChargeProgress(0.0f);
        SkyrimGuiOverlay.setShowShoutBar(false);
        isChargingShout = false;
    }

    private static void resetTicksHeld(int spellSlot) {
        if (spellSlot == 1) {
            spell1TicksHeld = 0;
        } else {
            spell2TicksHeld = 0;
        }
    }
}