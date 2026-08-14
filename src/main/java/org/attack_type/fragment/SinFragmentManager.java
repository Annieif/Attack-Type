package org.attack_type.fragment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.attack_type.advancement.ModAdvancements;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.component.ResistanceManager;
import org.attack_type.config.ModConfig;
import org.attack_type.enchantment.ModEnchantments;
import org.attack_type.network.NetworkHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
 * <h3>线程安全</h3>
 * <p>
 * 使用 {@link ConcurrentHashMap} + 对象级 {@code synchronized} 锁。
 * 关键操作（addFragments、tryTriggerSin）在玩家数据对象上加锁，保证：
 * <ul>
 *   <li>阈值检查→标记→总积衰减 原子执行</li>
 *   <li>碎片检查→消耗→设置激活状态 原子执行</li>
 * </ul>
 * <h3>网络优化</h3>
 * <p>
 * 使用脏标记机制合并碎片同步：多次碎片变更只触发一次网络推送（每 tick 最多一次）。
 */
public class SinFragmentManager {
    /** 所有在线玩家的碎片数据（UUID → 数据） */
    private static final Map<UUID, SinFragmentData> PLAYER_DATA = new ConcurrentHashMap<>();

    /** 需要同步碎片数据的玩家集合（脏标记，用于批量同步） */
    private static final Set<UUID> DIRTY_PLAYERS = Collections.synchronizedSet(new HashSet<>());

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
        DIRTY_PLAYERS.remove(uuid);
    }

    /**
     * 直接设置玩家的碎片数据（用于 NBT 恢复）。
     *
     * @param uuid 玩家 UUID
     * @param data 碎片数据
     */
    public static void setData(UUID uuid, SinFragmentData data) {
        PLAYER_DATA.put(uuid, data);
    }

    /**
     * 尝试手动触发罪孽攻击。
     * <p>
     * 消耗公式：{@code max(1, baseCost(level) - 2 × enchantLevel)}。
     * 成功触发后设置激活罪孽类型、等级和过期时间。
     * <p>
     * 在数据对象上加锁，保证检查→消耗→设置激活状态原子执行。
     *
     * @param player  目标玩家
     * @param sinType 罪孽类型
     * @param level   触发等级（1/2/3）
     * @return true 表示触发成功（碎片充足），false 表示碎片不足
     */
    public static boolean tryTriggerSin(ServerPlayerEntity player, SinType sinType, int level) {
        SinFragmentData data = getData(player);
        synchronized (data) {
            int baseCost;
            int duration;
            switch (level) {
                case 1: baseCost = ModConfig.COST_LEVEL_1; duration = ModConfig.DURATION_L1_TICKS; break;
                case 2: baseCost = ModConfig.COST_LEVEL_2; duration = ModConfig.DURATION_L2_TICKS; break;
                case 3: baseCost = ModConfig.COST_LEVEL_3; duration = ModConfig.DURATION_L3_TICKS; break;
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
        }
        markDirty(player.getUuid());
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
        synchronized (data) {
            if (data.isSinActive(worldTime)) {
                return new SinFragmentState(data.getActiveSinType(), data.getActiveSinLevel());
            }
        }
        return null;
    }

    /**
     * 清除玩家激活罪孽状态。
     */
    public static void clearActiveSin(PlayerEntity player) {
        SinFragmentData data = getData(player);
        synchronized (data) {
            data.clearActiveSin();
        }
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
     * <p>
     * 在数据对象上加锁，保证阈值检查→标记→总积衰减原子执行。
     */
    public static void addFragments(PlayerEntity player, SinType type, int amount) {
        SinFragmentData data = getData(player);
        synchronized (data) {
            data.addFragments(type, amount);

            int newCount = data.getFragments(type);
            if (newCount >= ModConfig.OVERFLOW_THRESHOLD && !data.hasThresholdReached(type)) {
                data.markThresholdReached(type);
                if (player instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) player;
                    ResistanceProfile profile = ResistanceManager.getOrCreateProfile(living);
                    float newProduct = (float) Math.max(ModConfig.TOTAL_PRODUCT_MIN, profile.getTotalProduct() - ModConfig.TOTAL_PRODUCT_DECAY_RATE);
                    profile.setTotalProduct(newProduct);
                    profile.normalize();
                    if (living instanceof ServerPlayerEntity) {
                        ServerPlayerEntity sp = (ServerPlayerEntity) living;
                        ResistanceManager.syncToPlayer(sp);
                        sp.sendMessage(Text.translatable("cmd.attack_type.total_product_decay", String.format("%.1f", newProduct)), true);
                        ModAdvancements.grant(sp, ModAdvancements.OVERFLOW);
                    }
                }
            }
            if (newCount < ModConfig.OVERFLOW_THRESHOLD && data.hasThresholdReached(type)) {
                data.clearThresholdReached(type);
            }

            if (player instanceof ServerPlayerEntity sp) {
                int total = data.getTotalFragments();
                ModAdvancements.checkSinAddict(sp, total);
                ModAdvancements.checkSinCollector(sp, data.getAllFragmentCounts());
            }
        }
        markDirty(player.getUuid());
    }

    /**
     * 标记玩家碎片数据为脏，等待批量同步。
     */
    private static void markDirty(UUID uuid) {
        DIRTY_PLAYERS.add(uuid);
    }

    /**
     * 刷新所有脏玩家的碎片数据到客户端。
     * 在 {@code ServerTickEvents.END_SERVER_TICK} 中调用，每 tick 最多同步一次。
     */
    public static void flushDirtyPlayers(ServerWorld world) {
        if (DIRTY_PLAYERS.isEmpty()) {
            return;
        }
        Set<UUID> toSync;
        synchronized (DIRTY_PLAYERS) {
            toSync = new HashSet<>(DIRTY_PLAYERS);
            DIRTY_PLAYERS.clear();
        }
        for (UUID uuid : toSync) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                NetworkHandler.sendFragmentSync(player);
            }
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