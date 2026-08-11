package org.attack_type.api;

import net.minecraft.nbt.NbtCompound;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 实体抗性配置数据模型。
 * <p>
 * 存储单个实体对 3 种物理攻击类型和 7 种罪孽属性的抗性乘数（范围 ≥0.01，无上限）。
 * 乘积 {@code totalProduct} 约束所有抗性乘数的几何平均值，确保抗性总体平衡。
 * <p>
 * 支持 NBT 序列化，用于持久化存储和网络同步。
 *
 * <h3>抗性等级参考</h3>
 * <table>
 *   <tr><th>等级</th><th>乘数</th><th>说明</th></tr>
 *   <tr><td>致命</td><td>&gt; 1.5</td><td>受到该类型伤害大幅增加</td></tr>
 *   <tr><td>脆弱</td><td>&gt; 1.0 (非 1.0)</td><td>受到该类型伤害增加</td></tr>
 *   <tr><td>一般</td><td>= 1.0</td><td>正常伤害</td></tr>
 *   <tr><td>耐性</td><td>&lt; 1.0</td><td>受到该类型伤害减少</td></tr>
 *   <tr><td>抵抗</td><td>&le; 0.5</td><td>受到该类型伤害大幅减少</td></tr>
 *   <tr><td>免疫</td><td>= 0.0</td><td>完全免疫该类型伤害</td></tr>
 * </table>
 */
public class ResistanceProfile {
    private static final Random RANDOM = new Random();

    private final Map<AttackType, Float> physicalResistances = new HashMap<>();
    private final Map<SinType, Float> sinResistances = new HashMap<>();
    private float totalProduct = 1.0f;
    private long lastUpdateTick = 0;

    /**
     * 创建默认抗性配置（所有抗性 = 1.0，总积 = 1.0）。
     */
    public ResistanceProfile() {
        reset();
    }

    /**
     * 重置所有抗性乘数为 1.0（正常伤害），总积为 1.0。
     */
    public void reset() {
        for (AttackType type : AttackType.values()) {
            if (type != AttackType.NONE) {
                physicalResistances.put(type, 1.0f);
            }
        }
        for (SinType type : SinType.values()) {
            sinResistances.put(type, 1.0f);
        }
        totalProduct = 1.0f;
    }

    /**
     * 获取指定物理攻击类型的抗性乘数。
     *
     * @param type 攻击类型（SLASH/PIERCE/BLUNT）
     * @return 抗性乘数，默认 1.0
     */
    public float getPhysicalResistance(AttackType type) {
        return physicalResistances.getOrDefault(type, 1.0f);
    }

    /**
     * 设置指定物理攻击类型的抗性乘数（自动钳制到 ≥0.0）。
     *
     * @param type  攻击类型
     * @param value 抗性乘数（负值自动钳制为 0）
     */
    public void setPhysicalResistance(AttackType type, float value) {
        if (type != AttackType.NONE) {
            physicalResistances.put(type, Math.max(0.0f, value));
        }
    }

    /**
     * 获取指定罪孽类型的抗性乘数。
     *
     * @param type 罪孽类型
     * @return 抗性乘数，默认 1.0
     */
    public float getSinResistance(SinType type) {
        return sinResistances.getOrDefault(type, 1.0f);
    }

    /**
     * 设置指定罪孽类型的抗性乘数（自动钳制到 ≥0.0）。
     *
     * @param type  罪孽类型
     * @param value 抗性乘数（负值自动钳制为 0）
     */
    public void setSinResistance(SinType type, float value) {
        sinResistances.put(type, Math.max(0.0f, value));
    }

    /**
     * @return 所有物理+罪孽抗性乘数的总乘积
     */
    public float getTotalProduct() {
        return totalProduct;
    }

    /**
     * 设置总乘积约束值（不低于 0.0）。
     *
     * @param value 总乘积
     */
    public void setTotalProduct(float value) {
        this.totalProduct = Math.max(0.0f, value);
    }

    /**
     * @return 上次抗性衰减的游戏 tick
     */
    public long getLastUpdateTick() {
        return lastUpdateTick;
    }

    /**
     * @param tick 当前游戏 tick
     */
    public void setLastUpdateTick(long tick) {
        this.lastUpdateTick = tick;
    }

    /**
     * 随机化所有抗性值。
     * <p>
     * 算法：随机交换 10 种抗性（3 物理 + 7 罪孽）中的值对，共执行 30 次交换。
     * 每次交换对两个值乘以一个随机因子（0.8~1.2），使总乘积保持近似不变。
     */
    public void randomizeResistances() {
        int count = 3 + 7;
        for (int i = 0; i < count * 3; i++) {
            int idx1 = RANDOM.nextInt(count);
            int idx2 = RANDOM.nextInt(count);
            if (idx1 == idx2) continue;

            float v1 = getValueByIndex(idx1);
            float v2 = getValueByIndex(idx2);

            float factor = 0.8f + RANDOM.nextFloat() * 0.4f;
            float newV1 = v1 * factor;
            float newV2 = v2 / factor;

            newV1 = clamp(newV1);
            newV2 = clamp(newV2);

            setValueByIndex(idx1, newV1);
            setValueByIndex(idx2, newV2);
        }
    }

    /**
     * 归一化所有抗性值，使其乘积趋近于 {@code totalProduct}。
     * <p>
     * 使用几何平均缩放：每个抗性值乘以 {@code (totalProduct / 当前乘积)^(1/10)}。
     * 仅在乘积偏差超过 0.001 时执行。
     */
    public void normalize() {
        float product = 1.0f;
        for (AttackType type : AttackType.values()) {
            if (type != AttackType.NONE) {
                product *= physicalResistances.getOrDefault(type, 1.0f);
            }
        }
        for (SinType type : SinType.values()) {
            product *= sinResistances.getOrDefault(type, 1.0f);
        }

        if (product > 0 && Math.abs(product - totalProduct) > 0.001f) {
            float ratio = (float) Math.pow(totalProduct / product, 1.0 / 10.0);
            for (AttackType type : AttackType.values()) {
                if (type != AttackType.NONE) {
                    physicalResistances.computeIfPresent(type, (k, v) -> clamp(v * ratio));
                }
            }
            for (SinType type : SinType.values()) {
                sinResistances.computeIfPresent(type, (k, v) -> clamp(v * ratio));
            }
        }
    }

    /**
     * 钳制值到 ≥0.01 并四舍五入到 2 位小数（无上界）。
     */
    private float clamp(float value) {
        return Math.round(Math.max(0.01f, value) * 100.0f) / 100.0f;
    }

    /**
     * 按统一索引获取抗性值（前 3 个为物理，后 7 个为罪孽）。
     */
    private float getValueByIndex(int index) {
        if (index < 3) {
            return physicalResistances.getOrDefault(AttackType.values()[index], 1.0f);
        }
        return sinResistances.getOrDefault(SinType.values()[index - 3], 1.0f);
    }

    /**
     * 按统一索引设置抗性值。
     */
    private void setValueByIndex(int index, float value) {
        if (index < 3) {
            physicalResistances.put(AttackType.values()[index], value);
        } else {
            sinResistances.put(SinType.values()[index - 3], value);
        }
    }

    /**
     * 将抗性数据序列化为 NBT。
     *
     * @param nbt 目标 NBT 复合标签
     * @return 传入的 nbt（链式调用）
     */
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound physNbt = new NbtCompound();
        for (Map.Entry<AttackType, Float> entry : physicalResistances.entrySet()) {
            physNbt.putFloat(entry.getKey().name(), entry.getValue());
        }
        nbt.put("physResist", physNbt);

        NbtCompound sinNbt = new NbtCompound();
        for (Map.Entry<SinType, Float> entry : sinResistances.entrySet()) {
            sinNbt.putFloat(entry.getKey().name(), entry.getValue());
        }
        nbt.put("sinResist", sinNbt);

        nbt.putFloat("totalProduct", totalProduct);
        nbt.putLong("lastUpdateTick", lastUpdateTick);
        return nbt;
    }

    /**
     * 根据抗性值返回对应的等级标签键。
     *
     * @param value 抗性乘数
     * @return 国际化标签键（如 "resistance.label.fatal"）
     */
    public static String getResistanceLabel(float value) {
        if (value > 1.5f) return "resistance.label.fatal";
        if (value > 1.0f) return "resistance.label.vulnerable";
        if (value == 1.0f) return "resistance.label.normal";
        if (value > 0.5f) return "resistance.label.tough";
        if (value > 0.0f) return "resistance.label.resist";
        return "resistance.label.immune";
    }

    /**
     * 根据物理攻击类型获取抗性等级标签。
     *
     * @param type 攻击类型
     * @return 国际化标签键
     */
    public String getResistanceLabel(AttackType type) {
        return getResistanceLabel(getPhysicalResistance(type));
    }

    /**
     * 根据罪孽类型获取抗性等级标签。
     *
     * @param type 罪孽类型
     * @return 国际化标签键
     */
    public String getResistanceLabel(SinType type) {
        return getResistanceLabel(getSinResistance(type));
    }

    /**
     * 从 NBT 反序列化抗性配置。
     *
     * @param nbt 包含抗性数据的 NBT 复合标签
     * @return 新的 ResistanceProfile 实例
     */
    public static ResistanceProfile readNbt(NbtCompound nbt) {
        ResistanceProfile profile = new ResistanceProfile();
        if (nbt.contains("physResist")) {
            NbtCompound physNbt = nbt.getCompound("physResist");
            for (String key : physNbt.getKeys()) {
                AttackType type = AttackType.fromString(key);
                if (type != AttackType.NONE) {
                    profile.setPhysicalResistance(type, physNbt.getFloat(key));
                }
            }
        }
        if (nbt.contains("sinResist")) {
            NbtCompound sinNbt = nbt.getCompound("sinResist");
            for (String key : sinNbt.getKeys()) {
                try {
                    SinType type = SinType.valueOf(key);
                    profile.setSinResistance(type, sinNbt.getFloat(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (nbt.contains("totalProduct")) {
            profile.setTotalProduct(nbt.getFloat("totalProduct"));
        }
        if (nbt.contains("lastUpdateTick")) {
            profile.setLastUpdateTick(nbt.getLong("lastUpdateTick"));
        }
        return profile;
    }
}