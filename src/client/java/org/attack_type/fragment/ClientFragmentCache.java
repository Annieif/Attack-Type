package org.attack_type.fragment;

import org.attack_type.api.SinType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 客户端碎片数据缓存。
 * <p>
 * 客户端侧镜像 {@link SinFragmentData} 的静态缓存，由 {@link org.attack_type.network.NetworkHandlerClient}
 * 接收服务端同步包后更新，供 HUD 渲染使用。
 * <p>
 * 注意：客户端不执行碎片增减逻辑，仅被动接收服务端数据。
 */
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

    /**
     * @return 指定罪孽类型的碎片数量
     */
    public static int getFragments(SinType type) {
        return fragments.getOrDefault(type, 0);
    }

    /**
     * 设置碎片数量（由网络同步调用）。
     */
    public static void setFragments(SinType type, int count) {
        fragments.put(type, count);
    }

    /**
     * @return 当前激活的罪孽类型
     */
    public static SinType getActiveSinType() {
        return activeSinType;
    }

    /**
     * 设置激活罪孽类型（由网络同步或本地按键切换调用）。
     */
    public static void setActiveSinType(SinType type) {
        activeSinType = type;
    }

    /**
     * @return 当前激活罪孽等级
     */
    public static int getActiveSinLevel() {
        return activeSinLevel;
    }

    /**
     * 设置激活罪孽等级。
     */
    public static void setActiveSinLevel(int level) {
        activeSinLevel = level;
    }

    /**
     * @return 激活罪孽过期 tick
     */
    public static long getActiveSinExpiry() {
        return activeSinExpiry;
    }

    /**
     * 设置激活罪孽过期 tick。
     */
    public static void setActiveSinExpiry(long expiry) {
        activeSinExpiry = expiry;
    }
}