package org.attack_type.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.attack_type.gui.ResistanceScreen;
import org.attack_type.network.NetworkHandlerClient;
import org.lwjgl.glfw.GLFW;

public class Attack_typeClient implements ClientModInitializer {
    private static KeyBinding resistanceKey;

    @Override
    public void onInitializeClient() {
        resistanceKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.attack_type.resistance",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.attack_type"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (resistanceKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new ResistanceScreen());
            }
        });

        NetworkHandlerClient.registerClient();
    }
}