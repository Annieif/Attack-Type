package org.attack_type.fragment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.attack_type.api.SinType;
import org.attack_type.enchantment.ModEnchantments;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SinFragmentManager {
    private static final Map<UUID, SinFragmentData> PLAYER_DATA = new ConcurrentHashMap<>();

    public static SinFragmentData getData(PlayerEntity player) {
        return PLAYER_DATA.computeIfAbsent(player.getUuid(), uuid -> new SinFragmentData());
    }

    public static void removeData(UUID uuid) {
        PLAYER_DATA.remove(uuid);
    }

    public static boolean tryTriggerSin(ServerPlayerEntity player, SinType sinType, int level) {
        SinFragmentData data = getData(player);

        int baseCost;
        int duration;
        switch (level) {
            case 1: baseCost = SinFragmentData.COST_LEVEL_1; duration = SinFragmentData.DURATION_L1_TICKS; break;
            case 2: baseCost = SinFragmentData.COST_LEVEL_2; duration = SinFragmentData.DURATION_L2_TICKS; break;
            case 3: baseCost = SinFragmentData.COST_LEVEL_3; duration = SinFragmentData.DURATION_L3_TICKS; break;
            default: return false;
        }

        Enchantment enchant = ModEnchantments.getSinEnchantment(sinType);
        int enchantLevel = enchant != null ? EnchantmentHelper.getLevel(enchant, player.getMainHandStack()) : 0;
        int cost = data.getCostWithEnchantment(baseCost, enchantLevel);

        if (!data.consumeFragments(sinType, cost)) {
            return false;
        }

        data.setActiveSinType(sinType);
        data.setActiveSinLevel(level);
        data.setActiveSinExpiry(player.getWorld().getTime() + duration);
        return true;
    }

    public static SinFragmentState getActiveSin(PlayerEntity player, long worldTime) {
        SinFragmentData data = getData(player);
        if (data.isSinActive(worldTime)) {
            return new SinFragmentState(data.getActiveSinType(), data.getActiveSinLevel());
        }
        return null;
    }

    public static void clearActiveSin(PlayerEntity player) {
        SinFragmentData data = getData(player);
        data.clearActiveSin();
    }

    public static int getFragmentCount(PlayerEntity player, SinType type) {
        return getData(player).getFragments(type);
    }

    public static void addFragments(PlayerEntity player, SinType type, int amount) {
        getData(player).addFragments(type, amount);
    }

    public static class SinFragmentState {
        public final SinType sinType;
        public final int level;

        public SinFragmentState(SinType sinType, int level) {
            this.sinType = sinType;
            this.level = level;
        }
    }
}