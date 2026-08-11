package org.attack_type.api;

import net.minecraft.nbt.NbtCompound;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ResistanceProfile {
    private static final Random RANDOM = new Random();

    private final Map<AttackType, Float> physicalResistances = new HashMap<>();
    private final Map<SinType, Float> sinResistances = new HashMap<>();
    private float totalProduct = 1.0f;
    private long lastUpdateTick = 0;

    public ResistanceProfile() {
        reset();
    }

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

    public float getPhysicalResistance(AttackType type) {
        return physicalResistances.getOrDefault(type, 1.0f);
    }

    public void setPhysicalResistance(AttackType type, float value) {
        if (type != AttackType.NONE) {
            physicalResistances.put(type, Math.max(0.0f, Math.min(5.0f, value)));
        }
    }

    public float getSinResistance(SinType type) {
        return sinResistances.getOrDefault(type, 1.0f);
    }

    public void setSinResistance(SinType type, float value) {
        sinResistances.put(type, Math.max(0.0f, Math.min(5.0f, value)));
    }

    public float getTotalProduct() {
        return totalProduct;
    }

    public void setTotalProduct(float value) {
        this.totalProduct = Math.max(0.0f, value);
    }

    public long getLastUpdateTick() {
        return lastUpdateTick;
    }

    public void setLastUpdateTick(long tick) {
        this.lastUpdateTick = tick;
    }

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

    private float clamp(float value) {
        return Math.round(Math.max(0.01f, Math.min(5.0f, value)) * 100.0f) / 100.0f;
    }

    private float getValueByIndex(int index) {
        if (index < 3) {
            return physicalResistances.getOrDefault(AttackType.values()[index], 1.0f);
        }
        return sinResistances.getOrDefault(SinType.values()[index - 3], 1.0f);
    }

    private void setValueByIndex(int index, float value) {
        if (index < 3) {
            physicalResistances.put(AttackType.values()[index], value);
        } else {
            sinResistances.put(SinType.values()[index - 3], value);
        }
    }

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

    public static ResistanceProfile readNbt(NbtCompound nbt) {
        ResistanceProfile profile = new ResistanceProfile();
        NbtCompound physNbt = nbt.getCompound("physResist");
        for (AttackType type : AttackType.values()) {
            if (type != AttackType.NONE && physNbt.contains(type.name())) {
                profile.physicalResistances.put(type, physNbt.getFloat(type.name()));
            }
        }
        NbtCompound sinNbt = nbt.getCompound("sinResist");
        for (SinType type : SinType.values()) {
            if (sinNbt.contains(type.name())) {
                profile.sinResistances.put(type, sinNbt.getFloat(type.name()));
            }
        }
        if (nbt.contains("totalProduct")) {
            profile.totalProduct = nbt.getFloat("totalProduct");
        }
        if (nbt.contains("lastUpdateTick")) {
            profile.lastUpdateTick = nbt.getLong("lastUpdateTick");
        }
        return profile;
    }

    public String getResistanceLabel(AttackType type) {
        float value = getPhysicalResistance(type);
        if (value > 1.5f) return "致命";
        if (value > 1.0f) return "脆弱";
        if (value == 1.0f) return "一般";
        if (value > 0.5f) return "耐性";
        return "抵抗";
    }

    public String getResistanceLabel(SinType type) {
        float value = getSinResistance(type);
        if (value > 1.5f) return "致命";
        if (value > 1.0f) return "脆弱";
        if (value == 1.0f) return "一般";
        if (value > 0.5f) return "耐性";
        return "抵抗";
    }
}