package org.attack_type;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import org.attack_type.component.ResistanceManager;
import org.attack_type.enchantment.ModEnchantments;
import org.attack_type.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Attack_type implements ModInitializer {
    public static final String MOD_ID = "attack_type";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEnchantments.initialize();
        NetworkHandler.registerServer();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.sendResistanceSync(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ResistanceManager.removeProfile(handler.player.getUuid());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                ResistanceManager.tickEntityResistance(player, server.getOverworld().getTime());
            });
            server.getWorlds().forEach(world -> {
                world.iterateEntities().forEach(entity -> {
                    if (entity instanceof LivingEntity living) {
                        ResistanceManager.tickEntityResistance(living, world.getTime());
                    }
                });
            });
        });

        LOGGER.info("Attack Type mod initialized!");
    }
}