package org.attack_type.component;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局抗性管理器。
 * <p>
 * 维护所有实体的 {@link ResistanceProfile} 映射，负责：
 * <ul>
 *   <li>创建/获取/重置实体抗性配置</li>
 *   <li>玩家首次创建时随机化抗性</li>
 *   <li>非玩家实体周期性抗性衰减（每 2 个游戏天一次）</li>
 *   <li>每 4 次衰减后总乘积降低 0.01</li>
 * </ul>
 * <p>
 * 使用 {@link ConcurrentHashMap} 保证多线程安全。
 */
public class ResistanceManager {
    /** 所有实体的抗性配置映射表（UUID → 抗性） */
    private static final Map<UUID, ResistanceProfile> PROFILES = new ConcurrentHashMap<>();

    /** 抗性衰减间隔：48000 tick = 2 个 Minecraft 游戏天（40 分钟现实时间） */
    public static final long UPDATE_INTERVAL_TICKS = 24000L * 2;

    /**
     * 获取或创建实体的抗性配置。
     * 玩家首次创建时自动随机化抗性并归一化。
     *
     * @param entity 目标实体
     * @return 实体的抗性配置（非 null）
     */
    public static ResistanceProfile getProfile(LivingEntity entity) {
        return PROFILES.computeIfAbsent(entity.getUuid(), uuid -> {
            ResistanceProfile profile = new ResistanceProfile();
            if (entity instanceof PlayerEntity) {
                profile.randomizeResistances();
                profile.normalize();
            }
            return profile;
        });
    }

    /**
     * 直接设置实体的抗性配置（覆盖已有配置）。
     *
     * @param entity  目标实体
     * @param profile 新的抗性配置
     */
    public static void setProfile(LivingEntity entity, ResistanceProfile profile) {
        PROFILES.put(entity.getUuid(), profile);
    }

    /**
     * 移除实体的抗性配置（用于断线清理）。
     *
     * @param uuid 实体 UUID
     */
    public static void removeProfile(UUID uuid) {
        PROFILES.remove(uuid);
    }

    /**
     * 获取或创建实体的抗性配置（等同于 {@link #getProfile(LivingEntity)}）。
     */
    public static ResistanceProfile getOrCreateProfile(LivingEntity entity) {
        return getProfile(entity);
    }

    /**
     * 重置实体抗性：先归零所有抗性为 1.0，再重新随机化。
     */
    public static void resetProfile(LivingEntity entity) {
        ResistanceProfile profile = getProfile(entity);
        profile.reset();
        profile.randomizeResistances();
        profile.normalize();
    }

    /**
     * 将实体抗性数据同步到客户端。
     *
     * @param player 目标玩家
     */
    public static void syncToPlayer(ServerPlayerEntity player) {
        NetworkHandler.sendResistanceSync(player);
    }

    /**
     * 执行实体抗性周期衰减。
     * <p>
     * 仅在距上次更新 ≥ {@link #UPDATE_INTERVAL_TICKS} 时触发：
     * <ol>
     *   <li>随机化抗性分布</li>
     *   <li>归一化到当前总乘积</li>
     *   <li>每 4 个完整周期（8 游戏天），总乘积降低 0.01（最低 0.01）</li>
     * </ol>
     * 注意：玩家实体跳过此衰减（玩家抗性由 GUI 手动管理）。
     *
     * @param entity    目标实体
     * @param worldTime 当前世界 tick
     */
    public static void tickEntityResistance(LivingEntity entity, long worldTime) {
        if (entity instanceof PlayerEntity) {
            return;
        }

        ResistanceProfile profile = getProfile(entity);
        long ticksSinceUpdate = worldTime - profile.getLastUpdateTick();

        if (ticksSinceUpdate >= UPDATE_INTERVAL_TICKS) {
            long periods = ticksSinceUpdate / UPDATE_INTERVAL_TICKS;
            profile.randomizeResistances();
            profile.normalize();

            long fullCycles = periods / 4;
            if (fullCycles > 0) {
                float newProduct = profile.getTotalProduct() - fullCycles * 0.01f;
                profile.setTotalProduct(Math.max(0.01f, newProduct));
                profile.normalize();
            }

            profile.setLastUpdateTick(worldTime);
        }
    }
}