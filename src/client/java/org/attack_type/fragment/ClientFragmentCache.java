package org.attack_type.fragment;

import org.attack_type.api.SinType;

import java.util.EnumMap;
import java.util.Map;

public class ClientFragmentCache {
    private static final Map<SinType, Integer> fragments = new EnumMap<>(SinType.class);
    private static SinType activeSinType = SinType.WRATH;
    private static int activeSinLevel = 0;
    private static long activeSinExpiry = 0;

    static {
        for (SinType type : SinType.values()) {
            fragments.put(type, 0);
        }
    }

    public static int getFragments(SinType type) {
        return fragments.getOrDefault(type, 0);
    }

    public static void setFragments(SinType type, int count) {
        fragments.put(type, count);
    }

    public static SinType getActiveSinType() {
        return activeSinType;
    }

    public static void setActiveSinType(SinType type) {
        activeSinType = type;
    }

    public static int getActiveSinLevel() {
        return activeSinLevel;
    }

    public static void setActiveSinLevel(int level) {
        activeSinLevel = level;
    }

    public static long getActiveSinExpiry() {
        return activeSinExpiry;
    }

    public static void setActiveSinExpiry(long expiry) {
        activeSinExpiry = expiry;
    }
}