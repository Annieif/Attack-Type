package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 无视抗性效果。
 * amplifier 0/1/2 对应 20%/40%/60% 伤害无视抗性。
 */
public class IgnoreResistanceEffect extends StatusEffect {
    public IgnoreResistanceEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0xFF4444);
    }

    public static float getIgnoreRatio(int amplifier) {
        return (amplifier + 1) * 0.2f;
    }
}