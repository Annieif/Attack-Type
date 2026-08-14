package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 碎片扣除效果。
 * amplifier 0/1/2 对应 每 5s 扣除 1/2/3 碎片。
 */
public class FragmentDrainEffect extends StatusEffect {
    public FragmentDrainEffect() {
        super(StatusEffectCategory.HARMFUL, 0x884444);
    }

    public static int getDrainAmount(int amplifier) {
        return amplifier + 1;
    }
}