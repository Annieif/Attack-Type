package org.attack_type.effect;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
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
 * 两段式酿造：粗制药水 + 罪孽材料 → 罪孽基础药水；基础药水 + 分类材料 → 具体药水。
 * 物理药水：粗制药水 + 分类材料 → 物理药水。
 */
public class ModPotions {

    private static final int BASE_DURATION = 3600;
    private static final int EXTENDED_DURATION = 9600;

    private static final Item STRENGTHEN_MAT = Items.FIRE_CHARGE;
    private static final Item GUARD_MAT = Items.IRON_INGOT;
    private static final Item BOOST_MAT = Items.PRISMARINE_CRYSTALS;
    private static final Item WEAKEN_MAT = Items.POISONOUS_POTATO;
    private static final Item VULNERABLE_MAT = Items.STRING;
    private static final Item REDUCE_MAT = Items.POPPED_CHORUS_FRUIT;
    private static final Item BURST_MAT = Items.ECHO_SHARD;

    private static final Item[] SIN_BASE_MATS = {
            Items.BLAZE_ROD, Items.ROSE_BUSH, Items.FEATHER, Items.ROTTEN_FLESH,
            Items.INK_SAC, Items.GOLD_INGOT, Items.EMERALD
    };

    private static Potion[] sinBasePotions;

    public static void initialize() {
        registerSinBasePotions();
        registerSinPotions();
        registerPhysPotions();
        registerBurstPotions();
        registerGenericPotions();
    }

    private static void registerSinBasePotions() {
        SinType[] sins = SinType.values();
        sinBasePotions = new Potion[sins.length];
        for (int i = 0; i < sins.length; i++) {
            String id = sins[i].name().toLowerCase() + "_base";
            sinBasePotions[i] = register(id, new Potion());
            BrewingRecipeRegistry.registerPotionRecipe(Potions.AWKWARD, SIN_BASE_MATS[i], sinBasePotions[i]);
        }
    }

    private static void registerSinPotions() {
        SinType[] sins = SinType.values();
        String[] catNames = {"strengthen", "guard", "boost", "weaken", "vulnerable", "reduce"};
        Item[] catMats = {STRENGTHEN_MAT, GUARD_MAT, BOOST_MAT, WEAKEN_MAT, VULNERABLE_MAT, REDUCE_MAT};
        EffectCategory[] categories = EffectCategory.values();

        for (int si = 0; si < sins.length; si++) {
            String prefix = sins[si].name().toLowerCase();
            for (int ci = 0; ci < catNames.length; ci++) {
                SinCategoryEffect effect = ModStatusEffects.getSinEffect(sins[si], categories[ci]);
                if (effect == null) continue;
                String id = prefix + "_" + catNames[ci];
                registerPotionLevels(id, effect, BASE_DURATION, EXTENDED_DURATION, sinBasePotions[si], catMats[ci]);
            }
        }
    }

    private static void registerPhysPotions() {
        String[] names = {"strengthen", "guard", "boost", "weaken", "vulnerable", "reduce"};
        Item[] catMats = {STRENGTHEN_MAT, GUARD_MAT, BOOST_MAT, WEAKEN_MAT, VULNERABLE_MAT, REDUCE_MAT};
        EffectCategory[] categories = EffectCategory.values();

        for (int i = 0; i < names.length; i++) {
            SinCategoryEffect effect = ModStatusEffects.getPhysEffect(categories[i]);
            if (effect == null) continue;
            String id = "physical_" + names[i];
            registerPotionLevels(id, effect, BASE_DURATION, EXTENDED_DURATION, Potions.AWKWARD, catMats[i]);
        }
    }

    private static void registerBurstPotions() {
        SinType[] sins = SinType.values();

        for (int i = 0; i < sins.length; i++) {
            BurstEffect effect = ModStatusEffects.getBurstEffect(sins[i]);
            if (effect == null) continue;
            String id = "burst_" + sins[i].name().toLowerCase();
            Potion[] levels = new Potion[5];
            for (int lv = 0; lv < 5; lv++) {
                String suffix = lv == 0 ? "" : "_lv" + (lv + 1);
                levels[lv] = register(id + suffix, new Potion(new StatusEffectInstance(effect, BASE_DURATION, lv)));
            }
            Potion lv1Long = register(id + "_long", new Potion(new StatusEffectInstance(effect, EXTENDED_DURATION, 0)));

            BrewingRecipeRegistry.registerPotionRecipe(sinBasePotions[i], BURST_MAT, levels[0]);
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

    private static void registerGeneric(String id, net.minecraft.entity.effect.StatusEffect effect, int baseDuration, Item ingredient) {
        registerPotionLevels(id, effect, baseDuration, baseDuration * 8 / 3, Potions.AWKWARD, ingredient);
    }

    private static void registerPotionLevels(String id, net.minecraft.entity.effect.StatusEffect effect,
                                              int baseDuration, int extendedDuration,
                                              Potion basePotion, Item ingredient) {
        Potion lv1 = register(id, new Potion(new StatusEffectInstance(effect, baseDuration, 0)));
        Potion lv2 = register(id + "_strong", new Potion(new StatusEffectInstance(effect, baseDuration, 1)));
        Potion lv3 = register(id + "_very_strong", new Potion(new StatusEffectInstance(effect, baseDuration, 2)));
        Potion lv1Long = register(id + "_long", new Potion(new StatusEffectInstance(effect, extendedDuration, 0)));

        BrewingRecipeRegistry.registerPotionRecipe(basePotion, ingredient, lv1);
        BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.GLOWSTONE_DUST, lv2);
        BrewingRecipeRegistry.registerPotionRecipe(lv2, Items.GLOWSTONE_DUST, lv3);
        BrewingRecipeRegistry.registerPotionRecipe(lv1, Items.REDSTONE, lv1Long);
    }

    private static Potion register(String id, Potion potion) {
        return Registry.register(Registries.POTION, new Identifier("attack_type", id), potion);
    }
}