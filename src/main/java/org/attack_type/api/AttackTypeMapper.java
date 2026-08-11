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

public class AttackTypeMapper {

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
        if (!(attacker instanceof PlayerEntity player)) {
            return null;
        }

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

    public static int getSinLevel(LivingEntity attacker) {
        if (!(attacker instanceof PlayerEntity player)) {
            return 0;
        }

        SinFragmentManager.SinFragmentState activeSin = SinFragmentManager.getActiveSin(player, player.getWorld().getTime());
        if (activeSin != null) {
            return activeSin.level;
        }
        return 1;
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
}