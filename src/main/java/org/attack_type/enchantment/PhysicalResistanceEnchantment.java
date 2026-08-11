package org.attack_type.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.attack_type.api.AttackType;

/**
 * 物理抗性护甲附魔。
 * <p>
 * 附在护甲上，降低受到的对应类型物理伤害。
 * 每级提供 5% 减伤，4 件护甲独立乘算。
 * <ul>
 *   <li>稀有度：RARE</li>
 *   <li>最高等级：4</li>
 *   <li>适用槽位：头盔、胸甲、护腿、靴子</li>
 *   <li>附魔台权重：1 + (level-1) × 8</li>
 *   <li>减伤公式：每件护甲提供 (1 - 0.05 × level)，4 件乘算</li>
 * </ul>
 */
public class PhysicalResistanceEnchantment extends Enchantment {
    private final AttackType attackType;

    /**
     * @param attackType 对应的攻击类型
     */
    public PhysicalResistanceEnchantment(AttackType attackType) {
        super(Rarity.RARE, EnchantmentTarget.ARMOR, new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
        this.attackType = attackType;
    }

    /**
     * @return 此附魔对应的攻击类型
     */
    public AttackType getAttackType() {
        return attackType;
    }

    /**
     * 计算单件护甲的物理抗性乘数。
     *
     * @param level 附魔等级
     * @return 抗性乘数 = 1 - 0.05 × level
     */
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