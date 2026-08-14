package org.attack_type;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import org.attack_type.command.ResistanceCommand;
import org.attack_type.component.ResistanceManager;
import org.attack_type.config.ModConfig;
import org.attack_type.enchantment.ModEnchantments;
import org.attack_type.fragment.SinFragmentAcquisition;
import org.attack_type.fragment.SinFragmentManager;
import org.attack_type.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attack Type 模组主入口。
 * <p>
 * 注册内容：
 * <ul>
 *   <li>10 种附魔（7 罪孽 + 3 物理抗性）</li>
 *   <li>网络包处理器（服务端）</li>
 *   <li>{@code /attacktype} 调试指令</li>
 *   <li>玩家加入/断线事件（同步抗性 + 碎片数据，清理离线数据）</li>
 *   <li>每 tick 实体抗性衰减（非玩家实体每 48000 tick = 2 游戏天衰减一次）</li>
 * </ul>
 */
public class Attack_type implements ModInitializer {
    public static final String MOD_ID = "attack_type";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Fabric 模组初始化入口。
     */
    @Override
    public void onInitialize() {
        ModEnchantments.initialize();
        NetworkHandler.registerServer();
        ModConfig.load();
        SinFragmentAcquisition.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ResistanceCommand.register(dispatcher);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.sendResistanceSync(handler.player);
            NetworkHandler.sendFragmentSync(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ResistanceManager.removeProfile(handler.player.getUuid());
            SinFragmentManager.removeData(handler.player.getUuid());
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
                SinFragmentManager.flushDirtyPlayers(world);
            });
        });

        LOGGER.info("Attack Type mod initialized!");
    }
}