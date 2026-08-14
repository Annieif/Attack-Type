package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.attack_type.api.SinType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 状态效果注册中心。
 * <p>
 * 注册 48 个分类效果 + 5 个通用效果 + 7 个爆发效果，共 60 个 StatusEffect。
 */
public class ModStatusEffects {

    private static final Map<SinType, Map<EffectCategory, SinCategoryEffect>> SIN_EFFECTS = new EnumMap<>(SinType.class);
    private static final Map<EffectCategory, SinCategoryEffect> PHYS_EFFECTS = new EnumMap<>(EffectCategory.class);
    private static final Map<SinType, BurstEffect> BURST_EFFECTS = new EnumMap<>(SinType.class);

    public static final SinCategoryEffect WRATH_STRENGTHEN = regSin(SinType.WRATH, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect WRATH_GUARD = regSin(SinType.WRATH, EffectCategory.GUARD);
    public static final SinCategoryEffect WRATH_BOOST = regSin(SinType.WRATH, EffectCategory.BOOST);
    public static final SinCategoryEffect WRATH_WEAKEN = regSin(SinType.WRATH, EffectCategory.WEAKEN);
    public static final SinCategoryEffect WRATH_VULNERABLE = regSin(SinType.WRATH, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect WRATH_REDUCE = regSin(SinType.WRATH, EffectCategory.REDUCE);

    public static final SinCategoryEffect LUST_STRENGTHEN = regSin(SinType.LUST, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect LUST_GUARD = regSin(SinType.LUST, EffectCategory.GUARD);
    public static final SinCategoryEffect LUST_BOOST = regSin(SinType.LUST, EffectCategory.BOOST);
    public static final SinCategoryEffect LUST_WEAKEN = regSin(SinType.LUST, EffectCategory.WEAKEN);
    public static final SinCategoryEffect LUST_VULNERABLE = regSin(SinType.LUST, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect LUST_REDUCE = regSin(SinType.LUST, EffectCategory.REDUCE);

    public static final SinCategoryEffect SLOTH_STRENGTHEN = regSin(SinType.SLOTH, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect SLOTH_GUARD = regSin(SinType.SLOTH, EffectCategory.GUARD);
    public static final SinCategoryEffect SLOTH_BOOST = regSin(SinType.SLOTH, EffectCategory.BOOST);
    public static final SinCategoryEffect SLOTH_WEAKEN = regSin(SinType.SLOTH, EffectCategory.WEAKEN);
    public static final SinCategoryEffect SLOTH_VULNERABLE = regSin(SinType.SLOTH, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect SLOTH_REDUCE = regSin(SinType.SLOTH, EffectCategory.REDUCE);

    public static final SinCategoryEffect GLUTTONY_STRENGTHEN = regSin(SinType.GLUTTONY, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect GLUTTONY_GUARD = regSin(SinType.GLUTTONY, EffectCategory.GUARD);
    public static final SinCategoryEffect GLUTTONY_BOOST = regSin(SinType.GLUTTONY, EffectCategory.BOOST);
    public static final SinCategoryEffect GLUTTONY_WEAKEN = regSin(SinType.GLUTTONY, EffectCategory.WEAKEN);
    public static final SinCategoryEffect GLUTTONY_VULNERABLE = regSin(SinType.GLUTTONY, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect GLUTTONY_REDUCE = regSin(SinType.GLUTTONY, EffectCategory.REDUCE);

    public static final SinCategoryEffect GLOOM_STRENGTHEN = regSin(SinType.GLOOM, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect GLOOM_GUARD = regSin(SinType.GLOOM, EffectCategory.GUARD);
    public static final SinCategoryEffect GLOOM_BOOST = regSin(SinType.GLOOM, EffectCategory.BOOST);
    public static final SinCategoryEffect GLOOM_WEAKEN = regSin(SinType.GLOOM, EffectCategory.WEAKEN);
    public static final SinCategoryEffect GLOOM_VULNERABLE = regSin(SinType.GLOOM, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect GLOOM_REDUCE = regSin(SinType.GLOOM, EffectCategory.REDUCE);

    public static final SinCategoryEffect PRIDE_STRENGTHEN = regSin(SinType.PRIDE, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect PRIDE_GUARD = regSin(SinType.PRIDE, EffectCategory.GUARD);
    public static final SinCategoryEffect PRIDE_BOOST = regSin(SinType.PRIDE, EffectCategory.BOOST);
    public static final SinCategoryEffect PRIDE_WEAKEN = regSin(SinType.PRIDE, EffectCategory.WEAKEN);
    public static final SinCategoryEffect PRIDE_VULNERABLE = regSin(SinType.PRIDE, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect PRIDE_REDUCE = regSin(SinType.PRIDE, EffectCategory.REDUCE);

    public static final SinCategoryEffect ENVY_STRENGTHEN = regSin(SinType.ENVY, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect ENVY_GUARD = regSin(SinType.ENVY, EffectCategory.GUARD);
    public static final SinCategoryEffect ENVY_BOOST = regSin(SinType.ENVY, EffectCategory.BOOST);
    public static final SinCategoryEffect ENVY_WEAKEN = regSin(SinType.ENVY, EffectCategory.WEAKEN);
    public static final SinCategoryEffect ENVY_VULNERABLE = regSin(SinType.ENVY, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect ENVY_REDUCE = regSin(SinType.ENVY, EffectCategory.REDUCE);

    public static final SinCategoryEffect PHYSICAL_STRENGTHEN = regPhys(EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect PHYSICAL_GUARD = regPhys(EffectCategory.GUARD);
    public static final SinCategoryEffect PHYSICAL_BOOST = regPhys(EffectCategory.BOOST);
    public static final SinCategoryEffect PHYSICAL_WEAKEN = regPhys(EffectCategory.WEAKEN);
    public static final SinCategoryEffect PHYSICAL_VULNERABLE = regPhys(EffectCategory.VULNERABLE);
    public static final SinCategoryEffect PHYSICAL_REDUCE = regPhys(EffectCategory.REDUCE);

    public static final FragmentBoostEffect FRAGMENT_BOOST = register("fragment_boost", new FragmentBoostEffect());
    public static final NoCostEffect NO_COST = register("no_cost", new NoCostEffect());
    public static final IgnoreResistanceEffect IGNORE_RESISTANCE = register("ignore_resistance", new IgnoreResistanceEffect());
    public static final FragmentDrainEffect FRAGMENT_DRAIN = register("fragment_drain", new FragmentDrainEffect());
    public static final CostIncreaseEffect COST_INCREASE = register("cost_increase", new CostIncreaseEffect());

    public static final BurstEffect WRATH_BURST = regBurst(SinType.WRATH);
    public static final BurstEffect LUST_BURST = regBurst(SinType.LUST);
    public static final BurstEffect SLOTH_BURST = regBurst(SinType.SLOTH);
    public static final BurstEffect GLUTTONY_BURST = regBurst(SinType.GLUTTONY);
    public static final BurstEffect GLOOM_BURST = regBurst(SinType.GLOOM);
    public static final BurstEffect PRIDE_BURST = regBurst(SinType.PRIDE);
    public static final BurstEffect ENVY_BURST = regBurst(SinType.ENVY);

    private static SinCategoryEffect regSin(SinType sinType, EffectCategory category) {
        SinCategoryEffect effect = register(
                sinType.name().toLowerCase() + "_" + category.name().toLowerCase(),
                new SinCategoryEffect(sinType, category));
        SIN_EFFECTS.computeIfAbsent(sinType, k -> new EnumMap<>(EffectCategory.class)).put(category, effect);
        return effect;
    }

    private static SinCategoryEffect regPhys(EffectCategory category) {
        SinCategoryEffect effect = register("physical_" + category.name().toLowerCase(),
                new SinCategoryEffect(null, category));
        PHYS_EFFECTS.put(category, effect);
        return effect;
    }

    private static BurstEffect regBurst(SinType sinType) {
        BurstEffect effect = register("burst_" + sinType.name().toLowerCase(), new BurstEffect(sinType));
        BURST_EFFECTS.put(sinType, effect);
        return effect;
    }

    private static <T extends StatusEffect> T register(String id, T effect) {
        return Registry.register(Registries.STATUS_EFFECT, new Identifier("attack_type", id), effect);
    }

    public static SinCategoryEffect getSinEffect(SinType sinType, EffectCategory category) {
        Map<EffectCategory, SinCategoryEffect> map = SIN_EFFECTS.get(sinType);
        return map != null ? map.get(category) : null;
    }

    public static SinCategoryEffect getPhysEffect(EffectCategory category) {
        return PHYS_EFFECTS.get(category);
    }

    public static BurstEffect getBurstEffect(SinType sinType) {
        return BURST_EFFECTS.get(sinType);
    }

    public static void initialize() {
    }
}