package org.attack_type.api;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import org.attack_type.enchantment.ModEnchantments;

public class AttackTypeMapper {

    public static AttackType getAttackType(DamageSource source) {
        if (source.getSource() instanceof ProjectileEntity) {
            return AttackType.PIERCE;
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
        ItemStack weapon = attacker.getMainHandStack();
        if (weapon.isEmpty()) {
            return null;
        }

        SinType bestSin = null;
        int bestLevel = 0;

        for (SinType sinType : SinType.values()) {
            Enchantment enchantment = ModEnchantments.getSinEnchantment(sinType);
            if (enchantment == null) continue;
            int level = EnchantmentHelper.getLevel(enchantment, weapon);
            if (level > bestLevel) {
                bestLevel = level;
                bestSin = sinType;
            }
        }

        if (bestSin != null) {
            float probability = bestLevel * 0.1f;
            if (Math.random() < probability) {
                return bestSin;
            }
        }

        return null;
    }
}