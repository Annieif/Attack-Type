package org.attack_type.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.attack_type.api.SinType;
import org.attack_type.fragment.ClientFragmentCache;

public class SinFragmentHUD {

    private static final int[] COLORS = {
            0xFF4444, // WRATH
            0xFF88FF, // LUST
            0x8888FF, // SLOTH
            0xFFAA44, // GLUTTONY
            0x6666AA, // GLOOM
            0xFFDD44, // PRIDE
            0x44DD44, // ENVY
    };

    private static final String[] SHORT_NAMES = {
            "WRA", "LUS", "SLO", "GLU", "GLO", "PRI", "ENV"
    };

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.options.hudHidden) return;

        int x = 4;
        int y = 4;
        int barWidth = 60;
        int barHeight = 10;
        int spacing = 14;

        SinType[] types = SinType.values();
        for (int i = 0; i < types.length; i++) {
            SinType type = types[i];
            int count = ClientFragmentCache.getFragments(type);
            int color = COLORS[i];
            int rowY = y + i * spacing;

            context.drawTextWithShadow(client.textRenderer, SHORT_NAMES[i], x, rowY, color);

            int barX = x + 30;
            context.fill(barX, rowY + 1, barX + barWidth, rowY + 1 + barHeight, 0x44000000);

            int fillWidth = Math.min(barWidth, (int) ((count / 1000.0f) * barWidth));
            if (fillWidth > 0) {
                context.fill(barX, rowY + 1, barX + fillWidth, rowY + 1 + barHeight, color | 0xAA000000);
            }

            String countText = String.valueOf(count);
            int textColor = count >= 500 ? 0xFF4444 : 0xFFFFFF;
            context.drawTextWithShadow(client.textRenderer, countText, barX + barWidth + 4, rowY, textColor);

            boolean isActive = ClientFragmentCache.getActiveSinType() == type && ClientFragmentCache.getActiveSinLevel() > 0;
            if (isActive) {
                context.drawTextWithShadow(client.textRenderer,
                        "L" + ClientFragmentCache.getActiveSinLevel(),
                        barX + barWidth + 28, rowY, 0xFFFF55);
            }
        }
    }
}