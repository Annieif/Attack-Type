package org.attack_type.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.attack_type.Attack_type;
import org.attack_type.api.SinType;
import org.attack_type.fragment.ClientFragmentCache;

public class SinFragmentHUD {

    public static final Identifier WRATH = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/wrath.png");
    public static final Identifier LUST = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/lust.png");
    public static final Identifier SLOTH = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/sloth.png");
    public static final Identifier GLUTTONY = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/gluttony.png");
    public static final Identifier GLOOM = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/gloom.png");
    public static final Identifier PRIDE = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/pride.png");
    public static final Identifier ENVY = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/envy.png");

    private static final Identifier[] TEXTURES = {WRATH, LUST, SLOTH, GLUTTONY, GLOOM, PRIDE, ENVY};

    private static final int[] COLORS = {
            0xFF4444, 0xFF88FF, 0x8888FF,
            0xFFAA44, 0x6666AA, 0xFFDD44, 0x44DD44
    };

    public static final int ICON_SIZE = 32;
    private static final int PADDING = 2;
    private static final int ROWS = 1;

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        SinType[] types = SinType.values();
        SinType selected = ClientFragmentCache.getActiveSinType();
        int selectedLevel = ClientFragmentCache.getActiveSinLevel();

        int cols = types.length;
        int totalWidth = cols * ICON_SIZE + (cols + 1) * PADDING;
        int totalHeight = ROWS * ICON_SIZE + (ROWS + 1) * PADDING + 14;

        int x = 4;
        int y = 4;

        for (int i = 0; i < types.length; i++) {
            SinType type = types[i];
            int count = ClientFragmentCache.getFragments(type);
            Identifier tex = TEXTURES[i];
            int color = COLORS[i];
            boolean isSelected = selected == type;

            int cellX = x + PADDING + i * (ICON_SIZE + PADDING);
            int cellY = y + PADDING;

            if (isSelected) {
                context.fill(cellX - 1, cellY - 1, cellX + ICON_SIZE + 1, cellY + ICON_SIZE + 1, 0xFFFFAA00);
            }
            if (count >= 1000) {
                context.fill(cellX - 2, cellY - 2, cellX + ICON_SIZE + 2, cellY + ICON_SIZE + 2, 0xFFFF0000);
            } else if (count >= 500) {
                context.fill(cellX - 1, cellY - 1, cellX + ICON_SIZE + 1, cellY + ICON_SIZE + 1, 0x99FF5555);
            }

            drawIcon(context, tex, cellX, cellY, color, count);

            String countText;
            int textColor;
            if (count >= 1000) {
                countText = "死";
                textColor = 0xFFFF4444;
            } else if (count >= 500) {
                countText = "溢";
                textColor = 0xFFFFAA00;
            } else {
                countText = String.valueOf(count);
                textColor = 0xFFFFFF;
            }
            int tw = client.textRenderer.getWidth(countText);
            int tx = cellX + (ICON_SIZE - tw) / 2;
            int ty = cellY + (ICON_SIZE - 9) / 2;
            context.fill(tx - 2, ty - 1, tx + tw + 2, ty + 9, 0x88000000);
            context.drawTextWithShadow(client.textRenderer, countText, tx, ty, textColor);

            if (isSelected && selectedLevel > 0) {
                String lv = "L" + selectedLevel;
                int tw2 = client.textRenderer.getWidth(lv);
                int tx2 = cellX + ICON_SIZE - tw2 - 2;
                int ty2 = cellY + 2;
                context.drawTextWithShadow(client.textRenderer, lv, tx2, ty2, 0xFFFFEE55);
            }
        }
    }

    private static void drawIcon(DrawContext context, Identifier tex, int x, int y, int color, int count) {
        float alpha = 0.4f + Math.min(0.6f, count / 500.0f);
        int a = (int) (alpha * 255);
        int rgba = (a << 24) | (color & 0x00FFFFFF);
        context.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        context.drawTexture(tex, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}