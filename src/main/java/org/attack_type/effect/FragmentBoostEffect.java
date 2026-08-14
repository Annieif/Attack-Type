package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * 碎片获取提升效果。
 * amplifier 0/1/2 对应 +1/+2/+3 额外碎片。
 */
public class FragmentBoostEffect extends StatusEffect {
    public FragmentBoostEffect() {
        super(StatusEffectCategory.BENEFICIAL, 0x44FF88);
    }

    public static int getExtraFragments(int amplifier) {
        return amplifier + 1;
    }
}