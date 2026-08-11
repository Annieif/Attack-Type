package org.attack_type.api;

import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.attack_type.enchantment.ModEnchantments;
import org.attack_type.fragment.SinFragmentData;
import org.attack_type.fragment.SinFragmentManager;

import java.util.Random;

public class AttackTypeMapper {

    private static final ThreadLocal<MobSinResult> MOB_SIN_CACHE = new ThreadLocal<>();

    public static AttackType getAttackType(DamageSource source) {
        if (source.getSource() instanceof ProjectileEntity projectile) {
            if (projectile instanceof ArrowEntity || projectile instanceof TridentEntity) {
                return AttackType.PIERCE;
            }
            return AttackType.BLUNT;
        }

        if (source.getSource() instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandStack();
            if (weapon.isEmpty()) {
                return AttackType.BLUNT;
            }

            String itemName = weapon.getItem().toString().toLowerCase();
            if (itemName.contains("sword") || itemName.contains("axe") || itemName.contains("trident")) {
                return AttackType.SLASH;
            }

            return AttackType.BLUNT;
        }

        return AttackType.NONE;
    }

    public static SinType getSinType(LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            MOB_SIN_CACHE.remove();

            SinFragmentManager.SinFragmentState activeSin = SinFragmentManager.getActiveSin(player, player.getWorld().getTime());
            if (activeSin != null) {
                return activeSin.sinType;
            }

            for (SinType sinType : SinType.values()) {
                if (SinFragmentManager.getData(player).isOverflowing(sinType)) {
                    int cost = SinFragmentData.COST_LEVEL_1;
                    Enchantment enchant = ModEnchantments.getSinEnchantment(sinType);
                    int enchantLevel = enchant != null ? EnchantmentHelper.getLevel(enchant, player.getMainHandStack()) : 0;
                    cost = SinFragmentManager.getData(player).getCostWithEnchantment(cost, enchantLevel);
                    if (SinFragmentManager.getData(player).consumeFragments(sinType, cost)) {
                        SinFragmentManager.getData(player).setActiveSinType(sinType);
                        SinFragmentManager.getData(player).setActiveSinLevel(1);
                        SinFragmentManager.getData(player).setActiveSinExpiry(player.getWorld().getTime() + SinFragmentData.DURATION_L1_TICKS);
                        return sinType;
                    }
                    break;
                }
            }

            return null;
        }

        MobSinResult cached = MOB_SIN_CACHE.get();
        if (cached != null) return cached.sinType;
        MobSinResult result = rollMobSin(attacker);
        MOB_SIN_CACHE.set(result);
        return result.sinType;
    }

    public static int getSinLevel(LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            MOB_SIN_CACHE.remove();

            SinFragmentManager.SinFragmentState activeSin = SinFragmentManager.getActiveSin(player, player.getWorld().getTime());
            if (activeSin != null) {
                return activeSin.level;
            }
            return 1;
        }

        MobSinResult cached = MOB_SIN_CACHE.get();
        if (cached != null) return cached.level;
        MobSinResult result = rollMobSin(attacker);
        MOB_SIN_CACHE.set(result);
        return result.level;
    }

    public static void clearMobSinCache() {
        MOB_SIN_CACHE.remove();
    }

    public static boolean shouldKillPlayer(LivingEntity attacker) {
        if (!(attacker instanceof PlayerEntity player)) {
            return false;
        }
        for (SinType sinType : SinType.values()) {
            if (SinFragmentManager.getData(player).shouldKill(sinType)) {
                return true;
            }
        }
        return false;
    }

    private static final class MobSinResult {
        final SinType sinType;
        final int level;
        MobSinResult(SinType sinType, int level) {
            this.sinType = sinType;
            this.level = level;
        }
    }

    private static MobSinResult rollMobSin(LivingEntity attacker) {
        ItemStack weapon = attacker.getMainHandStack();
        long seed = attacker.getUuid().getLeastSignificantBits()
                ^ attacker.getWorld().getTime()
                ^ attacker.getBlockX() ^ attacker.getBlockY() ^ attacker.getBlockZ();
        Random random = new Random(seed);

        SinType selectedType = null;
        int selectedLevel = 1;
        double bestChance = 0;

        for (SinType sinType : SinType.values()) {
            Enchantment enchant = ModEnchantments.getSinEnchantment(sinType);
            int enchantLevel = (enchant != null && !weapon.isEmpty())
                    ? EnchantmentHelper.getLevel(enchant, weapon) : 0;

            double chance = 0.05 + enchantLevel * 0.10;
            if (random.nextDouble() < chance) {
                if (chance > bestChance) {
                    bestChance = chance;
                    selectedType = sinType;
                    int minLevel = Math.max(1, Math.min(3, enchantLevel));
                    int maxLevel = Math.min(3, Math.max(1, enchantLevel + random.nextInt(2)));
                    selectedLevel = minLevel + random.nextInt(Math.max(1, maxLevel - minLevel + 1));
                }
            }
        }

        if (selectedType == null) {
            return new MobSinResult(null, 0);
        }
        return new MobSinResult(selectedType, selectedLevel);
    }
}