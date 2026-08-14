package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 无消耗效果。
 * amplifier 0/1/2 对应 30s/1min/2min。
 */
public class NoCostEffect extends StatusEffect {
    public NoCostEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFFFF88);
    }
}