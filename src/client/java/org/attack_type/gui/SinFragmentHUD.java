package org.attack_type.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.attack_type.Attack_type;
import org.attack_type.api.SinType;
import org.attack_type.fragment.ClientFragmentCache;
import org.joml.Matrix4f;

public class SinFragmentHUD {

    public static final Identifier WRATH = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/wrath.png");
    public static final Identifier LUST = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/lust.png");
    public static final Identifier SLOTH = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/sloth.png");
    public static final Identifier GLUTTONY = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/gluttony.png");
    public static final Identifier GLOOM = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/gloom.png");
    public static final Identifier PRIDE = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/pride.png");
    public static final Identifier ENVY = new Identifier(Attack_type.MOD_ID, "textures/gui/sin_fragment/envy.png");

    private static final Identifier[] TEXTURES = {WRATH, LUST, SLOTH, GLUTTONY, GLOOM, PRIDE, ENVY};

    public static final int ICON_SIZE = 32;
    private static final int PADDING = 3;
    private static final int CORNER = 2;

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        SinType[] types = SinType.values();
        SinType selected = ClientFragmentCache.getActiveSinType();
        int selectedLevel = ClientFragmentCache.getActiveSinLevel();

        int x = 4;
        int y = 4;

        for (int i = 0; i < types.length; i++) {
            SinType type = types[i];
            int count = ClientFragmentCache.getFragments(type);
            Identifier tex = TEXTURES[i];
            boolean isSelected = selected == type;

            int cellX = x + PADDING + i * (ICON_SIZE + PADDING);
            int cellY = y + PADDING;

            if (count >= 1000) {
                context.fill(cellX - 1, cellY - 1, cellX + ICON_SIZE + 1, cellY + ICON_SIZE + 1, 0xBBDD0000);
            } else if (count >= 500) {
                context.fill(cellX - 1, cellY - 1, cellX + ICON_SIZE + 1, cellY + ICON_SIZE + 1, 0x99DD5500);
            }

            drawIcon(context, tex, cellX, cellY, count);

            if (isSelected) {
                drawGoldCorners(context, cellX, cellY);
            }

            String countText;
            int textColor;
            if (count >= 1000) {
                countText = Text.translatable("hud.attack_type.death_char").getString();
                textColor = 0xFF6666;
            } else if (count >= 500) {
                countText = Text.translatable("hud.attack_type.overflow_char").getString();
                textColor = 0xFFBB44;
            } else if (count == 0) {
                countText = "0";
                textColor = 0x999999;
            } else {
                countText = String.valueOf(count);
                textColor = 0xFFFFFF;
            }
            int tw = client.textRenderer.getWidth(countText);
            int tx = cellX + (ICON_SIZE - tw) / 2;
            int ty = cellY + (ICON_SIZE - 9) / 2;
            context.fill(tx - 1, ty - 1, tx + tw + 1, ty + 9, 0x55000000);
            context.drawTextWithShadow(client.textRenderer, countText, tx, ty, textColor);

            if (isSelected && selectedLevel > 0) {
                String lv = "L" + selectedLevel;
                int tw2 = client.textRenderer.getWidth(lv);
                int tx2 = cellX + ICON_SIZE - tw2 - 1;
                int ty2 = cellY + 1;
                context.drawTextWithShadow(client.textRenderer, lv, tx2, ty2, 0xFFEE77);
            }
        }
    }

    private static void drawGoldCorners(DrawContext ctx, int x, int y) {
        int color = 0xFFFFD866;
        int s = ICON_SIZE;
        ctx.fill(x - 1, y - 1, x + CORNER + 1, y, color);
        ctx.fill(x - 1, y - 1, x, y + CORNER, color);
        ctx.fill(x + s - CORNER, y - 1, x + s + 1, y, color);
        ctx.fill(x + s, y - 1, x + s + 1, y + CORNER, color);
        ctx.fill(x - 1, y + s, x + CORNER + 1, y + s + 1, color);
        ctx.fill(x - 1, y + s - CORNER, x, y + s + 1, color);
        ctx.fill(x + s - CORNER, y + s, x + s + 1, y + s + 1, color);
        ctx.fill(x + s, y + s - CORNER, x + s + 1, y + s + 1, color);
    }

    private static void drawIcon(DrawContext context, Identifier tex, int x, int y, int count) {
        float alpha = 0.5f + Math.min(0.5f, count / 1000.0f);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
        RenderSystem.enableBlend();
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
        bufferBuilder.vertex(matrix, x, y + ICON_SIZE, 0).color(1f, 1f, 1f, alpha).texture(0, 1).next();
        bufferBuilder.vertex(matrix, x + ICON_SIZE, y + ICON_SIZE, 0).color(1f, 1f, 1f, alpha).texture(1, 1).next();
        bufferBuilder.vertex(matrix, x + ICON_SIZE, y, 0).color(1f, 1f, 1f, alpha).texture(1, 0).next();
        bufferBuilder.vertex(matrix, x, y, 0).color(1f, 1f, 1f, alpha).texture(0, 0).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}