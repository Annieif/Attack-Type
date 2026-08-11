package org.attack_type.fragment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.component.ResistanceManager;
import org.attack_type.enchantment.ModEnchantments;
import org.attack_type.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 罪孽碎片管理器（服务端）。
 * <p>
 * 管理所有在线玩家的碎片数据，提供：
 * <ul>
 *   <li>碎片增减（受罪孽伤害时 +1，手动触发消耗）</li>
 *   <li>手动触发罪孽（按键 \，消耗 40/70/100 碎片，附魔 -2/级）</li>
 *   <li>激活罪孽状态查询（类型 + 等级 + 过期时间）</li>
 *   <li>玩家断线自动清理数据</li>
 * </ul>
 * <p>
 * 使用 {@link ConcurrentHashMap} 保证多线程安全。
 */
public class SinFragmentManager {
    /** 所有在线玩家的碎片数据（UUID → 数据） */
    private static final Map<UUID, SinFragmentData> PLAYER_DATA = new ConcurrentHashMap<>();

    /**
     * 获取或创建玩家的碎片数据。
     *
     * @param player 目标玩家
     * @return 玩家的碎片数据（非 null）
     */
    public static SinFragmentData getData(PlayerEntity player) {
        return PLAYER_DATA.computeIfAbsent(player.getUuid(), uuid -> new SinFragmentData());
    }

    /**
     * 移除玩家的碎片数据（断线清理）。
     *
     * @param uuid 玩家 UUID
     */
    public static void removeData(UUID uuid) {
        PLAYER_DATA.remove(uuid);
    }

    /**
     * 尝试手动触发罪孽攻击。
     * <p>
     * 消耗公式：{@code max(1, baseCost(level) - 2 × enchantLevel)}。
     * 成功触发后设置激活罪孽类型、等级和过期时间。
     *
     * @param player  目标玩家
     * @param sinType 罪孽类型
     * @param level   触发等级（1/2/3）
     * @return true 表示触发成功（碎片充足），false 表示碎片不足
     */
    public static boolean tryTriggerSin(ServerPlayerEntity player, SinType sinType, int level) {
        SinFragmentData data = getData(player);

        int baseCost;
        int duration;
        switch (level) {
            case 1: baseCost = SinFragmentData.COST_LEVEL_1; duration = SinFragmentData.DURATION_L1_TICKS; break;
            case 2: baseCost = SinFragmentData.COST_LEVEL_2; duration = SinFragmentData.DURATION_L2_TICKS; break;
            case 3: baseCost = SinFragmentData.COST_LEVEL_3; duration = SinFragmentData.DURATION_L3_TICKS; break;
            default: return false;
        }

        Enchantment enchant = ModEnchantments.getSinEnchantment(sinType);
        int enchantLevel = enchant != null ? EnchantmentHelper.getLevel(enchant, player.getMainHandStack()) : 0;
        int cost = data.getCostWithEnchantment(baseCost, enchantLevel);

        if (!data.consumeFragments(sinType, cost)) {
            return false;
        }

        data.setActiveSinType(sinType);
        data.setActiveSinLevel(level);
        data.setActiveSinExpiry(player.getWorld().getTime() + duration);
        return true;
    }

    /**
     * 获取玩家当前激活的罪孽状态（考虑过期时间）。
     *
     * @param player    目标玩家
     * @param worldTime 当前世界 tick
     * @return 激活罪孽状态，无则返回 null
     */
    public static SinFragmentState getActiveSin(PlayerEntity player, long worldTime) {
        SinFragmentData data = getData(player);
        if (data.isSinActive(worldTime)) {
            return new SinFragmentState(data.getActiveSinType(), data.getActiveSinLevel());
        }
        return null;
    }

    /**
     * 清除玩家激活罪孽状态。
     */
    public static void clearActiveSin(PlayerEntity player) {
        SinFragmentData data = getData(player);
        data.clearActiveSin();
    }

    /**
     * 获取玩家指定罪孽类型的碎片数量。
     */
    public static int getFragmentCount(PlayerEntity player, SinType type) {
        return getData(player).getFragments(type);
    }

    /**
     * 增加玩家指定罪孽类型的碎片数量。
     * <p>
     * 同时检查 500 碎片阈值：任意罪孽碎片首次达到 500，
     * 玩家的总积（totalProduct）减少 0.1，最低降至 0.1。
     */
    public static void addFragments(PlayerEntity player, SinType type, int amount) {
        SinFragmentData data = getData(player);
        data.addFragments(type, amount);

        int newCount = data.getFragments(type);
        if (newCount >= 500 && !data.hasThresholdReached(type)) {
            data.markThresholdReached(type);
            if (player instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) player;
                ResistanceProfile profile = ResistanceManager.getOrCreateProfile(living);
                float newProduct = Math.max(0.1f, profile.getTotalProduct() - 0.1f);
                profile.setTotalProduct(newProduct);
                if (living instanceof ServerPlayerEntity) {
                    ServerPlayerEntity sp = (ServerPlayerEntity) living;
                    ResistanceManager.syncToPlayer(sp);
                    sp.sendMessage(Text.translatable("cmd.attack_type.total_product_decay", String.format("%.1f", newProduct)), true);
                }
            }
        }
        if (newCount < 500 && data.hasThresholdReached(type)) {
            data.clearThresholdReached(type);
        }
    }

    /**
     * 激活罪孽状态快照（不可变）。
     */
    public static class SinFragmentState {
        /** 罪孽类型 */
        public final SinType sinType;
        /** 罪孽等级（1/2/3） */
        public final int level;

        public SinFragmentState(SinType sinType, int level) {
            this.sinType = sinType;
            this.level = level;
        }
    }
}