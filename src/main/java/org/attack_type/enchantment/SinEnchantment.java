package org.attack_type.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.attack_type.api.SinType;

/**
 * 罪孽武器附魔。
 * <p>
 * 附在武器上（主手），使攻击附带对应罪孽属性。
 * 玩家手持此附魔武器时，手动触发罪孽可减少碎片消耗（-2/级）。
 * 非玩家生物持有此附魔武器时，对应罪孽触发率 +10%/级。
 * <ul>
 *   <li>稀有度：RARE</li>
 *   <li>最高等级：5</li>
 *   <li>适用槽位：主手</li>
 *   <li>附魔台权重：1 + (level-1) × 10</li>
 * </ul>
 */
public class SinEnchantment extends Enchantment {
    private final SinType sinType;

    /**
     * @param sinType 对应的罪孽类型
     */
    public SinEnchantment(SinType sinType) {
        super(Rarity.RARE, EnchantmentTarget.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.sinType = sinType;
    }

    /**
     * @return 此附魔对应的罪孽类型
     */
    public SinType getSinType() {
        return sinType;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public int getMinPower(int level) {
        return 1 + (level - 1) * 10;
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