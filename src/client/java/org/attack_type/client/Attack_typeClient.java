package org.attack_type.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.attack_type.api.SinType;
import org.attack_type.fragment.ClientFragmentCache;
import org.attack_type.gui.ResistanceScreen;
import org.attack_type.gui.SinFragmentHUD;
import org.attack_type.network.NetworkHandlerClient;
import org.attack_type.network.ModPackets;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端模组入口。
 * <p>
 * 注册按键绑定、HUD 渲染、网络包接收器和连接事件：
 * <ul>
 *   <li><b>U 键</b> — 打开抗性配置 GUI</li>
 *   <li><b>[ 键</b> — 向左切换激活罪孽</li>
 *   <li><b>] 键</b> — 向右切换激活罪孽</li>
 *   <li><b>\ 键</b> — 触发罪孽（连按 1~3 次选择 L1~L3）</li>
 * </ul>
 * <p>
 * 罪孽触发采用 20 tick 窗口缓冲：连按 N 次触发 Lv.N，超时后发送网络包。
 */
public class Attack_typeClient implements ClientModInitializer {
    private static KeyBinding resistanceKey;
    private static KeyBinding sinLeftKey;
    private static KeyBinding sinRightKey;
    private static KeyBinding sinTriggerKey;

    /** 触发键连按计数（1~3） */
    private static int triggerPressCount = 0;
    /** 触发窗口倒计时 */
    private static int triggerTimer = 0;
    /** 触发窗口长度（tick），连按窗口为 20 tick = 1 秒 */
    private static final int TRIGGER_WINDOW = 20;

    @Override
    public void onInitializeClient() {
        resistanceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.attack_type.resistance",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.attack_type"
        ));

        sinLeftKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.attack_type.sin_left",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                "category.attack_type"
        ));

        sinRightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.attack_type.sin_right",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                "category.attack_type"
        ));

        sinTriggerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.attack_type.sin_trigger",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                "category.attack_type"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (resistanceKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new ResistanceScreen());
            }

            if (sinLeftKey.wasPressed()) {
                cycleActiveSin(-1);
            }
            if (sinRightKey.wasPressed()) {
                cycleActiveSin(1);
            }

            if (sinTriggerKey.wasPressed()) {
                triggerPressCount = Math.min(triggerPressCount + 1, 3);
                triggerTimer = TRIGGER_WINDOW;
            }

            if (triggerTimer > 0) {
                triggerTimer--;
                if (triggerTimer == 0 && triggerPressCount > 0) {
                    sendTriggerPacket(triggerPressCount);
                    triggerPressCount = 0;
                }
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            SinFragmentHUD.render(context);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            triggerPressCount = 0;
            triggerTimer = 0;
        });

        NetworkHandlerClient.registerClient();
    }

    /**
     * 循环切换激活罪孽类型。
     *
     * @param direction -1 向左，+1 向右
     */
    private static void cycleActiveSin(int direction) {
        SinType[] types = SinType.values();
        SinType current = ClientFragmentCache.getActiveSinType();
        int idx = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == current) {
                idx = i;
                break;
            }
        }
        idx = (idx + direction + types.length) % types.length;
        ClientFragmentCache.setActiveSinType(types[idx]);
        showCycleMessage();
    }

    /**
     * 在 action bar 显示当前激活的罪孽名称。
     */
    private static void showCycleMessage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text sinName = Text.translatable("sin.attack_type." + ClientFragmentCache.getActiveSinType().name().toLowerCase());
            client.player.sendMessage(
                    Text.translatable("hud.attack_type.active_sin", sinName),
                    true
            );
        }
    }

    /**
     * 发送罪孽触发网络包到服务端。
     *
     * @param level 触发等级（1~3）
     */
    private static void sendTriggerPacket(int level) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(ClientFragmentCache.getActiveSinType().ordinal());
        buf.writeInt(level);
        ClientPlayNetworking.send(ModPackets.FRAGMENT_TRIGGER, buf);
    }
}