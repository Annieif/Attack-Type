package org.attack_type.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.fragment.ClientFragmentCache;
import org.attack_type.fragment.SinFragmentData;

public class NetworkHandlerClient {

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