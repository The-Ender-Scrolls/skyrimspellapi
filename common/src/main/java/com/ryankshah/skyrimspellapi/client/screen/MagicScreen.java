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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.glfwGetKeyName;

public class MagicScreen extends Screen
{
    protected static final ResourceLocation OVERLAY_ICONS = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/overlay_icons.png");
    private static final int PADDING = 7;

    // Mouse interaction variables
    private boolean isDragging = false;

    // UI bounds for mouse interaction (adjusted for right-side panels)
    private int spellTypePanelLeft;
    private int spellTypePanelRight;
    private int spellPanelLeft;
    private int spellPanelRight;

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

        // Initialize spellsAndTypes map
        this.spellsAndTypes = new HashMap<>();
        spellsAndTypes.put(Spell.SpellType.ALL, new ArrayList<>());

        // Process all non-empty spells
        for(Spell spell : knownSpells) {
            if(spell instanceof EmptySpell)
                continue;  // Changed from 'return' to 'continue'

            if(spellsAndTypes.containsKey(spell.getType()))
                spellsAndTypes.get(spell.getType()).add(spell);
            else {
                ArrayList<Spell> temp = new ArrayList<>();
                temp.add(spell);
                spellsAndTypes.put(spell.getType(), temp);
            }
            spellsAndTypes.get(Spell.SpellType.ALL).add(spell);
        }

        // Get spell data (moved outside the loop)
        this.spellData = SpellData.get(Minecraft.getInstance().player);

        // Sort the spells by type
        spellsAndTypes = spellsAndTypes.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().getTypeID()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Initialize spellTypes list - THIS MUST HAPPEN BEFORE LINE 80
        this.spellTypes = new ArrayList<>(spellsAndTypes.keySet());

        // Safety check: ensure we have at least one spell type
        if (this.spellTypes.isEmpty()) {
            this.spellTypes.add(Spell.SpellType.ALL);
            this.currentSpellType = 0;
            this.currentSpell = 0;
            this.spellsListForCurrentSpellType = new ArrayList<>();
            this.currentSpellObject = SpellRegistry.EMPTY_SPELL.get();
        } else {
            this.currentSpellType = 0;
            this.currentSpell = 0;
            this.currentSpellTypeObject = spellTypes.get(currentSpellType);
            this.spellsListForCurrentSpellType = spellsAndTypes.get(currentSpellTypeObject);

            // Safety check for spell list
            if (!this.spellsListForCurrentSpellType.isEmpty()) {
                this.currentSpellObject = spellsListForCurrentSpellType.get(currentSpell);
            } else {
                this.currentSpellObject = SpellRegistry.EMPTY_SPELL.get();
            }
        }

        this.currentTick = 0;
        this.lastTick = 0;
        this.currentSpellFrame = 0;
    }

    @Override
    protected void init() {
        super.init();
        // Initialize panel bounds based on screen width
        spellTypePanelLeft = this.width - 90;
        spellTypePanelRight = this.width - 10;
        spellPanelLeft = this.width - 190;
        spellPanelRight = this.width - 110;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Matrix3x2fStack poseStack = graphics.pose();
        Minecraft mc = this.minecraft;
        Window window = mc.getWindow();
        int scaledWidth = window.getGuiScaledWidth();
        int scaledHeight = window.getGuiScaledHeight();

//        System.out.println("SpellTypeChosen: " + spellTypeChosen + " - CurrentSpell: " + currentSpellObject.getName());

        poseStack.pushMatrix();
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderTransparentBackground(graphics);

        if(this.spellTypeChosen) {
            graphics.fillGradient(this.width - 90, 0, this.width - 10, this.height, 0xAA000000, 0xAA000000);
            graphics.fillGradient(this.width - 88, 0, this.width - 87, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 13, 0, this.width - 12, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 190, 0, this.width - 110, this.height, 0xAA000000, 0xAA555555);
            graphics.fillGradient(this.width - 188, 0, this.width - 187, this.height, 0xFF6E6B64, 0xFF6E6B64);
            graphics.fillGradient(this.width - 113, 0, this.width - 112, this.height, 0xFF6E6B64, 0xFF6E6B64);
        } else {
            graphics.fillGradient(this.width - 90, 0, this.width - 10, this.height, 0xAA000000, 0xAA555555);
            graphics.fillGradient(this.width - 88, 0, this.width - 87, this.height, 0xFF6E6B64, 0xFF6E6B64);
            graphics.fillGradient(this.width - 13, 0, this.width - 12, this.height, 0xFF6E6B64, 0xFF6E6B64);
            graphics.fillGradient(this.width - 190, 0, this.width - 110, this.height, 0xAA000000, 0xAA000000);
            graphics.fillGradient(this.width - 188, 0, this.width - 187, this.height, 0xFF5D5A51, 0xFF5D5A51);
            graphics.fillGradient(this.width - 113, 0, this.width - 112, this.height, 0xFF5D5A51, 0xFF5D5A51);
        }
        graphics.fillGradient(0, this.height * 3 / 4 + 20, this.width, this.height, 0xAA000000, 0xAA000000);
        graphics.fillGradient(0, this.height * 3 / 4 + 22, this.width, this.height * 3 / 4 + 23, 0xFF5D5A51, 0xFF5D5A51);

        // Draw "buttons" for keys for selecting spells
        drawGradientRect(graphics, poseStack, 17, this.height - 29, 32, this.height - 14, 0xAA000000, 0xAA000000, 0xFF5D5A51);
        drawGradientRect(graphics, poseStack, 37, this.height - 29, 52, this.height - 14, 0xAA000000, 0xAA000000, 0xFF5D5A51);
        graphics.drawCenteredString(font, glfwGetKeyName(KeysRegistry.SPELL_SLOT_1_KEY.get().getDefaultKey().getValue(), 0).toUpperCase(), 25, this.height - 25, 0xFF00FF00);
        graphics.drawCenteredString(font, glfwGetKeyName(KeysRegistry.SPELL_SLOT_2_KEY.get().getDefaultKey().getValue(), 0).toUpperCase(), 45, this.height - 25, 0xFF00FFFF);
        graphics.drawCenteredString(font, "Equip", 70, this.height - 25, 0xFFFFFFFF);

        int MIN_Y = 30;
        int MAX_Y = height / 2 + 14 * 6 - 10;

        int i;
        for(i = 0; i < spellTypes.size(); i++) {
            int y = this.height / 2 + 14 * i - this.currentSpellType * font.lineHeight;
            if(y <= MIN_Y || y >= MAX_Y)
                continue;
            String spellTypeName = spellTypes.get(i).toString();
            if (spellTypeName.length() >= 12)
                spellTypeName = spellTypeName.substring(0, 10) + "..";

            int color = i == this.currentSpellType ? 0xFFFFFFFF : 0xFFC0C0C0;
            // Highlight on hover
            if (isMouseOverSpellType(i, y)) {
                color = 0xFFFFFFAA; // Light yellow highlight
            }

            graphics.drawString(font, spellTypeName, this.width - 20 - font.width(spellTypeName), y, color);
        }

        // Get ISpell.SpellType
        currentSpellTypeObject = (Spell.SpellType) spellTypes.get(currentSpellType);
        // Get player's known spells for chosen spell type
        spellsListForCurrentSpellType = spellsAndTypes.get(currentSpellTypeObject);

        for(int j = 0; j < spellsListForCurrentSpellType.size(); j++) {
            Spell spell = spellsListForCurrentSpellType.get(j);
            String displayName = spell.getName();

            AtomicInteger color = new AtomicInteger(0xFFC0C0C0);

            int finalJ = j;
            Spell selectedSpell1 = spellData.getSelectedSpell1();
            Spell selectedSpell2 = spellData.getSelectedSpell2();
            if(!(selectedSpell1 instanceof EmptySpell) && selectedSpell1 == spell) color.set(0xFF00FF00);
            else if(!(selectedSpell2 instanceof EmptySpell) && selectedSpell2 == spell) color.set(0xFF00FFFF);
            else if(finalJ == this.currentSpell) {
                color.set(0xFFFFFFFF);
            }

            int y = this.height / 2 + 14 * j - this.currentSpell * font.lineHeight;

            // Highlight on hover (only if not already a selected spell color)
            if (isMouseOverSpell(j, y) && color.get() == 0xFFC0C0C0) {
                color.set(0xFFFFFFAA); // Light yellow highlight
            } else if (isMouseOverSpell(j, y) && finalJ == this.currentSpell) {
                color.set(0xFFFFFFAA); // Light yellow highlight for current spell on hover
            }

            if (j == this.currentSpell && this.spellTypeChosen) {
                this.currentSpellObject = spell;
                poseStack.pushMatrix();
                drawSpellInformation(graphics, poseStack, currentSpellObject, this.width, this.height, partialTick);
                poseStack.popMatrix();
            }

            if(y <= MIN_Y || y >= MAX_Y)
                continue;

            if (displayName.length() >= 12)
                displayName = displayName.substring(0, 10) + "..";

            graphics.drawString(font, displayName, this.width - 183, y, color.get());
        }

        poseStack.popMatrix();
    }

    private void drawGradientRect(GuiGraphics graphics, Matrix3x2fStack matrixStack, int startX, int startY, int endX, int endY, int colorStart, int colorEnd, int borderColor) {
        matrixStack.pushMatrix();
        // Draw background
        graphics.fillGradient(startX, startY, endX, endY, colorStart, colorEnd);
        // Draw borders
        graphics.fill(startX, startY, endX, startY+1, borderColor); // top
        graphics.fill(startX, endY-1, endX, endY, borderColor); // bottom
        graphics.fill(startX, startY+1, startX+1, endY-1, borderColor); // left
        graphics.fill(endX-1, startY+1, endX, endY-1, borderColor); // right
        matrixStack.popMatrix();
    }


    private void drawSpellInformation(GuiGraphics graphics, Matrix3x2fStack matrixStack, Spell spell, int width, int height, float partialTicks) {
        int leftBorder = 0;
        int rightBorder = this.width - 190;
        int centerX = (leftBorder + rightBorder) / 2;
        int infoWidth = 160; // Adjust this value as needed for the width of your spell info box
        int infoLeft = centerX - (infoWidth / 2);
        int infoRight = centerX + (infoWidth / 2);

        // Background
        drawGradientRect(graphics, matrixStack, infoLeft, (height) / 2 - 20, infoRight, (height) / 2 + 60, 0xAA000000, 0xAA000000, 0xFF6E6B64);

        // Line under spell name
        graphics.fillGradient(infoLeft + 10, (this.height) / 2, infoRight - 10, (this.height) / 2 + 1, 0xFF6E6B64, 0xFF6E6B64);

        // Spell name
        graphics.drawCenteredString(font, spell.getName(), centerX, (this.height) / 2 - 10, 0xFFFFFFFF);

        // Spell description
        for(int i = 1; i < spell.getDescription().size()+1; i++) {
            graphics.drawCenteredString(font, spell.getDescription().get(i-1), centerX, (this.height) / 2 + (8 * i), 0xFFFFFFFF);
        }

        // Spell details
        if(spell.getType() != Spell.SpellType.SHOUT) {
            graphics.drawString(font, "Cost: " + (int) spell.getCost(), infoLeft + 10, (this.height) / 2 + 34, 0xFFFFFFFF);
            graphics.drawString(font, "Difficulty: " + StringUtils.capitalize(StringUtils.lowerCase(spell.getDifficulty().toString())), infoLeft + 10, (this.height) / 2 + 44, 0xFFFFFFFF);
        } else {
            graphics.drawString(font, "Cooldown: " + (int)spell.getCooldown(), infoLeft + 10, (this.height) / 2 + 40, 0xFFFFFFFF);
        }

        // Draw spell animation using the new GuiGraphics.blit method
        ResourceLocation spellTexture = spell.getDisplayAnimation();

        if(spellTexture == null)
            return;

        matrixStack.pushMatrix();
        if(spell.getType() == Spell.SpellType.SHOUT || spell.getType() == Spell.SpellType.POWERS) {
            currentSpellFrame = (int)(lastTick + (currentTick - lastTick) * partialTicks) / 16;
            int uOffset = 0, vOffset = 16 * (currentSpellFrame % 7);
            float scaleFactor = 4.0f;
            float xPos = centerX - 32;
            float yPos = (this.height / 2) - 94;

            matrixStack.pushMatrix();
            matrixStack.translate(xPos, yPos);
            matrixStack.scale(scaleFactor, scaleFactor);

            // Use GuiGraphics.blit with blend - GUI_TEXTURED pipeline supports alpha blending
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    spellTexture,
                    0, 0,           // x, y position (after translation)
                    uOffset, vOffset, // u, v texture coordinates
                    16, 16,         // width, height to draw
                    16, 112         // texture width, texture height
            );

            matrixStack.popMatrix();
        } else {
            currentSpellFrame = (int)(lastTick + (currentTick - lastTick) * partialTicks) / 64;
            int uOffset = 64 * (currentSpellFrame % 4), vOffset = 0;
            float xPos = centerX - 32; // Center the 64x64 image

            // Use GuiGraphics.blit for non-shout spells
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    spellTexture,
                    (int)xPos, (this.height / 2) - 94, // x, y position
                    uOffset, vOffset,                   // u, v texture coordinates
                    64, 64,                             // width, height to draw
                    256, 64                             // texture width, texture height
            );
        }
        matrixStack.popMatrix();
    }

    @Override
    public void tick() {
        if(!spellTypeChosen)
            currentTick = 0;
        else {
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
        // Navigate down (S or Down Arrow)
        if (event.key() == GLFW.GLFW_KEY_DOWN || event.key() == GLFW.GLFW_KEY_S) {
            if (!this.spellTypeChosen) {
                if (this.currentSpellType < this.spellTypes.size() - 1) {
                    ++this.currentSpellType;
                } else {
                    this.currentSpellType = this.spellTypes.size() - 1;
                }
            } else {
                if (this.currentSpell < this.spellsListForCurrentSpellType.size() - 1) {
                    ++this.currentSpell;
                } else {
                    this.currentSpell = this.spellsListForCurrentSpellType.size() - 1;
                }
            }
        }

        // Navigate up (W or Up Arrow)
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_W) {
            if (!this.spellTypeChosen) {
                if (this.currentSpellType > 0) {
                    --this.currentSpellType;
                } else {
                    this.currentSpellType = 0;
                }
            } else {
                if (this.currentSpell > 0) {
                    --this.currentSpell;
                } else {
                    this.currentSpell = 0;
                }
            }
        }

        // Navigate left (A or Left Arrow) - go back to spell types
        if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_A) {
            if (this.spellTypeChosen) {
                this.spellTypeChosen = false;
                this.currentSpell = 0;
            }
        }

        // Navigate right (D or Right Arrow) - select spell type, enter spells list
        if (event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_D) {
            if (!this.spellTypeChosen) {
                this.spellTypeChosen = true;
                this.currentSpell = 0;
            }
        }

        // Spell slot 1 key
        if (KeysRegistry.SPELL_SLOT_1_KEY.get().matches(event)) {
            handleSpellSlot1Selection();
        }

        // Spell slot 2 key
        if (KeysRegistry.SPELL_SLOT_2_KEY.get().matches(event)) {
            handleSpellSlot2Selection();
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() == 0) { // Left click
            int MIN_Y = 30;
            int MAX_Y = height / 2 + 14 * 6 - 10;

            // Check if clicking on spell types (right panel)
            if (event.x() >= spellTypePanelLeft && event.x() <= spellTypePanelRight) {
                int clickedSpellType = getSpellTypeAtMouse((int) event.y());
                if (clickedSpellType != -1) {
                    if (clickedSpellType != this.currentSpellType) {
                        this.currentSpellType = clickedSpellType;
                        this.currentSpell = 0;
                        this.spellTypeChosen = false;
                    } else {
                        // Clicking on current spell type enters the spells list
                        this.spellTypeChosen = true;
                        this.currentSpell = 0;
                    }
                    return true;
                }
            }

            // Check if clicking on spells (left of spell types panel)
            if (event.x() >= spellPanelLeft && event.x() <= spellPanelRight) {
                int clickedSpell = getSpellAtMouse((int) event.y());
                if (clickedSpell != -1) {
                    this.currentSpell = clickedSpell;
                    this.spellTypeChosen = true;
                    return true;
                }
            }

            // Check if clicking on equip button area (slot 1)
            if (event.x() >= 17 && event.x() <= 32 && event.y() >= this.height - 29 && event.y() <= this.height - 14) {
                if (this.spellTypeChosen) {
                    handleSpellSlot1Selection();
                    return true;
                }
            }

            // Check if clicking on equip button area (slot 2)
            if (event.x() >= 37 && event.x() <= 52 && event.y() >= this.height - 29 && event.y() <= this.height - 14) {
                if (this.spellTypeChosen) {
                    handleSpellSlot2Selection();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (event.button() == 0) {
            this.isDragging = true;
            return true;
        }
        return super.mouseDragged(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(event);
    }

    // Helper methods for mouse interaction
    private boolean isMouseOverSpellType(int spellTypeIndex, int spellTypeY) {
        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        return mouseX >= spellTypePanelLeft && mouseX <= spellTypePanelRight &&
                mouseY >= spellTypeY && mouseY <= spellTypeY + font.lineHeight;
    }

    private boolean isMouseOverSpell(int spellIndex, int spellY) {
        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();

        return mouseX >= spellPanelLeft && mouseX <= spellPanelRight &&
                mouseY >= spellY && mouseY <= spellY + font.lineHeight;
    }

    private int getSpellTypeAtMouse(int mouseY) {
        int MIN_Y = 30;
        int MAX_Y = height / 2 + 14 * 6 - 10;

        for (int i = 0; i < this.spellTypes.size(); i++) {
            int y = this.height / 2 + 14 * i - this.currentSpellType * font.lineHeight;
            if (y <= MIN_Y || y >= MAX_Y) continue;

            if (mouseY >= y && mouseY <= y + font.lineHeight) {
                return i;
            }
        }
        return -1;
    }

    private int getSpellAtMouse(int mouseY) {
        if (this.spellsListForCurrentSpellType == null) return -1;

        int MIN_Y = 30;
        int MAX_Y = height / 2 + 14 * 6 - 10;

        for (int i = 0; i < this.spellsListForCurrentSpellType.size(); i++) {
            int y = this.height / 2 + 14 * i - this.currentSpell * font.lineHeight;
            if (y <= MIN_Y || y >= MAX_Y) continue;

            if (mouseY >= y && mouseY <= y + font.lineHeight) {
                return i;
            }
        }
        return -1;
    }

    // Spell slot selection helper methods
    private void handleSpellSlot1Selection() {
        if (!this.spellTypeChosen || this.currentSpellObject == null) return;

        Spell selectedSpell1 = spellData.getSelectedSpell1();
        Spell selectedSpell2 = spellData.getSelectedSpell2();

        if (!selectedSpell1.equals(currentSpellObject)) {
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

//        System.out.println(spellData.toString());
    }

    private void handleSpellSlot2Selection() {
        if (!this.spellTypeChosen || this.currentSpellObject == null) return;

        Spell selectedSpell1 = spellData.getSelectedSpell1();
        Spell selectedSpell2 = spellData.getSelectedSpell2();

        if (!selectedSpell2.equals(currentSpellObject)) {
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

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return true;
    }

    @Override
    public void removed() {
        super.removed();
    }
}