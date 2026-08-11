package org.attack_type.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import org.attack_type.api.AttackType;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.network.ClientResistanceCache;
import org.attack_type.network.ModPackets;

public class ResistanceScreen extends Screen {
    private ResistanceProfile profile;
    private static final int ROW_HEIGHT = 22;
    private static final int START_Y = 30;
    private static final int COL_DEC = 260;
    private static final int COL_INC = 300;

    public ResistanceScreen() {
        super(Text.translatable("screen.attack_type.resistance"));
        this.profile = new ResistanceProfile();
        ResistanceProfile cached = ClientResistanceCache.getProfile();
        if (cached != null) {
            copyProfile(cached, this.profile);
        }
    }

    @Override
    protected void init() {
        super.init();
        int y = START_Y;

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.attack_type.physical"), b -> {}).dimensions(10, y - 16, 80, 16).build());

        for (AttackType type : AttackType.values()) {
            if (type == AttackType.NONE) continue;
            final AttackType at = type;
            final int rowY = y;
            addDrawableChild(ButtonWidget.builder(Text.literal("-0.1"), b -> adjustPhysical(at, -0.1f))
                    .dimensions(COL_DEC, rowY, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+0.1"), b -> adjustPhysical(at, 0.1f))
                    .dimensions(COL_INC, rowY, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("-0.01"), b -> adjustPhysical(at, -0.01f))
                    .dimensions(COL_DEC + 50, rowY, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+0.01"), b -> adjustPhysical(at, 0.01f))
                    .dimensions(COL_INC + 50, rowY, 40, 20).build());
            y += ROW_HEIGHT;
        }

        y += 8;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.attack_type.sin"), b -> {}).dimensions(10, y - 16, 80, 16).build());

        for (SinType type : SinType.values()) {
            final SinType st = type;
            final int rowY = y;
            addDrawableChild(ButtonWidget.builder(Text.literal("-0.1"), b -> adjustSin(st, -0.1f))
                    .dimensions(COL_DEC, rowY, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+0.1"), b -> adjustSin(st, 0.1f))
                    .dimensions(COL_INC, rowY, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("-0.01"), b -> adjustSin(st, -0.01f))
                    .dimensions(COL_DEC + 50, rowY, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("+0.01"), b -> adjustSin(st, 0.01f))
                    .dimensions(COL_INC + 50, rowY, 40, 20).build());
            y += ROW_HEIGHT;
        }

        y += 8;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.attack_type.apply"), b -> applyChanges())
                .dimensions(width / 2 - 50, y, 100, 20).build());
    }

    private void adjustPhysical(AttackType type, float delta) {
        float current = profile.getPhysicalResistance(type);
        float newValue = current + delta;
        newValue = Math.round(newValue * 100.0f) / 100.0f;
        if (newValue < 0.0f || newValue > 5.0f) return;
        profile.setPhysicalResistance(type, newValue);
    }

    private void adjustSin(SinType type, float delta) {
        float current = profile.getSinResistance(type);
        float newValue = current + delta;
        newValue = Math.round(newValue * 100.0f) / 100.0f;
        if (newValue < 0.0f || newValue > 5.0f) return;
        profile.setSinResistance(type, newValue);
    }

    private void applyChanges() {
        ClientResistanceCache.setProfile(profile);

        PacketByteBuf buf = PacketByteBufs.create();
        for (AttackType type : AttackType.values()) {
            if (type != AttackType.NONE) {
                buf.writeFloat(profile.getPhysicalResistance(type));
            }
        }
        for (SinType type : SinType.values()) {
            buf.writeFloat(profile.getSinResistance(type));
        }
        buf.writeFloat(profile.getTotalProduct());
        ClientPlayNetworking.send(ModPackets.RESISTANCE_UPDATE, buf);

        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int y = START_Y;
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.attack_type.total_product", String.format("%.2f", profile.getTotalProduct())),
                width / 2, 20, 0xAAAAAA);

        for (AttackType type : AttackType.values()) {
            if (type == AttackType.NONE) continue;
            float value = profile.getPhysicalResistance(type);
            String label = Text.translatable("attack_type.attack_type." + type.name().toLowerCase()).getString();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(label + ": " + String.format("%.2f", value) + " (" + profile.getResistanceLabel(type) + ")"),
                    10, y + 6, 0xFFFFFF);
            y += ROW_HEIGHT;
        }

        y += 8;

        for (SinType type : SinType.values()) {
            float value = profile.getSinResistance(type);
            String label = Text.translatable("sin.attack_type." + type.name().toLowerCase()).getString();
            context.drawTextWithShadow(textRenderer,
                    Text.literal(label + ": " + String.format("%.2f", value) + " (" + profile.getResistanceLabel(type) + ")"),
                    10, y + 6, 0xFFFFFF);
            y += ROW_HEIGHT;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static void copyProfile(ResistanceProfile from, ResistanceProfile to) {
        for (AttackType type : AttackType.values()) {
            if (type != AttackType.NONE) {
                to.setPhysicalResistance(type, from.getPhysicalResistance(type));
            }
        }
        for (SinType type : SinType.values()) {
            to.setSinResistance(type, from.getSinResistance(type));
        }
        to.setTotalProduct(from.getTotalProduct());
    }
}