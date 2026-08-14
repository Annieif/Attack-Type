package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.attack_type.api.SinType;

/**
 * 药水注册中心。
 * <p>
 * 注册 53 效果 × 3 级 = 159 个药水，以及对应的酿造配方。
 */
public class ModPotions {

    private static final int BASE_DURATION = 3600;
    private static final int EXTENDED_DURATION = 9600;

    public static void initialize() {
        registerSinPotions(SinType.WRATH, Items.BLAZE_POWDER);
        registerSinPotions(SinType.LUST, Items.ROSE_BUSH);
        registerSinPotions(SinType.SLOTH, Items.FEATHER);
        registerSinPotions(SinType.GLUTTONY, Items.ROTTEN_FLESH);
        registerSinPotions(SinType.GLOOM, Items.INK_SAC);
        registerSinPotions(SinType.PRIDE, Items.GOLD_INGOT);
        registerSinPotions(SinType.ENVY, Items.EMERALD);

        registerPhysPotions(Items.IRON_INGOT);

        registerBurstPotions();

        registerGenericPotions();
    }

    private static void registerSinPotions(SinType sinType, net.minecraft.item.Item ingredient) {
        String prefix = sinType.name().toLowerCase();
        SinCategoryEffect[] effects = {
                getSinEffect(sinType, EffectCategory.STRENGTHEN),
                getSinEffect(sinType, EffectCategory.GUARD),
                getSinEffect(sinType, EffectCategory.BOOST),
                getSinEffect(sinType, EffectCategory.WEAKEN),
                getSinEffect(sinType, EffectCategory.VULNERABLE),
                getSinEffect(sinType, EffectCategory.REDUCE)
        };
        String[] names = {"strengthen", "guard", "boost", "weaken", "vulnerable", "reduce"};

        for (int i = 0; i < effects.length; i++) {
            String id = prefix + "_" + names[i];
            Potion lv1 = register(id, new Potion(new StatusEffectInstance(effects[i], BASE_DURATION, 0)));
            Potion lv2 = register(id + "_strong", new Potion(new StatusEffectInstance(effects[i], BASE_DURATION, 1)));
            Potion lv3 = register(id + "_very_strong", new Potion(new StatusEffectInstance(effects[i], BASE_DURATION, 2)));
            Potion lv1Long = register(id + "_long", new Potion(new StatusEffectInstance(effects[i], EXTENDED_DURATION, 0)));

            BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, ingredient, lv1);
            BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.GLOWSTONE_DUST, lv2);
            BrewingRecipeRegistry.registerPotionRecipe(lv2, Items.GLOWSTONE_DUST, lv3);
            BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.REDSTONE, lv1Long);
        }
    }

    private static void registerPhysPotions(net.minecraft.item.Item ingredient) {
        SinCategoryEffect[] effects = {
                ModStatusEffects.PHYSICAL_STRENGTHEN,
                ModStatusEffects.PHYSICAL_GUARD,
                ModStatusEffects.PHYSICAL_BOOST,
                ModStatusEffects.PHYSICAL_WEAKEN,
                ModStatusEffects.PHYSICAL_VULNERABLE,
                ModStatusEffects.PHYSICAL_REDUCE
        };
        String[] names = {"strengthen", "guard", "boost", "weaken", "vulnerable", "reduce"};

        for (int i = 0; i < effects.length; i++) {
            String id = "physical_" + names[i];
            Potion lv1 = register(id, new Potion(new StatusEffectInstance(effects[i], BASE_DURATION, 0)));
            Potion lv2 = register(id + "_strong", new Potion(new StatusEffectInstance(effects[i], BASE_DURATION, 1)));
            Potion lv3 = register(id + "_very_strong", new Potion(new StatusEffectInstance(effects[i], BASE_DURATION, 2)));
            Potion lv1Long = register(id + "_long", new Potion(new StatusEffectInstance(effects[i], EXTENDED_DURATION, 0)));

            BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, ingredient, lv1);
            BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.GLOWSTONE_DUST, lv2);
            BrewingRecipeRegistry.registerPotionRecipe(lv2, Items.GLOWSTONE_DUST, lv3);
            BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.REDSTONE, lv1Long);
        }
    }

    private static void registerBurstPotions() {
        SinType[] sins = SinType.values();
        BurstEffect[] effects = {
                ModStatusEffects.WRATH_BURST, ModStatusEffects.LUST_BURST,
                ModStatusEffects.SLOTH_BURST, ModStatusEffects.GLUTTONY_BURST,
                ModStatusEffects.GLOOM_BURST, ModStatusEffects.PRIDE_BURST,
                ModStatusEffects.ENVY_BURST
        };
        net.minecraft.item.Item[] ingredients = {
                Items.BLAZE_POWDER, Items.ROSE_BUSH, Items.FEATHER, Items.ROTTEN_FLESH,
                Items.INK_SAC, Items.GOLD_INGOT, Items.EMERALD
        };

        for (int i = 0; i < sins.length; i++) {
            String id = "burst_" + sins[i].name().toLowerCase();
            Potion[] levels = new Potion[5];
            for (int lv = 0; lv < 5; lv++) {
                String suffix = lv == 0 ? "" : "_lv" + (lv + 1);
                levels[lv] = register(id + suffix, new Potion(new StatusEffectInstance(effects[i], BASE_DURATION, lv)));
            }
            Potion lv1Long = register(id + "_long", new Potion(new StatusEffectInstance(effects[i], EXTENDED_DURATION, 0)));

            BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, ingredients[i], levels[0]);
            for (int lv = 0; lv < 4; lv++) {
                BrewingRecipeRegistry.registerPotionRecipe(levels[lv], Items.GLOWSTONE_DUST, levels[lv + 1]);
            }
            BrewingRecipeRegistry.registerPotionRecipe(levels[0], Items.REDSTONE, lv1Long);
        }
    }

    private static void registerGenericPotions() {
        registerGeneric("fragment_boost", ModStatusEffects.FRAGMENT_BOOST, 6000, Items.AMETHYST_SHARD);
        registerGeneric("no_cost", ModStatusEffects.NO_COST, 1200, Items.DRAGON_BREATH);
        registerGeneric("ignore_resistance", ModStatusEffects.IGNORE_RESISTANCE, 2400, Items.NETHERITE_SCRAP);
        registerGeneric("fragment_drain", ModStatusEffects.FRAGMENT_DRAIN, 600, Items.WITHER_ROSE);
        registerGeneric("cost_increase", ModStatusEffects.COST_INCREASE, BASE_DURATION, Items.MAGMA_CREAM);
    }

    private static void registerGeneric(String id, net.minecraft.entity.effect.StatusEffect effect, int baseDuration, net.minecraft.item.Item ingredient) {
        Potion lv1 = register(id, new Potion(new StatusEffectInstance(effect, baseDuration, 0)));
        Potion lv2 = register(id + "_strong", new Potion(new StatusEffectInstance(effect, baseDuration, 1)));
        Potion lv3 = register(id + "_very_strong", new Potion(new StatusEffectInstance(effect, baseDuration, 2)));
        Potion lv1Long = register(id + "_long", new Potion(new StatusEffectInstance(effect, baseDuration * 8 / 3, 0)));

        BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, ingredient, lv1);
        BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.GLOWSTONE_DUST, lv2);
        BrewingRecipeRegistry.registerPotionRecipe(lv2, Items.GLOWSTONE_DUST, lv3);
        BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.REDSTONE, lv1Long);
    }

    private static SinCategoryEffect getSinEffect(SinType sinType, EffectCategory category) {
        return switch (category) {
            case STRENGTHEN -> switch (sinType) {
                case WRATH -> ModStatusEffects.WRATH_STRENGTHEN;
                case LUST -> ModStatusEffects.LUST_STRENGTHEN;
                case SLOTH -> ModStatusEffects.SLOTH_STRENGTHEN;
                case GLUTTONY -> ModStatusEffects.GLUTTONY_STRENGTHEN;
                case GLOOM -> ModStatusEffects.GLOOM_STRENGTHEN;
                case PRIDE -> ModStatusEffects.PRIDE_STRENGTHEN;
                case ENVY -> ModStatusEffects.ENVY_STRENGTHEN;
            };
            case GUARD -> switch (sinType) {
                case WRATH -> ModStatusEffects.WRATH_GUARD;
                case LUST -> ModStatusEffects.LUST_GUARD;
                case SLOTH -> ModStatusEffects.SLOTH_GUARD;
                case GLUTTONY -> ModStatusEffects.GLUTTONY_GUARD;
                case GLOOM -> ModStatusEffects.GLOOM_GUARD;
                case PRIDE -> ModStatusEffects.PRIDE_GUARD;
                case ENVY -> ModStatusEffects.ENVY_GUARD;
            };
            case BOOST -> switch (sinType) {
                case WRATH -> ModStatusEffects.WRATH_BOOST;
                case LUST -> ModStatusEffects.LUST_BOOST;
                case SLOTH -> ModStatusEffects.SLOTH_BOOST;
                case GLUTTONY -> ModStatusEffects.GLUTTONY_BOOST;
                case GLOOM -> ModStatusEffects.GLOOM_BOOST;
                case PRIDE -> ModStatusEffects.PRIDE_BOOST;
                case ENVY -> ModStatusEffects.ENVY_BOOST;
            };
            case WEAKEN -> switch (sinType) {
                case WRATH -> ModStatusEffects.WRATH_WEAKEN;
                case LUST -> ModStatusEffects.LUST_WEAKEN;
                case SLOTH -> ModStatusEffects.SLOTH_WEAKEN;
                case GLUTTONY -> ModStatusEffects.GLUTTONY_WEAKEN;
                case GLOOM -> ModStatusEffects.GLOOM_WEAKEN;
                case PRIDE -> ModStatusEffects.PRIDE_WEAKEN;
                case ENVY -> ModStatusEffects.ENVY_WEAKEN;
            };
            case VULNERABLE -> switch (sinType) {
                case WRATH -> ModStatusEffects.WRATH_VULNERABLE;
                case LUST -> ModStatusEffects.LUST_VULNERABLE;
                case SLOTH -> ModStatusEffects.SLOTH_VULNERABLE;
                case GLUTTONY -> ModStatusEffects.GLUTTONY_VULNERABLE;
                case GLOOM -> ModStatusEffects.GLOOM_VULNERABLE;
                case PRIDE -> ModStatusEffects.PRIDE_VULNERABLE;
                case ENVY -> ModStatusEffects.ENVY_VULNERABLE;
            };
            case REDUCE -> switch (sinType) {
                case WRATH -> ModStatusEffects.WRATH_REDUCE;
                case LUST -> ModStatusEffects.LUST_REDUCE;
                case SLOTH -> ModStatusEffects.SLOTH_REDUCE;
                case GLUTTONY -> ModStatusEffects.GLUTTONY_REDUCE;
                case GLOOM -> ModStatusEffects.GLOOM_REDUCE;
                case PRIDE -> ModStatusEffects.PRIDE_REDUCE;
                case ENVY -> ModStatusEffects.ENVY_REDUCE;
            };
        };
    }

    private static Potion register(String id, Potion potion) {
        return Registry.register(Registries.POTION, new Identifier("attack_type", id), potion);
    }
}