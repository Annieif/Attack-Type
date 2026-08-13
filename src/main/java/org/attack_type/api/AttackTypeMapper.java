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

/**
 * 攻击类型与罪孽属性映射器。
 * <p>
 * 根据伤害来源和攻击者信息，判定本次攻击的：
 * <ul>
 *   <li>物理攻击类型（{@link AttackType}）— 基于武器/弹射物类型</li>
 *   <li>罪孽属性（{@link SinType}）— 基于玩家碎片系统或生物随机掷骰</li>
 *   <li>罪孽等级（1~3）— 决定罪孽伤害倍率</li>
 * </ul>
 * <p>
 * 使用 {@link ThreadLocal} 缓存保证同一攻击中 {@code getSinType()} 和 {@code getSinLevel()}
 * 的返回值一致，调用结束后需通过 {@link #clearMobSinCache()} 清理。
 */
public class AttackTypeMapper {

    private static final ThreadLocal<MobSinResult> MOB_SIN_CACHE = new ThreadLocal<>();

    /**
     * 根据伤害来源判定物理攻击类型。
     * <ul>
     *   <li>箭矢、投掷三叉戟 → {@link AttackType#PIERCE 突刺}</li>
     *   <li>其他弹射物（雪球、鸡蛋等）→ {@link AttackType#BLUNT 打击}</li>
     *   <li>近战武器（剑、斧、三叉戟）→ {@link AttackType#SLASH 斩击}</li>
     *   <li>空手/其他物品 → {@link AttackType#BLUNT 打击}</li>
     *   <li>非实体来源（摔落、火焰等）→ {@link AttackType#NONE 无}</li>
     * </ul>
     *
     * @param source 伤害来源
     * @return 物理攻击类型
     */
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

    /**
     * 获取攻击者的罪孽属性。
     * <p>
     * 玩家：优先检查激活罪孽（L1/L2/L3），其次检查溢出自动触发（≥500 碎片），否则返回 null。
     * 非玩家：通过 {@link #rollMobSin(LivingEntity)} 随机掷骰（5% 基础 + 附魔加成）。
     * 结果通过 {@link ThreadLocal} 缓存，确保同一次攻击中类型和等级一致。
     *
     * @param attacker 攻击者
     * @return 罪孽属性，无则返回 null
     */
    public static SinType getSinType(LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            MOB_SIN_CACHE.remove();

            SinFragmentManager.SinFragmentState activeSin = SinFragmentManager.getActiveSin(player, player.getWorld().getTime());
            if (activeSin != null) {
                return activeSin.sinType;
            }

            for (SinType sinType : SinType.values()) {
                int frags = SinFragmentManager.getData(player).getFragments(sinType);
                if (frags >= 100) {
                    int cost = SinFragmentData.COST_LEVEL_3;
                    Enchantment enchant = ModEnchantments.getSinEnchantment(sinType);
                    int enchantLevel = enchant != null ? EnchantmentHelper.getLevel(enchant, player.getMainHandStack()) : 0;
                    cost = SinFragmentManager.getData(player).getCostWithEnchantment(cost, enchantLevel);
                    if (SinFragmentManager.getData(player).consumeFragments(sinType, cost)) {
                        SinFragmentManager.getData(player).setActiveSinType(sinType);
                        SinFragmentManager.getData(player).setActiveSinLevel(3);
                        SinFragmentManager.getData(player).setActiveSinExpiry(player.getWorld().getTime() + SinFragmentData.DURATION_L3_TICKS);
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

    /**
     * 获取罪孽攻击等级（1~3）。
     * <p>
     * 玩家：返回激活罪孽的等级，无激活时返回 1（溢出自动触发）。
     * 非玩家：返回随机掷骰结果，与 {@link #getSinType(LivingEntity)} 共享缓存。
     *
     * @param attacker 攻击者
     * @return 罪孽等级 1~3，无则返回 0
     */
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

    /**
     * 清理非玩家罪孽掷骰缓存。应在每次伤害计算结束后调用。
     */
    public static void clearMobSinCache() {
        MOB_SIN_CACHE.remove();
    }

    /**
     * 检查攻击者是否应被即死（任一罪孽碎片 ≥ 1000）。
     *
     * @param attacker 攻击者
     * @return true 表示攻击者应立即被击杀
     */
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

    /**
     * 非玩家生物罪孽掷骰结果。
     */
    private static final class MobSinResult {
        final SinType sinType;
        final int level;
        MobSinResult(SinType sinType, int level) {
            this.sinType = sinType;
            this.level = level;
        }
    }

    /**
     * 为非玩家生物随机掷骰罪孽属性。
     * <p>
     * 每种罪孽独立掷骰：基础概率 5%，每级对应附魔 +10% 触发率。
     * 等级范围：无附魔时 1~3 均等随机；有附魔时 minLevel = enchantLevel (上限 3)，maxLevel = enchantLevel + 1。
     * 使用确定性种子（基于 UUID + 坐标 + 时间），确保多端一致。
     *
     * @param attacker 非玩家攻击者
     * @return 掷骰结果（可能为 null 表示未触发）
     */
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