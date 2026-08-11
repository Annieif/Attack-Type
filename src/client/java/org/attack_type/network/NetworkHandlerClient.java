package org.attack_type.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.attack_type.api.ResistanceProfile;

public class NetworkHandlerClient {

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.RESISTANCE_SYNC,
                (client, handler, buf, responseSender) -> {
                    ResistanceProfile profile = ResistanceProfile.readNbt(buf.readNbt());
                    client.execute(() -> {
                        ClientResistanceCache.setProfile(profile);
                    });
                });
    }
}