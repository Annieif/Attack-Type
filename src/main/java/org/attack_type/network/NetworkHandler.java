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

public class NetworkHandler {

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

    public static void sendResistanceSync(ServerPlayerEntity player) {
        ResistanceProfile profile = ResistanceManager.getProfile(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(profile.writeNbt(new net.minecraft.nbt.NbtCompound()));
        ServerPlayNetworking.send(player, ModPackets.RESISTANCE_SYNC, buf);
    }

    public static void sendFragmentSync(ServerPlayerEntity player) {
        SinFragmentData data = SinFragmentManager.getData(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(data.writeNbt(new net.minecraft.nbt.NbtCompound()));
        ServerPlayNetworking.send(player, ModPackets.FRAGMENT_SYNC, buf);
    }
}