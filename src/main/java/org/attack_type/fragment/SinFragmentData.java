package org.attack_type.fragment;

import net.minecraft.nbt.NbtCompound;
import org.attack_type.api.SinType;

import java.util.EnumMap;
import java.util.Map;

public class SinFragmentData {
    private final Map<SinType, Integer> fragments = new EnumMap<>(SinType.class);
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

    public SinFragmentData() {
        for (SinType type : SinType.values()) {
            fragments.put(type, 0);
        }
    }

    public int getFragments(SinType type) {
        return fragments.getOrDefault(type, 0);
    }

    public void setFragments(SinType type, int count) {
        fragments.put(type, Math.max(0, count));
    }

    public void addFragments(SinType type, int amount) {
        fragments.merge(type, amount, Integer::sum);
    }

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

    public boolean isSinActive(long worldTime) {
        return activeSinLevel > 0 && worldTime < activeSinExpiry;
    }

    public void clearActiveSin() {
        activeSinLevel = 0;
        activeSinExpiry = 0;
    }

    public boolean isOverflowing(SinType type) {
        return getFragments(type) >= OVERFLOW_THRESHOLD;
    }

    public boolean shouldKill(SinType type) {
        return getFragments(type) >= KILL_THRESHOLD;
    }

    public int getCostWithEnchantment(int baseCost, int enchantLevel) {
        return Math.max(1, baseCost - enchantLevel * 2);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        for (SinType type : SinType.values()) {
            nbt.putInt("frag_" + type.name(), getFragments(type));
        }
        nbt.putString("activeSin", activeSinType.name());
        nbt.putInt("activeSinLevel", activeSinLevel);
        nbt.putLong("activeSinExpiry", activeSinExpiry);
        return nbt;
    }

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