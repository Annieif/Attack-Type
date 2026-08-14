package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import org.attack_type.api.SinType;

/**
 * 爆发效果 — 罪孽专用。
 * <p>
 * amplifier 0-4 对应 Lv1-5：
 * <ul>
 *   <li>罪孽抗性 +0.3 × level</li>
 *   <li>物理抗性 -0.5 × level</li>
 *   <li>反伤：受到攻击时对攻击者造成 3 × level 点对应罪孽伤害</li>
 *   <li>受伤转换：所有受到的伤害改为对应罪孽属性</li>
 * </ul>
 */
public class BurstEffect extends StatusEffect {
    private final SinType sinType;

    public BurstEffect(SinType sinType) {
        super(StatusEffectCategory.NEUTRAL, sinType.getColor());
        this.sinType = sinType;
    }

    public SinType getSinType() {
        return sinType;
    }

    public float getSinResistanceBonus(int amplifier) {
        return 0.3f * (amplifier + 1);
    }

    public float getPhysicalResistancePenalty(int amplifier) {
        return -0.5f * (amplifier + 1);
    }

    public float getThornsDamage(int amplifier) {
        return 3.0f * (amplifier + 1);
    }
}