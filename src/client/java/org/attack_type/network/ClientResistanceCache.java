package org.attack_type.network;

import org.attack_type.api.ResistanceProfile;

/**
 * 客户端抗性配置缓存。
 * <p>
 * 存储从服务端同步的当前玩家抗性配置，供 GUI 和 HUD 渲染使用。
 * 使用静态单例，在 {@link NetworkHandlerClient} 收到同步包时更新。
 */
public class ClientResistanceCache {
    private static ResistanceProfile cachedProfile = new ResistanceProfile();

    /**
     * @return 当前缓存的抗性配置
     */
    public static ResistanceProfile getProfile() {
        return cachedProfile;
    }

    /**
     * 更新缓存（由网络包处理器调用）。
     *
     * @param profile 新的抗性配置
     */
    public static void setProfile(ResistanceProfile profile) {
        cachedProfile = profile;
    }
}