package org.attack_type.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import org.attack_type.api.AttackType;
import org.attack_type.api.ResistanceProfile;
import org.attack_type.api.SinType;
import org.attack_type.network.ClientResistanceCache;
import org.attack_type.network.ModPackets;

import java.util.ArrayList;
import java.util.List;

public class ResistanceScreen extends Screen {
    private ResistanceProfile profile;
    private final List<TextFieldWidget> physicalFields = new ArrayList<>();
    private final List<TextFieldWidget> sinFields = new ArrayList<>();

    private static final int FIELD_W = 72;
    private static final int ROW_H = 22;
    private static final int COL_GAP = 16;
    private static final int SECTION_GAP = 14;

    public ResistanceScreen() {
        super(Text.literal("Resistance Allocation"));
        this.profile = new ResistanceProfile();
        ResistanceProfile cached = ClientResistanceCache.getProfile();
        if (cached != null) copyProfile(cached, this.profile);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int y = 28;

        int col2FieldX = centerX + COL_GAP / 2;
        int col1FieldX = centerX - COL_GAP / 2 - FIELD_W;

        AttackType[] atypes = getAttackTypes();
        SinType[] stypes = SinType.values();
        int rows1 = (atypes.length + 1) / 2;
        int rows2 = (stypes.length + 1) / 2;

        for (int i = 0; i < atypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int fx = col == 0 ? col1FieldX : col2FieldX;
            int fy = y + row * ROW_H;
            TextFieldWidget tf = new TextFieldWidget(textRenderer, fx, fy, FIELD_W, 18,
                    Text.literal(atypes[i].name()));
            tf.setText(String.format("%.2f", profile.getPhysicalResistance(atypes[i])));
            addDrawableChild(tf);
            physicalFields.add(tf);
        }

        y += rows1 * ROW_H + SECTION_GAP;
        int sinStartY = y;

        for (int i = 0; i < stypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int fx = col == 0 ? col1FieldX : col2FieldX;
            int fy = y + row * ROW_H;
            TextFieldWidget tf = new TextFieldWidget(textRenderer, fx, fy, FIELD_W, 18,
                    Text.literal(stypes[i].name()));
            tf.setText(String.format("%.2f", profile.getSinResistance(stypes[i])));
            addDrawableChild(tf);
            sinFields.add(tf);
        }

        y = sinStartY + rows2 * ROW_H + SECTION_GAP;
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applyChanges())
                .dimensions(centerX - 50, y, 100, 20).build());
    }

    private AttackType[] getAttackTypes() {
        return java.util.Arrays.stream(AttackType.values())
                .filter(t -> t != AttackType.NONE).toArray(AttackType[]::new);
    }

    private float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 5f) return 5f;
        return Math.round(v * 100f) / 100f;
    }

    private void collectInputs() {
        AttackType[] atypes = getAttackTypes();
        for (int i = 0; i < atypes.length && i < physicalFields.size(); i++) {
            try {
                float v = Float.parseFloat(physicalFields.get(i).getText());
                profile.setPhysicalResistance(atypes[i], clamp(v));
            } catch (NumberFormatException ignored) {}
        }
        SinType[] stypes = SinType.values();
        for (int i = 0; i < stypes.length && i < sinFields.size(); i++) {
            try {
                float v = Float.parseFloat(sinFields.get(i).getText());
                profile.setSinResistance(stypes[i], clamp(v));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void applyChanges() {
        collectInputs();
        ClientResistanceCache.setProfile(profile);
        PacketByteBuf buf = PacketByteBufs.create();
        for (AttackType type : AttackType.values())
            if (type != AttackType.NONE) buf.writeFloat(profile.getPhysicalResistance(type));
        for (SinType type : SinType.values()) buf.writeFloat(profile.getSinResistance(type));
        buf.writeFloat(profile.getTotalProduct());
        ClientPlayNetworking.send(ModPackets.RESISTANCE_UPDATE, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        collectInputs();
        renderBackground(context);

        int centerX = width / 2;
        int y = 28;
        int col2FieldX = centerX + COL_GAP / 2;
        int col1FieldX = centerX - COL_GAP / 2 - FIELD_W;
        int col1LabelX = col1FieldX - 42;
        int col2LabelX = col2FieldX - 42;
        int status1X = col1FieldX + FIELD_W + 4;
        int status2X = col2FieldX + FIELD_W + 4;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 8, 0xFFFFFF);

        double product = 1.0;
        AttackType[] atypes = getAttackTypes();
        for (int i = 0; i < atypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int ly = y + row * ROW_H + 5;
            float v = profile.getPhysicalResistance(atypes[i]);
            product *= v;
            String label = Text.translatable("attack_type.attack_type." + atypes[i].name().toLowerCase()).getString();
            if (col == 0) {
                context.drawTextWithShadow(textRenderer, Text.literal(label), col1LabelX, ly, 0xCCCCCC);
                context.drawTextWithShadow(textRenderer,
                        Text.literal(v >= 1.0f ? "脆弱" : "耐性"), status1X, ly,
                        v >= 1.0f ? 0xFF8888 : 0x88FF88);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal(label), col2LabelX, ly, 0xCCCCCC);
                context.drawTextWithShadow(textRenderer,
                        Text.literal(v >= 1.0f ? "脆弱" : "耐性"), status2X, ly,
                        v >= 1.0f ? 0xFF8888 : 0x88FF88);
            }
        }

        int rows1 = (atypes.length + 1) / 2;
        y += rows1 * ROW_H + SECTION_GAP;
        int sinStartY = y;

        SinType[] stypes = SinType.values();
        for (int i = 0; i < stypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int ly = y + row * ROW_H + 5;
            float v = profile.getSinResistance(stypes[i]);
            product *= v;
            String label = Text.translatable("sin.attack_type." + stypes[i].name().toLowerCase()).getString();
            if (col == 0) {
                context.drawTextWithShadow(textRenderer, Text.literal(label), col1LabelX, ly, 0xCCCCCC);
                context.drawTextWithShadow(textRenderer,
                        Text.literal(v >= 1.0f ? "脆弱" : "耐性"), status1X, ly,
                        v >= 1.0f ? 0xFF8888 : 0x88FF88);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal(label), col2LabelX, ly, 0xCCCCCC);
                context.drawTextWithShadow(textRenderer,
                        Text.literal(v >= 1.0f ? "脆弱" : "耐性"), status2X, ly,
                        v >= 1.0f ? 0xFF8888 : 0x88FF88);
            }
        }

        int rows2 = (stypes.length + 1) / 2;
        y = sinStartY + rows2 * ROW_H + SECTION_GAP - 4;

        boolean ok = product >= 1.0;
        String prodStr = String.format("Total Product: %.4f   %s", product, ok ? "✓ OK" : "✗ <1");
        int tw = textRenderer.getWidth(prodStr);
        context.drawTextWithShadow(textRenderer, Text.literal(prodStr),
                centerX - tw / 2, y, ok ? 0x66FF66 : 0xFF6666);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static void copyProfile(ResistanceProfile from, ResistanceProfile to) {
        for (AttackType type : AttackType.values())
            if (type != AttackType.NONE) to.setPhysicalResistance(type, from.getPhysicalResistance(type));
        for (SinType type : SinType.values()) to.setSinResistance(type, from.getSinResistance(type));
        to.setTotalProduct(from.getTotalProduct());
    }
}