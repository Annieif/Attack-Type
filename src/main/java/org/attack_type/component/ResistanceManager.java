package org.attack_type.component;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ResistanceManager {
    private static final Map<UUID, ResistanceProfile> PROFILES = new ConcurrentHashMap<>();
    public static final long UPDATE_INTERVAL_TICKS = 24000L * 2;

    public static ResistanceProfile getProfile(LivingEntity entity) {
        return PROFILES.computeIfAbsent(entity.getUuid(), uuid -> {
            ResistanceProfile profile = new ResistanceProfile();
            if (entity instanceof PlayerEntity) {
                profile.randomizeResistances();
                profile.normalize();
            }
            return profile;
        });
    }

    public static void setProfile(LivingEntity entity, ResistanceProfile profile) {
        PROFILES.put(entity.getUuid(), profile);
    }

    public static void removeProfile(UUID uuid) {
        PROFILES.remove(uuid);
    }

    public static ResistanceProfile getOrCreateProfile(LivingEntity entity) {
        return getProfile(entity);
    }

    public static void resetProfile(LivingEntity entity) {
        ResistanceProfile profile = getProfile(entity);
        profile.reset();
        profile.randomizeResistances();
        profile.normalize();
    }

    public static void syncToPlayer(ServerPlayerEntity player) {
        NetworkHandler.sendResistanceSync(player);
    }

    public static void tickEntityResistance(LivingEntity entity, long worldTime) {
        if (entity instanceof PlayerEntity) {
            return;
        }

        ResistanceProfile profile = getProfile(entity);
        long ticksSinceUpdate = worldTime - profile.getLastUpdateTick();

        if (ticksSinceUpdate >= UPDATE_INTERVAL_TICKS) {
            long periods = ticksSinceUpdate / UPDATE_INTERVAL_TICKS;
            profile.randomizeResistances();
            profile.normalize();

            long fullCycles = periods / 4;
            if (fullCycles > 0) {
                float newProduct = profile.getTotalProduct() - fullCycles * 0.01f;
                profile.setTotalProduct(Math.max(0.01f, newProduct));
                profile.normalize();
            }

            profile.setLastUpdateTick(worldTime);
        }
    }
}