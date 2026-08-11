package org.attack_type.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.attack_type.api.AttackType;

public class PhysicalResistanceEnchantment extends Enchantment {
    private final AttackType attackType;

    public PhysicalResistanceEnchantment(AttackType attackType) {
        super(Rarity.RARE, EnchantmentTarget.ARMOR, new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
        this.attackType = attackType;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public float getResistanceMultiplier(int level) {
        return 1.0f - level * 0.05f;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getMinPower(int level) {
        return 1 + (level - 1) * 8;
    }

    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 15;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return true;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return true;
    }
}