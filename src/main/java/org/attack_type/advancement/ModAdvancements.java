package org.attack_type.advancement;

import net.minecraft.advancement.Advancement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.attack_type.api.SinType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模组进度管理工具。
 * <p>
 * 所有进度使用 {@code minecraft:impossible} 触发器，由代码手动授予。
 * 调用 {@link #grant(ServerPlayerEntity, String)} 即可触发进度。
 * <p>
 * 进度树结构：
 * <pre>
 * root
 * ├── first_sin     — 首次触发罪孽攻击
 * ├── sin_master    — 触发全部 7 种罪孽
 * │   └── sin_collector — 全部 7 种罪孽碎片 ≥100
 * ├── overflow      — 首次碎片溢出（≥500）
 * │   └── instant_kill  — 首次即死（≥1000）
 * └── sin_addict    — 累计碎片 ≥1000
 * </pre>
 */
public class ModAdvancements {
    private static final String NAMESPACE = "attack_type";

    public static final Identifier ROOT = id("root");
    public static final Identifier FIRST_SIN = id("first_sin");
    public static final Identifier SIN_MASTER = id("sin_master");
    public static final Identifier OVERFLOW = id("overflow");
    public static final Identifier INSTANT_KILL = id("instant_kill");
    public static final Identifier SIN_ADDICT = id("sin_addict");
    public static final Identifier SIN_COLLECTOR = id("sin_collector");

    private static final Map<UUID, Set<SinType>> TRIGGERED_SIN_TYPES = new ConcurrentHashMap<>();

    private static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }

    /**
     * 授予玩家指定进度。
     *
     * @param player 目标玩家
     * @param id     进度 ID
     */
    public static void grant(ServerPlayerEntity player, Identifier id) {
        Advancement advancement = player.getServer()
                .getAdvancementLoader().get(id);
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "impossible");
        }
    }

    /**
     * 检查玩家是否已完成指定进度。
     */
    public static boolean has(ServerPlayerEntity player, Identifier id) {
        Advancement advancement = player.getServer()
                .getAdvancementLoader().get(id);
        if (advancement == null) return false;
        return player.getAdvancementTracker().getProgress(advancement).isDone();
    }

    /**
     * 记录玩家触发过的罪孽类型，并检查 first_sin/sin_master 进度。
     */
    public static void recordSinTrigger(ServerPlayerEntity player, SinType type) {
        Set<SinType> types = TRIGGERED_SIN_TYPES.computeIfAbsent(player.getUuid(),
                k -> Collections.synchronizedSet(new HashSet<>()));
        types.add(type);
        checkFirstSin(player);
        checkSinMaster(player, types);
    }

    /**
     * 移除玩家的触发记录（断线清理）。
     */
    public static void removeRecord(UUID uuid) {
        TRIGGERED_SIN_TYPES.remove(uuid);
    }

    /**
     * 检查并授予首次罪孽触发进度。
     */
    public static void checkFirstSin(ServerPlayerEntity player) {
        if (!has(player, FIRST_SIN)) {
            grant(player, FIRST_SIN);
        }
    }

    /**
     * 检查并授予罪孽大师进度（全部 7 种罪孽）。
     *
     * @param triggeredTypes 已触发过的罪孽类型集合
     */
    public static void checkSinMaster(ServerPlayerEntity player, java.util.Set<SinType> triggeredTypes) {
        if (!has(player, SIN_MASTER) && triggeredTypes.size() >= 7) {
            grant(player, SIN_MASTER);
        }
    }

    /**
     * 检查并授予碎片溢出进度。
     */
    public static void checkOverflow(ServerPlayerEntity player) {
        if (!has(player, OVERFLOW)) {
            grant(player, OVERFLOW);
        }
    }

    /**
     * 检查并授予即死进度。
     */
    public static void checkInstantKill(ServerPlayerEntity player) {
        if (!has(player, INSTANT_KILL)) {
            grant(player, INSTANT_KILL);
        }
    }

    /**
     * 检查并授予碎片成瘾进度（累计 ≥1000）。
     */
    public static void checkSinAddict(ServerPlayerEntity player, int totalFragments) {
        if (!has(player, SIN_ADDICT) && totalFragments >= 1000) {
            grant(player, SIN_ADDICT);
        }
    }

    /**
     * 检查并授予碎片收藏家进度（全部 7 种 ≥100）。
     */
    public static void checkSinCollector(ServerPlayerEntity player, int[] fragmentCounts) {
        if (!has(player, SIN_COLLECTOR)) {
            for (int count : fragmentCounts) {
                if (count < 100) return;
            }
            grant(player, SIN_COLLECTOR);
        }
    }
}