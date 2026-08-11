package org.attack_type.gui;

import net.minecraft.client.MinecraftClient;
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
    private static final int LABEL_W = 54;
    private static final int FIELD_W = 60;
    private static final int ROW_H = 20;
    private static final int GAP = 4;

    public ResistanceScreen() {
        super(Text.literal("Resistance Allocation"));
        this.profile = new ResistanceProfile();
        ResistanceProfile cached = ClientResistanceCache.getProfile();
        if (cached != null) {
            copyProfile(cached, this.profile);
        }
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int y = 32;

        int col1X = centerX - FIELD_W - GAP / 2;
        int col2X = centerX + GAP / 2;
        int label1X = col1X - LABEL_W - GAP;
        int label2X = col2X - LABEL_W - GAP;

        AttackType[] atypes = getAttackTypes();
        SinType[] stypes = SinType.values();

        int rows1 = (int) Math.ceil(atypes.length / 2.0);
        int rows2 = (int) Math.ceil(stypes.length / 2.0);

        for (int i = 0; i < atypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int fx = col == 0 ? col1X : col2X;
            int lx = col == 0 ? label1X : label2X;
            int fy = y + row * ROW_H;
            AttackType at = atypes[i];
            TextFieldWidget tf = new TextFieldWidget(textRenderer, fx, fy, FIELD_W, 16,
                    Text.literal(at.name()));
            tf.setText(String.format("%.2f", profile.getPhysicalResistance(at)));
            addDrawableChild(tf);
            physicalFields.add(tf);
        }

        y += Math.max(rows1, 1) * ROW_H + 16;

        for (int i = 0; i < stypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int fx = col == 0 ? col1X : col2X;
            int lx = col == 0 ? label1X : label2X;
            int fy = y + row * ROW_H;
            SinType st = stypes[i];
            TextFieldWidget tf = new TextFieldWidget(textRenderer, fx, fy, FIELD_W, 16,
                    Text.literal(st.name()));
            tf.setText(String.format("%.2f", profile.getSinResistance(st)));
            addDrawableChild(tf);
            sinFields.add(tf);
        }

        y += Math.max(rows2, 1) * ROW_H + 14;

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> applyChanges())
                .dimensions(centerX - 44, y, 88, 20).build());
    }

    private AttackType[] getAttackTypes() {
        return java.util.Arrays.stream(AttackType.values())
                .filter(t -> t != AttackType.NONE).toArray(AttackType[]::new);
    }

    private float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 5f) return 5f;
        return v;
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
        for (AttackType type : AttackType.values()) {
            if (type != AttackType.NONE) buf.writeFloat(profile.getPhysicalResistance(type));
        }
        for (SinType type : SinType.values()) buf.writeFloat(profile.getSinResistance(type));
        buf.writeFloat(profile.getTotalProduct());
        ClientPlayNetworking.send(ModPackets.RESISTANCE_UPDATE, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        collectInputs();
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int y = 32;
        int col1X = centerX - FIELD_W - GAP / 2;
        int col2X = centerX + GAP / 2;
        int label1X = col1X - LABEL_W - GAP;
        int label2X = col2X - LABEL_W - GAP;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 6, 0xFFFFFF);

        double product = 1.0;
        AttackType[] atypes = getAttackTypes();
        for (int i = 0; i < atypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int lx = col == 0 ? label1X : label2X;
            int ly = y + row * ROW_H + 3;
            AttackType at = atypes[i];
            float v = profile.getPhysicalResistance(at);
            product *= v;
            String label = Text.translatable("attack_type.attack_type." + at.name().toLowerCase()).getString();
            context.drawTextWithShadow(textRenderer, Text.literal(label), lx, ly, 0xCCCCCC);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(v >= 1.0f ? "脆弱" : "耐性"),
                    col == 0 ? col1X + FIELD_W + 4 : col2X + FIELD_W + 4, ly,
                    v >= 1.0f ? 0xFF8888 : 0x88FF88);
        }

        int rows1 = (int) Math.ceil(atypes.length / 2.0);
        y += Math.max(rows1, 1) * ROW_H + 2;

        SinType[] stypes = SinType.values();
        for (int i = 0; i < stypes.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int lx = col == 0 ? label1X : label2X;
            int ly = y + row * ROW_H + 3;
            SinType st = stypes[i];
            float v = profile.getSinResistance(st);
            product *= v;
            String label = Text.translatable("sin.attack_type." + st.name().toLowerCase()).getString();
            context.drawTextWithShadow(textRenderer, Text.literal(label), lx, ly, 0xCCCCCC);
            context.drawTextWithShadow(textRenderer,
                    Text.literal(v >= 1.0f ? "脆弱" : "耐性"),
                    col == 0 ? col1X + FIELD_W + 4 : col2X + FIELD_W + 4, ly,
                    v >= 1.0f ? 0xFF8888 : 0x88FF88);
        }

        int rows2 = (int) Math.ceil(stypes.length / 2.0);
        y += Math.max(rows2, 1) * ROW_H + 8;

        int prodColor;
        String prodLabel;
        if (product < 0.95) { prodColor = 0x88FF88; prodLabel = "耐性"; }
        else if (product > 1.05) { prodColor = 0xFF8888; prodLabel = "脆弱"; }
        else { prodColor = 0xFFFFAA; prodLabel = "平衡"; }

        String prodStr = String.format("Total Product: %.4f  (%s)", product, prodLabel);
        int tw = textRenderer.getWidth(prodStr);
        context.drawTextWithShadow(textRenderer, Text.literal(prodStr), centerX - tw / 2, y, prodColor);
    }

    @Override
    public boolean shouldPause() { return false; }

    private static void copyProfile(ResistanceProfile from, ResistanceProfile to) {
        for (AttackType type : AttackType.values()) {
            if (type != AttackType.NONE) to.setPhysicalResistance(type, from.getPhysicalResistance(type));
        }
        for (SinType type : SinType.values()) to.setSinResistance(type, from.getSinResistance(type));
        to.setTotalProduct(from.getTotalProduct());
    }
}