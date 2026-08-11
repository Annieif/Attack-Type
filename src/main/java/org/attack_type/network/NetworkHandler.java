package org.attack_type.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.attack_type.api.AttackType;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.component.ResistanceManager;
import org.attack_type.fragment.SinFragmentData;
import org.attack_type.fragment.SinFragmentManager;

/**
 * 服务端网络包处理器。
 * <p>
 * 注册并处理以下网络包：
 * <ul>
 *   <li>{@link ModPackets#RESISTANCE_UPDATE} — 接收客户端抗性修改，归一化后同步回客户端</li>
 *   <li>{@link ModPackets#FRAGMENT_TRIGGER} — 接收客户端手动触发请求，扣除碎片后同步</li>
 * </ul>
 * 同时提供主动推送方法：
 * <ul>
 *   <li>{@link #sendResistanceSync(ServerPlayerEntity)} — 推送抗性配置</li>
 *   <li>{@link #sendFragmentSync(ServerPlayerEntity)} — 推送碎片数据</li>
 * </ul>
 */
public class NetworkHandler {

    /**
     * 注册服务端网络包接收器。在模组初始化时调用。
     */
    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.RESISTANCE_UPDATE,
                (server, player, handler, buf, responseSender) -> {
                    ResistanceProfile profile = new ResistanceProfile();
                    for (AttackType type : AttackType.values()) {
                        if (type != AttackType.NONE) {
                            profile.setPhysicalResistance(type, buf.readFloat());
                        }
                    }
                    for (SinType type : SinType.values()) {
                        profile.setSinResistance(type, buf.readFloat());
                    }
                    profile.setTotalProduct(buf.readFloat());
                    profile.normalize();
                    server.execute(() -> {
                        ResistanceManager.setProfile(player, profile);
                        sendResistanceSync(player);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.FRAGMENT_TRIGGER,
                (server, player, handler, buf, responseSender) -> {
                    int sinOrdinal = buf.readInt();
                    int level = buf.readInt();
                    server.execute(() -> {
                        SinType sinType = SinType.values()[sinOrdinal];
                        boolean success = SinFragmentManager.tryTriggerSin(player, sinType, level);
                        sendFragmentSync(player);
                    });
                });
    }

    /**
     * 向客户端推送当前玩家的抗性配置（NBT 格式）。
     *
     * @param player 目标玩家
     */
    public static void sendResistanceSync(ServerPlayerEntity player) {
        ResistanceProfile profile = ResistanceManager.getProfile(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(profile.writeNbt(new net.minecraft.nbt.NbtCompound()));
        ServerPlayNetworking.send(player, ModPackets.RESISTANCE_SYNC, buf);
    }

    /**
     * 向客户端推送当前玩家的碎片数据（NBT 格式）。
     *
     * @param player 目标玩家
     */
    public static void sendFragmentSync(ServerPlayerEntity player) {
        SinFragmentData data = SinFragmentManager.getData(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data.writeNbt(new net.minecraft.nbt.NbtCompound()));
        ServerPlayNetworking.send(player, ModPackets.FRAGMENT_SYNC, buf);
    }
}