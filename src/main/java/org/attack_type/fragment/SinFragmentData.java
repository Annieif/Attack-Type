package org.attack_type.fragment;

import net.minecraft.nbt.NbtCompound;
import org.attack_type.api.SinType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 罪孽碎片数据模型（单个玩家）。
 * <p>
 * 存储 7 种罪孽碎片计数、激活罪孽状态和触发消耗常量。
 *
 * <h3>关键常量</h3>
 * <table>
 *   <tr><th>常量</th><th>值</th><th>说明</th></tr>
 *   <tr><td>{@link #COST_LEVEL_1}</td><td>40</td><td>L1 触发消耗碎片</td></tr>
 *   <tr><td>{@link #COST_LEVEL_2}</td><td>70</td><td>L2 触发消耗碎片</td></tr>
 *   <tr><td>{@link #COST_LEVEL_3}</td><td>100</td><td>L3 触发消耗碎片</td></tr>
 *   <tr><td>{@link #OVERFLOW_THRESHOLD}</td><td>500</td><td>溢出阈值（自动触发）</td></tr>
 *   <tr><td>{@link #KILL_THRESHOLD}</td><td>1000</td><td>即死阈值</td></tr>
 *   <tr><td>{@link #DURATION_L1_TICKS}</td><td>80</td><td>L1 激活持续（4 秒）</td></tr>
 *   <tr><td>{@link #DURATION_L2_TICKS}</td><td>140</td><td>L2 激活持续（7 秒）</td></tr>
 *   <tr><td>{@link #DURATION_L3_TICKS}</td><td>200</td><td>L3 激活持续（10 秒）</td></tr>
 * </table>
 *
 * <h3>附魔减耗</h3>
 * 实际消耗 = max(1, baseCost - 2 × enchantLevel)，见 {@link #getCostWithEnchantment(int, int)}。
 */
public class SinFragmentData {
    private final Map<SinType, Integer> fragments = new EnumMap<>(SinType.class);
    private final Set<SinType> thresholdReachedFragments = EnumSet.noneOf(SinType.class);
    private SinType activeSinType = SinType.WRATH;
    private int activeSinLevel = 0;
    private long activeSinExpiry = 0;

    public static final int COST_LEVEL_1 = 40;
    public static final int COST_LEVEL_2 = 70;
    public static final int COST_LEVEL_3 = 100;
    public static final int OVERFLOW_THRESHOLD = 500;
    public static final int KILL_THRESHOLD = 1000;

    public static final int DURATION_L1_TICKS = 80;
    public static final int DURATION_L2_TICKS = 140;
    public static final int DURATION_L3_TICKS = 200;

    /**
     * 创建碎片数据，所有碎片初始化为 0。
     */
    public SinFragmentData() {
        for (SinType type : SinType.values()) {
            fragments.put(type, 0);
        }
    }

    /**
     * 获取指定罪孽类型的碎片数量。
     */
    public int getFragments(SinType type) {
        return fragments.getOrDefault(type, 0);
    }

    /**
     * 设置指定罪孽类型的碎片数量（最小 0）。
     */
    public void setFragments(SinType type, int count) {
        fragments.put(type, Math.max(0, count));
    }

    /**
     * 增加指定罪孽类型的碎片数量（可负值）。
     */
    public void addFragments(SinType type, int amount) {
        fragments.merge(type, amount, Integer::sum);
    }

    /**
     * 消耗指定数量的碎片。
     *
     * @param type   罪孽类型
     * @param amount 消耗数量
     * @return true 表示消耗成功，false 表示碎片不足
     */
    public boolean consumeFragments(SinType type, int amount) {
        int current = getFragments(type);
        if (current < amount) return false;
        fragments.put(type, current - amount);
        return true;
    }

    public SinType getActiveSinType() {
        return activeSinType;
    }

    public void setActiveSinType(SinType type) {
        this.activeSinType = type;
    }

    public int getActiveSinLevel() {
        return activeSinLevel;
    }

    public void setActiveSinLevel(int level) {
        this.activeSinLevel = level;
    }

    public long getActiveSinExpiry() {
        return activeSinExpiry;
    }

    public void setActiveSinExpiry(long expiry) {
        this.activeSinExpiry = expiry;
    }

    /**
     * 检查激活罪孽是否仍在有效期内。
     *
     * @param worldTime 当前世界 tick
     * @return true 表示激活状态有效
     */
    public boolean isSinActive(long worldTime) {
        return activeSinLevel > 0 && worldTime < activeSinExpiry;
    }

    /**
     * 清除激活罪孽状态。
     */
    public void clearActiveSin() {
        activeSinLevel = 0;
        activeSinExpiry = 0;
    }

    /**
     * 检查碎片是否达到溢出阈值（≥500）。
     */
    public boolean isOverflowing(SinType type) {
        return getFragments(type) >= OVERFLOW_THRESHOLD;
    }

    /**
     * 检查碎片是否达到即死阈值（≥1000）。
     */
    public boolean shouldKill(SinType type) {
        return getFragments(type) >= KILL_THRESHOLD;
    }

    /**
     * 检查该罪孽碎片是否已触发过 500 阈值（用于总积衰减）。
     */
    public boolean hasThresholdReached(SinType type) {
        return thresholdReachedFragments.contains(type);
    }

    /**
     * 标记该罪孽碎片已触发 500 阈值。
     */
    public void markThresholdReached(SinType type) {
        thresholdReachedFragments.add(type);
    }

    /**
     * 清除该罪孽碎片的阈值触发标记（碎片降至 500 以下时调用）。
     */
    public void clearThresholdReached(SinType type) {
        thresholdReachedFragments.remove(type);
    }

    /**
     * 计算附魔减免后的实际消耗。
     *
     * @param baseCost     基础消耗
     * @param enchantLevel 罪孽附魔等级
     * @return 实际消耗 = max(1, baseCost - 2 × enchantLevel)
     */
    public int getCostWithEnchantment(int baseCost, int enchantLevel) {
        return Math.max(1, baseCost - enchantLevel * 2);
    }

    /**
     * 序列化到 NBT。
     */
    public NbtCompound writeNbt(NbtCompound nbt) {
        for (SinType type : SinType.values()) {
            nbt.putInt("frag_" + type.name(), getFragments(type));
        }
        nbt.putString("activeSin", activeSinType.name());
        nbt.putInt("activeSinLevel", activeSinLevel);
        nbt.putLong("activeSinExpiry", activeSinExpiry);
        return nbt;
    }

    /**
     * 从 NBT 反序列化。
     */
    public void readNbt(NbtCompound nbt) {
        for (SinType type : SinType.values()) {
            fragments.put(type, nbt.getInt("frag_" + type.name()));
        }
        String sinName = nbt.getString("activeSin");
        try {
            activeSinType = SinType.valueOf(sinName);
        } catch (IllegalArgumentException e) {
            activeSinType = SinType.WRATH;
        }
        activeSinLevel = nbt.getInt("activeSinLevel");
        activeSinExpiry = nbt.getLong("activeSinExpiry");
    }
}