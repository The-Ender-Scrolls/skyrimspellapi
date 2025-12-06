package com.ryankshah.skyrimspellapi.client.screen;

import com.mojang.blaze3d.platform.Window;
import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.network.spell.UpdateSelectedSpell;
import com.ryankshah.skyrimspellapi.registry.KeysRegistry;
import com.ryankshah.skyrimspellapi.spell.EmptySpell;
import com.ryankshah.skyrimspellapi.spell.Spell;
import com.ryankshah.skyrimspellapi.spell.SpellRegistry;
import commonnetwork.api.Dispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.glfwGetKeyName;

public class MagicScreen extends Screen
{
    protected static final ResourceLocation OVERLAY_ICONS = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/overlay_icons.png");
    private static final int PADDING = 7;

    private Map<Spell.SpellType, ArrayList<Spell>> spellsAndTypes;
    private List<Spell.SpellType> spellTypes;
    private List<Spell> spellsListForCurrentSpellType;
    private boolean spellTypeChosen;
    private int currentSpellType;
    private int currentSpell;
    private Spell currentSpellObject;
    private Spell.SpellType currentSpellTypeObject;
    private SpellData spellData;

    private float currentTick, lastTick;
    private int currentSpellFrame;

    public MagicScreen(List<Spell> knownSpells) {
        super(Component.translatable(Constants.MOD_ID + ".magicgui.title"));
        this.spellsAndTypes = new HashMap<>();
        spellsAndTypes.put(Spell.SpellType.ALL, new ArrayList<>());

        for(Spell spell : knownSpells) {
            if(spellsAndTypes.containsKey(spell.getType()))
                spellsAndTypes.get(spell.getType()).add(spell);
            else {
                ArrayList<Spell> temp = new ArrayList<>();
                temp.add(spell);
                spellsAndTypes.put(spell.getType(), temp);
            }
            spellsAndTypes.get(Spell.SpellType.ALL).add(spell);

            this.spellData = SpellData.get(Minecraft.getInstance().player);
        }

        spellsAndTypes = spellsAndTypes.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getTypeID()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        this.currentSpellType = 0;
        this.currentSpell = 0;
        this.spellTypes = new ArrayList<>(spellsAndTypes.keySet().stream().toList());
        this.currentSpellTypeObject = spellTypes.get(currentSpellType);
        this.spellsListForCurrentSpellType = spellsAndTypes.get(currentSpellTypeObject);
        this.currentSpellObject = spellsListForCurrentSpellType.get(currentSpell);
        this.currentTick = 0;
        this.lastTick = 0;
        this.currentSpellFrame = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = this.minecraft;
        Window window = mc.getWindow();
        int scaledWidth = window.getGuiScaledWidth();
        int scaledHeight = window.getGuiScaledHeight();

        this.renderTransparentBackground(graphics);

        // Draw side panels
        if(this.spellTypeChosen) {
            graphics.fillGradient(this.width - 90, 0, this.width - 10, this.height, 0xAA000000, 0xAA000000);
            graphics.fillGradient(this.width - 88, 0, this.width - 87, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 13, 0, this.width - 12, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 190, 0, this.width - 110, this.height, 0xAA222222, 0xAA222222);
            graphics.fillGradient(this.width - 188, 0, this.width - 187, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 113, 0, this.width - 112, this.height, 0xFF5D5A51, 0xFF5D5A51);
        } else {
            graphics.fillGradient(this.width - 90, 0, this.width - 10, this.height, 0xAA222222, 0xAA222222);
            graphics.fillGradient(this.width - 88, 0, this.width - 87, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 13, 0, this.width - 12, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 190, 0, this.width - 110, this.height, 0xAA000000, 0xAA000000);
            graphics.fillGradient(this.width - 188, 0, this.width - 187, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 113, 0, this.width - 112, this.height, 0xFF5D5A51, 0xFF5D5A51);
        }

        // Bottom panel
        graphics.fillGradient(0, this.height * 3 / 4 + 20, this.width, this.height, 0xAA000000, 0xAA000000);
        graphics.fillGradient(0, this.height * 3 / 4 + 22, this.width, this.height * 3 / 4 + 23, 0xFF5D5A51, 0xFF5D5A51);

        // Draw "buttons" for keys for selecting spells
        drawGradientRect(graphics, 17, this.height - 29, 32, this.height - 14, 0xAA000000, 0xAA000000, 0xFF5D5A51);
        drawGradientRect(graphics, 37, this.height - 29, 52, this.height - 14, 0xAA000000, 0xAA000000, 0xFF5D5A51);
        graphics.drawCenteredString(font, glfwGetKeyName(KeysRegistry.SPELL_SLOT_1_KEY.get().getDefaultKey().getValue(), 0).toUpperCase(), 25, this.height - 25, 0x0000FF00);
        graphics.drawCenteredString(font, glfwGetKeyName(KeysRegistry.SPELL_SLOT_2_KEY.get().getDefaultKey().getValue(), 0).toUpperCase(), 45, this.height - 25, 0x0000FFFF);
        graphics.drawCenteredString(font, "Equip", 70, this.height - 25, 0x00FFFFFF);

        int MIN_Y = 30;
        int MAX_Y = height / 2 + 14 * 6 - 10;

        // Draw spell types
        for(int i = 0; i < spellTypes.size(); i++) {
            int y = this.height / 2 + 14 * i - this.currentSpellType * font.lineHeight;
            if(y <= MIN_Y || y >= MAX_Y)
                continue;
            String spellTypeName = spellTypes.get(i).toString();
            if (spellTypeName.length() >= 12)
                spellTypeName = spellTypeName.substring(0, 10) + "..";
            graphics.drawString(font, spellTypeName, this.width - 20 - font.width(spellTypeName), y, i == this.currentSpellType ? 0x00FFFFFF : 0x00C0C0C0);
        }

        // Get spell type and spells list
        currentSpellTypeObject = spellTypes.get(currentSpellType);
        spellsListForCurrentSpellType = spellsAndTypes.get(currentSpellTypeObject);

        // Draw spells for current type
        for(int j = 0; j < spellsListForCurrentSpellType.size(); j++) {
            Spell spell = spellsListForCurrentSpellType.get(j);
            String displayName = spell.getName();

            AtomicInteger color = new AtomicInteger(0x00C0C0C0);

            Spell selectedSpell1 = spellData.getSelectedSpell1();
            Spell selectedSpell2 = spellData.getSelectedSpell2();
            if(!(selectedSpell1 instanceof EmptySpell) && selectedSpell1 == spell) {
                color.set(0x0000FF00);
            } else if(!(selectedSpell2 instanceof EmptySpell) && selectedSpell2 == spell) {
                color.set(0x0000FFFF);
            } else if(j == this.currentSpell) {
                color.set(0x00FFFFFF);
            }

            if (j == this.currentSpell && this.spellTypeChosen) {
                this.currentSpellObject = spell;
                drawSpellInformation(graphics, currentSpellObject, this.width, this.height, partialTick);
            }

            int y = this.height / 2 + 14 * j - this.currentSpell * font.lineHeight;
            if(y <= MIN_Y || y >= MAX_Y)
                continue;

            if (displayName.length() >= 12)
                displayName = displayName.substring(0, 10) + "..";

            graphics.drawString(font, displayName, this.width - 183, y, color.get());
        }
    }

    private void drawGradientRect(GuiGraphics graphics, int startX, int startY, int endX, int endY, int colorStart, int colorEnd, int borderColor) {
        // Draw background
        graphics.fillGradient(startX, startY, endX, endY, colorStart, colorEnd);
        // Draw borders
        graphics.fill(startX, startY, endX, startY+1, borderColor); // top
        graphics.fill(startX, endY-1, endX, endY, borderColor); // bottom
        graphics.fill(startX, startY+1, startX+1, endY-1, borderColor); // left
        graphics.fill(endX-1, startY+1, endX, endY-1, borderColor); // right
    }

    private void drawSpellInformation(GuiGraphics graphics, Spell spell, int width, int height, float partialTicks) {
        int leftBorder = 0;
        int rightBorder = this.width - 190;
        int centerX = (leftBorder + rightBorder) / 2;
        int infoWidth = 160;
        int infoLeft = centerX - (infoWidth / 2);
        int infoRight = centerX + (infoWidth / 2);

        // Background
        drawGradientRect(graphics, infoLeft, height / 2 - 20, infoRight, height / 2 + 60, 0xAA000000, 0xAA000000, 0xFF6E6B64);

        // Line under spell name
        graphics.fillGradient(infoLeft + 10, height / 2, infoRight - 10, height / 2 + 1, 0xFF6E6B64, 0xFF6E6B64);

        // Spell name
        graphics.drawCenteredString(font, spell.getName(), centerX, height / 2 - 10, 0x00FFFFFF);

        // Spell description
        for(int i = 1; i < spell.getDescription().size() + 1; i++) {
            graphics.drawCenteredString(font, spell.getDescription().get(i - 1), centerX, height / 2 + (8 * i), 0x00FFFFFF);
        }

        // Spell details
        if(spell.getType() != Spell.SpellType.SHOUT) {
            graphics.drawString(font, "Cost: " + (int) spell.getCost(), infoLeft + 10, height / 2 + 34, 0x00FFFFFF);
            graphics.drawString(font, "Difficulty: " + StringUtils.capitalize(StringUtils.lowerCase(spell.getDifficulty().toString())), infoLeft + 10, height / 2 + 44, 0x00FFFFFF);
        } else {
            graphics.drawString(font, "Cooldown: " + (int)spell.getCooldown(), infoLeft + 10, height / 2 + 40, 0x00FFFFFF);
        }

        // Draw spell animation
        ResourceLocation animationTexture = spell.getDisplayAnimation();

        if(spell.getType() == Spell.SpellType.SHOUT || spell.getType() == Spell.SpellType.POWERS) {
            // Shout/Power animation (16x16 frames, 7 frames vertical)
            currentSpellFrame = (int)(lastTick + (currentTick - lastTick) * partialTicks) / 16;
            int uOffset = 0;
            int vOffset = 16 * (currentSpellFrame % 7);
            float scaleFactor = 4.0f;
            int scaledSize = (int)(16 * scaleFactor);
            float xPos = centerX - (scaledSize / 2.0f);
            float yPos = (height / 2) - 94;

            graphics.pose().pushMatrix();
            graphics.pose().translate(xPos, yPos);
            graphics.pose().scale(scaleFactor, scaleFactor);
            graphics.blit(RenderPipelines.GUI_TEXTURED, animationTexture, 0, 0, uOffset, vOffset, 16, 16, 16, 112);
            graphics.pose().popMatrix();
        } else {
            // Regular spell animation (64x64 frames, 4 frames horizontal)
            currentSpellFrame = (int)(lastTick + (currentTick - lastTick) * partialTicks) / 64;
            int uOffset = 64 * (currentSpellFrame % 4);
            int vOffset = 0;
            int xPos = centerX - 32; // Center the 64x64 image
            int yPos = (height / 2) - 94;

            graphics.blit(RenderPipelines.GUI_TEXTURED, animationTexture, xPos, yPos, uOffset, vOffset, 64, 64, 256, 64);
        }
    }

    @Override
    public void tick() {
        if(!spellTypeChosen) {
            currentTick = 0;
        } else {
            currentTick = lastTick;
            lastTick += 32f;
        }
        super.tick();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if(scrollY < 0) {
            if (!this.spellTypeChosen) {
                if (this.currentSpellType < this.spellTypes.size() - 1)
                    ++this.currentSpellType;
            } else {
                if (this.currentSpell < this.spellsListForCurrentSpellType.size() - 1)
                    ++this.currentSpell;
            }
        } else if(scrollY > 0) {
            if (!this.spellTypeChosen) {
                if(this.currentSpellType > 0)
                    --this.currentSpellType;
            } else {
                if (this.currentSpell > 0)
                    --this.currentSpell;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if(event.key() == GLFW.GLFW_KEY_DOWN || event.key() == GLFW.GLFW_KEY_S) {
            if(!this.spellTypeChosen) {
                if(this.currentSpellType < this.spellTypes.size()-1)
                    ++this.currentSpellType;
                else
                    this.currentSpellType = this.spellTypes.size()-1;
            } else {
                if(this.currentSpell >= 0 && this.currentSpell < spellsListForCurrentSpellType.size()-1)
                    this.currentSpell++;
            }
        }

        if(event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_W) {
            if(!this.spellTypeChosen) {
                if(this.currentSpellType > 0)
                    --this.currentSpellType;
                else
                    this.currentSpellType = 0;
            } else {
                if(this.currentSpell > 0 && this.currentSpell < spellsListForCurrentSpellType.size())
                    this.currentSpell--;
            }
        }

        if(event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_A) {
            if(this.spellTypeChosen) {
                this.spellTypeChosen = false;
                this.currentSpell = 0;
            }
        }

        if(event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_D) {
            if(!this.spellTypeChosen) {
                this.spellTypeChosen = true;
                this.currentSpell = 0;
            }
        }

        if(KeysRegistry.SPELL_SLOT_1_KEY.get().matches(event)) {
            Spell selectedSpell1 = spellData.getSelectedSpell1();
            Spell selectedSpell2 = spellData.getSelectedSpell2();

            if(!selectedSpell1.equals(currentSpellObject)) {
                if (!(selectedSpell2.equals(currentSpellObject))) {
                    final UpdateSelectedSpell updatedSpells0 = new UpdateSelectedSpell(1, SpellRegistry.SPELLS_REGISTRY.getResourceKey(currentSpellObject).get());
                    Dispatcher.sendToServer(updatedSpells0);
                } else {
                    final UpdateSelectedSpell updatedSpells0 = new UpdateSelectedSpell(2, SpellRegistry.SPELLS_REGISTRY.getResourceKey(spellData.getSelectedSpell1()).get());
                    Dispatcher.sendToServer(updatedSpells0);
                    final UpdateSelectedSpell updatedSpells1 = new UpdateSelectedSpell(1, SpellRegistry.SPELLS_REGISTRY.getResourceKey(currentSpellObject).get());
                    Dispatcher.sendToServer(updatedSpells1);
                }
            } else {
                final UpdateSelectedSpell updatedSpells0 = new UpdateSelectedSpell(1, SpellRegistry.SPELLS_REGISTRY.getResourceKey(SpellRegistry.EMPTY_SPELL.get()).get());
                Dispatcher.sendToServer(updatedSpells0);
            }

            System.out.println(spellData.toString());
        }

        if(KeysRegistry.SPELL_SLOT_2_KEY.get().matches(event)) {
            Spell selectedSpell1 = spellData.getSelectedSpell1();
            Spell selectedSpell2 = spellData.getSelectedSpell2();

            if(!selectedSpell2.equals(currentSpellObject)) {
                if (!selectedSpell1.equals(currentSpellObject)) {
                    final UpdateSelectedSpell updatedSpells0 = new UpdateSelectedSpell(2, SpellRegistry.SPELLS_REGISTRY.getResourceKey(currentSpellObject).get());
                    Dispatcher.sendToServer(updatedSpells0);
                } else {
                    final UpdateSelectedSpell updatedSpells0 = new UpdateSelectedSpell(1, SpellRegistry.SPELLS_REGISTRY.getResourceKey(spellData.getSelectedSpell2()).get());
                    Dispatcher.sendToServer(updatedSpells0);
                    final UpdateSelectedSpell updatedSpells1 = new UpdateSelectedSpell(2, SpellRegistry.SPELLS_REGISTRY.getResourceKey(currentSpellObject).get());
                    Dispatcher.sendToServer(updatedSpells1);
                }
            } else {
                final UpdateSelectedSpell updatedSpells0 = new UpdateSelectedSpell(2, SpellRegistry.SPELLS_REGISTRY.getResourceKey(SpellRegistry.EMPTY_SPELL.get()).get());
                Dispatcher.sendToServer(updatedSpells0);
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return true;
    }

    @Override
    public void removed() {
        super.removed();
    }
}