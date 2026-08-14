package org.attack_type.effect;

/**
 * 状态效果分类。
 */
public enum EffectCategory {
    STRENGTHEN,
    GUARD,
    BOOST,
    WEAKEN,
    VULNERABLE,
    REDUCE;

    public boolean isPositive() {
        return this == STRENGTHEN || this == GUARD || this == BOOST;
    }

    public boolean isDamageDealt() {
        return this == STRENGTHEN || this == WEAKEN;
    }

    public boolean isDamageTaken() {
        return this == GUARD || this == VULNERABLE;
    }

    public boolean isFlatDamage() {
        return this == BOOST || this == REDUCE;
    }
}