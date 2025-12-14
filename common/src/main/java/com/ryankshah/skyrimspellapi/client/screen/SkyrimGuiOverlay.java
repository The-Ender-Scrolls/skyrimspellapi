package com.ryankshah.skyrimspellapi.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.spell.EmptySpell;
import com.ryankshah.skyrimspellapi.spell.Spell;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

import java.util.Map;

public class SkyrimGuiOverlay
{
    public static final int PLAYER_BAR_MAX_WIDTH = 78;

    private static float shoutChargeProgress = 0.0f;
    private static boolean showShoutBar = false;
    private static int spellLocation = 0;

    public static void setShoutChargeProgress(float progress) {
        shoutChargeProgress = progress;
    }

    public static void setShoutLocation(int loc) {
        spellLocation = loc;
    }

    public static void setShowShoutBar(boolean show) {
        showShoutBar = show;
    }

    public static class SkyrimSpells
    {
        private final ResourceLocation OVERLAY_ICONS = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/overlay_icons.png");
        private static final int SLOT_WIDTH = 22;
        private static final int SLOT_HEIGHT = 22;
        private static final int DOUBLE_SLOT_HEIGHT = 41;
        private static final int ICON_WIDTH = 16;
        private static final int ICON_HEIGHT = 16;

        public void render(GuiGraphics guiGraphics, DeltaTracker partialTick) {
            Minecraft mc = Minecraft.getInstance();
            Window window = mc.getWindow();
            int scaledWidth = window.getGuiScaledWidth();
            int scaledHeight = window.getGuiScaledHeight();

            SpellData character = SpellData.get(mc.player);

            renderMagickaBar(guiGraphics, partialTick);

            Spell selectedSpell1 = character.getSelectedSpell1();
            Spell selectedSpell2 = character.getSelectedSpell2();
            Map<Spell, Float> spellCooldowns = character.getSpellsOnCooldown();

            // Render spell slots
            if(!(selectedSpell1 instanceof EmptySpell) && (selectedSpell2 instanceof EmptySpell || selectedSpell2 == null)) {
                renderSingleSpellSlot(guiGraphics, scaledWidth, scaledHeight, selectedSpell1, spellCooldowns, 0);
            } else if((selectedSpell1 instanceof EmptySpell || selectedSpell1 == null) && !(selectedSpell2 instanceof EmptySpell)) {
                renderSingleSpellSlot(guiGraphics, scaledWidth, scaledHeight, selectedSpell2, spellCooldowns, 20);
            } else if (!(selectedSpell1 instanceof EmptySpell) && !(selectedSpell2 instanceof EmptySpell)) {
                renderDoubleSpellSlot(guiGraphics, scaledWidth, scaledHeight, selectedSpell1, selectedSpell2, spellCooldowns);
            }

            // Render shout bar
            if(showShoutBar) {
                renderShoutBar(guiGraphics, scaledWidth, scaledHeight, character);
            }
        }

        private void renderSingleSpellSlot(GuiGraphics guiGraphics, int scaledWidth, int scaledHeight, Spell spell, Map<Spell, Float> spellCooldowns, int yOffset) {
            int x = scaledWidth - SLOT_WIDTH;
            int y = (scaledHeight / 2) - (DOUBLE_SLOT_HEIGHT / 2) + yOffset;

            // Draw slot background
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_ICONS, x, y, 234, 83, SLOT_WIDTH, SLOT_HEIGHT, 256, 256);

            // Draw spell icon
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, spell.getIcon(), x + 3, y + 3, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);

            // Draw cooldown overlay if applicable
            renderCooldownOverlay(guiGraphics, x + 3, y + 3, spell, spellCooldowns);
        }

        private void renderDoubleSpellSlot(GuiGraphics guiGraphics, int scaledWidth, int scaledHeight, Spell spell1, Spell spell2, Map<Spell, Float> spellCooldowns) {
            int x = scaledWidth - SLOT_WIDTH;
            int y = (scaledHeight / 2) - (DOUBLE_SLOT_HEIGHT / 2);

            // Draw double slot background
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_ICONS, x, y, 234, 106, SLOT_WIDTH, DOUBLE_SLOT_HEIGHT, 256, 256);

            // Draw first spell icon
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, spell1.getIcon(), x + 3, y + 3, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
            renderCooldownOverlay(guiGraphics, x + 3, y + 3, spell1, spellCooldowns);

            // Draw second spell icon
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, spell2.getIcon(), x + 3, y + 23, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
            renderCooldownOverlay(guiGraphics, x + 3, y + 23, spell2, spellCooldowns);
        }

        private void renderCooldownOverlay(GuiGraphics guiGraphics, int x, int y, Spell spell, Map<Spell, Float> spellCooldowns) {
            if(spellCooldowns.containsKey(spell)) {
                float cooldown = spellCooldowns.get(spell);
                if (cooldown > 0) {
                    float maxCooldown = spell.getCooldown();
                    float offset = -1 * Mth.floor(cooldown / maxCooldown * 8);
                    int textureOffset = 230 + ((int)(16 * offset));
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_ICONS, x, y, textureOffset, 148, ICON_WIDTH, ICON_HEIGHT, 256, 256);
                }
            }
        }

        private void renderShoutBar(GuiGraphics guiGraphics, int scaledWidth, int scaledHeight, SpellData character) {
            int barWidth = 144;
            int barHeight = 10;
            int barX = (scaledWidth / 2) - (barWidth / 2);
            int barY = scaledHeight - 70;

            // Draw bar background
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_ICONS, barX, barY, 0, 179, barWidth, barHeight, 256, 256);

            // Draw progress fill
            int fillWidth = (int) (120 * shoutChargeProgress);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_ICONS, barX + 12, barY + 2, 12, 190, fillWidth, 6, 256, 256);

            // Render shout name
            String shoutName = spellLocation == 1 ? character.getSelectedSpell1().getShoutName() : character.getSelectedSpell2().getShoutName();
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, shoutName, scaledWidth / 2, barY - 20, 0xFFFFFFFF);
        }

        public void renderMagickaBar(GuiGraphics guiGraphics, DeltaTracker partialTick) {
            Matrix3x2fStack poseStack = guiGraphics.pose();
            Minecraft mc = Minecraft.getInstance();
            Window window = mc.getWindow();
            int scaledWidth = window.getGuiScaledWidth();
            int scaledHeight = window.getGuiScaledHeight();
            SpellData character = SpellData.get(mc.player);
            float magickaPercentage = character.getMagicka() / character.getMaxMagicka();
            float magickaBarWidth = PLAYER_BAR_MAX_WIDTH * magickaPercentage;

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_ICONS, 20, scaledHeight - 40, 0, 51, 102, 10, 256, 256, 1, 1);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERLAY_ICONS, 32, scaledHeight - 38, 12 + ((PLAYER_BAR_MAX_WIDTH - magickaBarWidth) / 2.0f), 64, (int)(78 * magickaPercentage), 6, 256, 256, 1, 1);
        }
    }
}