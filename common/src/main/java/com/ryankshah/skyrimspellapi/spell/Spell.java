package com.ryankshah.skyrimspellapi.spell;

import com.ryankshah.skyrimspellapi.Constants;
import com.ryankshah.skyrimspellapi.data.SpellData;
import com.ryankshah.skyrimspellapi.network.spell.UpdateShoutCooldown;
import commonnetwork.api.Dispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class Spell
{
    private int identifier;
    private Player caster;
    private String loc;
    private ResourceLocation location;

    public Spell() {
        this(-1, "");
    }

    public Spell(int identifier, String location) {
        this.identifier = identifier;
        this.loc = location;
        this.location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, this.loc);
    }

    /**
     * Get the ID of the spell
     *
     * @return spell ID
     */
    public int getID() {
        return identifier;
    }

    public Spell getSpell() {
        return this;
    }

    /**
     * Get the name of the spell
     *
     * @return name
     */
    public String getName() {
        return "";
    }

    public boolean isContinuous() { return false; }

    /**
     * Get the spell's description
     * @return description
     */
    public List<String> getDescription() {
        return new ArrayList<>();
    }

    public void setCaster(Player playerEntity) {
        this.caster = playerEntity;
    }

    /**
     * Get the player entity who casted the spell
     * @return {@link Player}
     */
    public Player getCaster() {
        return this.caster;
    }

    /**
     * Get the display animation for the spell (within the magic gui)
     * @return display animation
     */
    public ResourceLocation getDisplayAnimation() { return ResourceLocation.withDefaultNamespace(""); }

    /**
     * Get the in-game overlay icon for the spell
     * @return spell icon
     */
    public ResourceLocation getIcon() { return ResourceLocation.withDefaultNamespace(""); }

    /**
     * Get the magicka cost of the spell
     *
     * @return magicka cost
     */
    public float getCost() {
        return 0;
    }

    /**
     * Get the cooldown (seconds) of the spell
     *
     * @return cooldown
     */
    public float getCooldown() {
        return 0;
    }

    /**
     * Get the type of spell {@link SpellType}
     *
     * @return {@link SpellType}
     */
    public SpellType getType() {
        return SpellType.DESTRUCTION;
    }

    /**
     * Gets the sound to play before the spell is cast
     * @return {@link SoundEvent}
     */
    public SoundEvent getSound() {
        return null;
    }

    /**
     * Returns the length of the sound
     * @return sound length
     */
    public float getSoundLength() { return 0f; }

    /**
     * If true, spell can interrupt effects - i.e. EtherealEffect
     * @return can interrupt
     */
    public boolean canInterrupt() { return true; }

    public boolean hasStages() {
        return getNumStages() > 0;
    }

    public float getStageChargeTime() {
        return 0.25f;
    }

    public float getChargeTime() {
        return 2.2f; //getCastReference() == CastReference.CHARGE ? getStageChargeTime() * getNumStages() : 0f;
    }

    public String getShoutName() {
        return "";
    }

    public int getNumStages() {
        return 0;
    }

    /**
     * Returns whether the spell is a single cast or continually cast
     * - For example, the spell may drain mana while the key is held down
     * @return is continuous?
     */
    public CastReference getCastReference() {
        return CastReference.INSTANT;
    }

    /**
     * Get the spell difficulty {@link SpellDifficulty}
     *
     * @return {@link SpellDifficulty}
     */
    public SpellDifficulty getDifficulty() {
        return SpellDifficulty.NA;
    }

    /**
     * Determines whether the spell can be cast
     * @return {@link CastResult}
     */
    protected CastResult canCast() {
        if(getCaster().isUnderWater())
            return CastResult.UNDERWATER;

        SpellData spellData = SpellData.get(getCaster());

//        if(getCastReference() == CastReference.CHARGE) // TODO: Implement check for charging reference etc.

        return spellData.getSpellCooldown(this) <= 0f ? CastResult.SUCCESS : CastResult.COOLDOWN;
    }

    /**
     * Get the base xp added to the SpellData for casting this spell
     * @return base xp
     */
    public int getBaseXp() {
        return 0;
    }

    public void cast() {
        if(canCast() == CastResult.SUCCESS) {
            onCast();
        } else if(canCast() == CastResult.FAIL) {
            getCaster().displayClientMessage(Component.literal("Failed to Cast Spell/Shout!"), false);
        } else if(canCast() == CastResult.UNDERWATER) {
            getCaster().displayClientMessage(Component.literal("You cannot cast spells underwater!"), false);
        } else {
            getCaster().displayClientMessage(Component.literal("" + (canCast() == CastResult.MAGICKA ? "Not enough magicka!" : "This shout is still on cooldown!")), false);
        }
    }

    /**
     * Specifies what happens on spell cast
     */
    public void onCast() {
        if(!caster.isCreative()) {
            final UpdateShoutCooldown updateShoutCooldown = new UpdateShoutCooldown(SpellRegistry.SPELLS_REGISTRY.getResourceKey(this).get(), getCooldown());
            Dispatcher.sendToServer(updateShoutCooldown);
        }
        // player var was null, check it
        if(this.getSound() != null)
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), getSound(), SoundSource.PLAYERS, 1f, 1f);
    }

    public void onSpellCancel() {
    }

    public enum CastReference {
        INSTANT(0),
        HOLD(1),
        CHARGE(2);

        private final int value;

        CastReference(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public boolean requiresHold() {
            return this == HOLD;
        }
    }

    public enum SpellType {
        ALL(-1, "ALL"),
        ALTERATION(0, "ALTERATION"),
        DESTRUCTION(1, "DESTRUCTION"),
        RESTORATION(2, "RESTORATION"),
        SHOUT(3, "SHOUTS"),
        POWERS(4, "POWERS"),
        ILLUSION(5, "ILLUSION"),
        CONJURATION(6, "CONJURATION");

        private int typeID;
        private String displayName;

        SpellType(int typeID, String displayName) {
            this.typeID = typeID;
            this.displayName = displayName;
        }

        public int getTypeID() {
            return this.typeID;
        }


        @Override
        public String toString() {
            return this.displayName;
        }
    }
    public enum SpellDifficulty {
        NA(0),
        NOVICE(1),
        APPRENTICE(2),
        ADEPT(3),
        EXPERT(4),
        MASTER(5);

        private int difficulty;

        SpellDifficulty(int difficulty) {
            this.difficulty = difficulty;
        }

        public int getDifficulty() {
            return this.difficulty;
        }
    }

    public enum CastResult {
        SUCCESS(0),
        COOLDOWN(1),
        MAGICKA(2),
        FAIL(3),
        UNDERWATER(4),
        CHARGING(5);

        private int id;

        CastResult(int id) { this.id = id; }

        public int getId() { return this.id; }
    }

//    @Override
//    public int hashCode() {
//        return getID();
//    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Spell && getID() == ((Spell) obj).getID();
    }
}