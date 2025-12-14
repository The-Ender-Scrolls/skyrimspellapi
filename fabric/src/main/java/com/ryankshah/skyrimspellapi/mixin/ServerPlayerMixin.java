package com.ryankshah.skyrimspellapi.mixin;

import com.mojang.authlib.GameProfile;
import com.ryankshah.skyrimspellapi.data.SpellData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player
{
    @Shadow
    public abstract ServerLevel level();

    @Unique
    private static boolean flag = false;

    private ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void tick(CallbackInfo callbackInfo) {
        SpellData character = SpellData.get(this);

        if (character.getMagicka() < character.getMaxMagicka()) {
            if (tickCount % 20 == 0) {
                // If in combat, regenerate 1% of max magicka, else 3%
                if (getCombatTracker().lastDamageTime > 20 * 3)
                    character.setMagicka(character.getMagicka() + ((0.01f * character.getMaxMagicka()) * character.getMagickaRegenModifier()));
                else
                    character.setMagicka(character.getMagicka() + ((0.03f * character.getMaxMagicka()) * character.getMagickaRegenModifier()));
            }
        }
    }
}