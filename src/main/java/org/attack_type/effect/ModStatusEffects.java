package org.attack_type.effect;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.attack_type.api.SinType;

/**
 * 状态效果注册中心。
 * <p>
 * 注册 6 类 × (7 罪孽 + 1 物理) = 48 个分类效果 + 5 个通用效果，共 53 个 StatusEffect。
 */
public class ModStatusEffects {

    public static final SinCategoryEffect WRATH_STRENGTHEN = registerSinEffect(SinType.WRATH, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect WRATH_GUARD = registerSinEffect(SinType.WRATH, EffectCategory.GUARD);
    public static final SinCategoryEffect WRATH_BOOST = registerSinEffect(SinType.WRATH, EffectCategory.BOOST);
    public static final SinCategoryEffect WRATH_WEAKEN = registerSinEffect(SinType.WRATH, EffectCategory.WEAKEN);
    public static final SinCategoryEffect WRATH_VULNERABLE = registerSinEffect(SinType.WRATH, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect WRATH_REDUCE = registerSinEffect(SinType.WRATH, EffectCategory.REDUCE);

    public static final SinCategoryEffect LUST_STRENGTHEN = registerSinEffect(SinType.LUST, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect LUST_GUARD = registerSinEffect(SinType.LUST, EffectCategory.GUARD);
    public static final SinCategoryEffect LUST_BOOST = registerSinEffect(SinType.LUST, EffectCategory.BOOST);
    public static final SinCategoryEffect LUST_WEAKEN = registerSinEffect(SinType.LUST, EffectCategory.WEAKEN);
    public static final SinCategoryEffect LUST_VULNERABLE = registerSinEffect(SinType.LUST, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect LUST_REDUCE = registerSinEffect(SinType.LUST, EffectCategory.REDUCE);

    public static final SinCategoryEffect SLOTH_STRENGTHEN = registerSinEffect(SinType.SLOTH, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect SLOTH_GUARD = registerSinEffect(SinType.SLOTH, EffectCategory.GUARD);
    public static final SinCategoryEffect SLOTH_BOOST = registerSinEffect(SinType.SLOTH, EffectCategory.BOOST);
    public static final SinCategoryEffect SLOTH_WEAKEN = registerSinEffect(SinType.SLOTH, EffectCategory.WEAKEN);
    public static final SinCategoryEffect SLOTH_VULNERABLE = registerSinEffect(SinType.SLOTH, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect SLOTH_REDUCE = registerSinEffect(SinType.SLOTH, EffectCategory.REDUCE);

    public static final SinCategoryEffect GLUTTONY_STRENGTHEN = registerSinEffect(SinType.GLUTTONY, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect GLUTTONY_GUARD = registerSinEffect(SinType.GLUTTONY, EffectCategory.GUARD);
    public static final SinCategoryEffect GLUTTONY_BOOST = registerSinEffect(SinType.GLUTTONY, EffectCategory.BOOST);
    public static final SinCategoryEffect GLUTTONY_WEAKEN = registerSinEffect(SinType.GLUTTONY, EffectCategory.WEAKEN);
    public static final SinCategoryEffect GLUTTONY_VULNERABLE = registerSinEffect(SinType.GLUTTONY, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect GLUTTONY_REDUCE = registerSinEffect(SinType.GLUTTONY, EffectCategory.REDUCE);

    public static final SinCategoryEffect GLOOM_STRENGTHEN = registerSinEffect(SinType.GLOOM, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect GLOOM_GUARD = registerSinEffect(SinType.GLOOM, EffectCategory.GUARD);
    public static final SinCategoryEffect GLOOM_BOOST = registerSinEffect(SinType.GLOOM, EffectCategory.BOOST);
    public static final SinCategoryEffect GLOOM_WEAKEN = registerSinEffect(SinType.GLOOM, EffectCategory.WEAKEN);
    public static final SinCategoryEffect GLOOM_VULNERABLE = registerSinEffect(SinType.GLOOM, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect GLOOM_REDUCE = registerSinEffect(SinType.GLOOM, EffectCategory.REDUCE);

    public static final SinCategoryEffect PRIDE_STRENGTHEN = registerSinEffect(SinType.PRIDE, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect PRIDE_GUARD = registerSinEffect(SinType.PRIDE, EffectCategory.GUARD);
    public static final SinCategoryEffect PRIDE_BOOST = registerSinEffect(SinType.PRIDE, EffectCategory.BOOST);
    public static final SinCategoryEffect PRIDE_WEAKEN = registerSinEffect(SinType.PRIDE, EffectCategory.WEAKEN);
    public static final SinCategoryEffect PRIDE_VULNERABLE = registerSinEffect(SinType.PRIDE, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect PRIDE_REDUCE = registerSinEffect(SinType.PRIDE, EffectCategory.REDUCE);

    public static final SinCategoryEffect ENVY_STRENGTHEN = registerSinEffect(SinType.ENVY, EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect ENVY_GUARD = registerSinEffect(SinType.ENVY, EffectCategory.GUARD);
    public static final SinCategoryEffect ENVY_BOOST = registerSinEffect(SinType.ENVY, EffectCategory.BOOST);
    public static final SinCategoryEffect ENVY_WEAKEN = registerSinEffect(SinType.ENVY, EffectCategory.WEAKEN);
    public static final SinCategoryEffect ENVY_VULNERABLE = registerSinEffect(SinType.ENVY, EffectCategory.VULNERABLE);
    public static final SinCategoryEffect ENVY_REDUCE = registerSinEffect(SinType.ENVY, EffectCategory.REDUCE);

    public static final SinCategoryEffect PHYSICAL_STRENGTHEN = registerPhysEffect(EffectCategory.STRENGTHEN);
    public static final SinCategoryEffect PHYSICAL_GUARD = registerPhysEffect(EffectCategory.GUARD);
    public static final SinCategoryEffect PHYSICAL_BOOST = registerPhysEffect(EffectCategory.BOOST);
    public static final SinCategoryEffect PHYSICAL_WEAKEN = registerPhysEffect(EffectCategory.WEAKEN);
    public static final SinCategoryEffect PHYSICAL_VULNERABLE = registerPhysEffect(EffectCategory.VULNERABLE);
    public static final SinCategoryEffect PHYSICAL_REDUCE = registerPhysEffect(EffectCategory.REDUCE);

    public static final FragmentBoostEffect FRAGMENT_BOOST = register("fragment_boost", new FragmentBoostEffect());
    public static final NoCostEffect NO_COST = register("no_cost", new NoCostEffect());
    public static final IgnoreResistanceEffect IGNORE_RESISTANCE = register("ignore_resistance", new IgnoreResistanceEffect());
    public static final FragmentDrainEffect FRAGMENT_DRAIN = register("fragment_drain", new FragmentDrainEffect());
    public static final CostIncreaseEffect COST_INCREASE = register("cost_increase", new CostIncreaseEffect());

    public static final BurstEffect WRATH_BURST = registerBurst(SinType.WRATH);
    public static final BurstEffect LUST_BURST = registerBurst(SinType.LUST);
    public static final BurstEffect SLOTH_BURST = registerBurst(SinType.SLOTH);
    public static final BurstEffect GLUTTONY_BURST = registerBurst(SinType.GLUTTONY);
    public static final BurstEffect GLOOM_BURST = registerBurst(SinType.GLOOM);
    public static final BurstEffect PRIDE_BURST = registerBurst(SinType.PRIDE);
    public static final BurstEffect ENVY_BURST = registerBurst(SinType.ENVY);

    private static BurstEffect registerBurst(SinType sinType) {
        return register("burst_" + sinType.name().toLowerCase(), new BurstEffect(sinType));
    }

    private static SinCategoryEffect registerSinEffect(SinType sinType, EffectCategory category) {
        String id = sinType.name().toLowerCase() + "_" + category.name().toLowerCase();
        return register(id, new SinCategoryEffect(sinType, category));
    }

    private static SinCategoryEffect registerPhysEffect(EffectCategory category) {
        String id = "physical_" + category.name().toLowerCase();
        return register(id, new SinCategoryEffect(null, category));
    }

    private static <T extends net.minecraft.entity.effect.StatusEffect> T register(String id, T effect) {
        return Registry.register(Registries.STATUS_EFFECT, new Identifier("attack_type", id), effect);
    }

    public static void initialize() {
    }
}