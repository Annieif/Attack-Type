package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 消耗增加效果。
 * amplifier 0/1/2 对应 消耗 +20%/+40%/+60%。
 */
public class CostIncreaseEffect extends StatusEffect {
    public CostIncreaseEffect() {
        super(StatusEffectCategory.HARMFUL, 0x884400);
    }

    public static float getCostMultiplier(int amplifier) {
        return 1.0f + (amplifier + 1) * 0.2f;
    }
}