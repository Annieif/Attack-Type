package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import org.attack_type.api.SinType;

/**
 * 罪孽/物理分类状态效果。
 * <p>
 * 统一处理 6 种分类（强化/守护/提升/弱化/易损/降低）对 7 罪孽 + 物理的伤害修正。
 * amplifier 0/1/2 对应 Lv1/2/3。
 */
public class SinCategoryEffect extends StatusEffect {
    private final SinType sinType;
    private final EffectCategory category;

    public SinCategoryEffect(SinType sinType, EffectCategory category) {
        super(category.isPositive() ? StatusEffectCategory.BENEFICIAL : StatusEffectCategory.HARMFUL,
                sinType != null ? sinType.getColor() : 0xCCCCCC);
        this.sinType = sinType;
        this.category = category;
    }

    public SinType getSinType() {
        return sinType;
    }

    public EffectCategory getEffectCategory() {
        return category;
    }

    public float getMagnitude(int amplifier) {
        int level = amplifier + 1;
        if (category.isFlatDamage()) {
            return level;
        }
        return level * 0.1f;
    }

    public boolean isPhysical() {
        return sinType == null;
    }
}