package org.attack_type.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.fragment.ClientFragmentCache;
import org.attack_type.fragment.SinFragmentData;

/**
 * 客户端网络包处理器。
 * <p>
 * 注册并处理服务端推送的网络包：
 * <ul>
 *   <li>{@link ModPackets#RESISTANCE_SYNC} — 接收抗性配置，更新 {@link ClientResistanceCache}</li>
 *   <li>{@link ModPackets#FRAGMENT_SYNC} — 接收碎片数据，更新 {@link ClientFragmentCache}</li>
 * </ul>
 */
public class NetworkHandlerClient {

    /**
     * 注册客户端网络包接收器。在客户端初始化时调用。
     */
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.RESISTANCE_SYNC,
                (client, handler, buf, responseSender) -> {
                    ResistanceProfile profile = ResistanceProfile.readNbt(buf.readNbt());
                    client.execute(() -> {
                        ClientResistanceCache.setProfile(profile);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.FRAGMENT_SYNC,
                (client, handler, buf, responseSender) -> {
                    NbtCompound nbt = buf.readNbt();
                    client.execute(() -> {
                        for (SinType type : SinType.values()) {
                            ClientFragmentCache.setFragments(type, nbt.getInt("frag_" + type.name()));
                        }
                        String sinName = nbt.getString("activeSin");
                        try {
                            ClientFragmentCache.setActiveSinType(SinType.valueOf(sinName));
                        } catch (IllegalArgumentException ignored) {}
                        ClientFragmentCache.setActiveSinLevel(nbt.getInt("activeSinLevel"));
                        ClientFragmentCache.setActiveSinExpiry(nbt.getLong("activeSinExpiry"));
                    });
                });
    }
}