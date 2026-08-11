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
    private static final int FIELD_H = 18;
    private static final int LABEL_W = 64;
    private static final int ROW_H = 18;
    private static final int SECTION_GAP = 8;
    private static final float CLAMP_MAX = 5.0f;

    public ResistanceScreen() {
        super(Text.translatable("screen.attack_type.resistance"));
        this.profile = new ResistanceProfile();
        ResistanceProfile cached = ClientResistanceCache.getProfile();
        if (cached != null) copyProfile(cached, this.profile);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int y = 24;

        int totalHalf = (LABEL_W + 8 + FIELD_W + 8 + 48) / 2;
        int labelX = centerX - totalHalf;
        int fieldX = labelX + LABEL_W + 8;

        AttackType[] atypes = getAttackTypes();
        SinType[] stypes = SinType.values();
        int rows1 = atypes.length;
        int rows2 = stypes.length;

        for (int i = 0; i < atypes.length; i++) {
            int fy = y + i * ROW_H;
            TextFieldWidget tf = new TextFieldWidget(textRenderer, fieldX, fy, FIELD_W, FIELD_H,
                    Text.literal(atypes[i].name()));
            tf.setDrawsBackground(false);
            tf.setText(String.format("%.2f", profile.getPhysicalResistance(atypes[i])));
            addDrawableChild(tf);
            physicalFields.add(tf);
        }

        y += rows1 * ROW_H + SECTION_GAP;
        int sinStartY = y;

        for (int i = 0; i < stypes.length; i++) {
            int fy = y + i * ROW_H;
            TextFieldWidget tf = new TextFieldWidget(textRenderer, fieldX, fy, FIELD_W, FIELD_H,
                    Text.literal(stypes[i].name()));
            tf.setDrawsBackground(false);
            tf.setText(String.format("%.2f", profile.getSinResistance(stypes[i])));
            addDrawableChild(tf);
            sinFields.add(tf);
        }

        y = sinStartY + rows2 * ROW_H + SECTION_GAP;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.attack_type.apply"), b -> applyChanges())
                .dimensions(centerX - 50, y, 100, 20).build());
    }

    private AttackType[] getAttackTypes() {
        return java.util.Arrays.stream(AttackType.values())
                .filter(t -> t != AttackType.NONE).toArray(AttackType[]::new);
    }

    private float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > CLAMP_MAX) return CLAMP_MAX;
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

    private static float tryParse(String s) {
        try { return Float.parseFloat(s); }
        catch (NumberFormatException e) { return Float.NaN; }
    }

    private void drawFieldBorder(DrawContext ctx, TextFieldWidget tf, boolean overflow) {
        int x = tf.getX();
        int y = tf.getY();
        int w = tf.getWidth();
        int h = tf.getHeight();
        int border = overflow ? 0xFFFF5555 : 0xFF888888;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        ctx.fill(x, y, x + w, y + h, 0xFF111111);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        collectInputs();
        renderBackground(context);

        int centerX = width / 2;
        int y = 24;

        int totalHalf = (LABEL_W + 8 + FIELD_W + 8 + 48) / 2;
        int labelX = centerX - totalHalf;
        int fieldX = labelX + LABEL_W + 8;
        int statusX = fieldX + FIELD_W + 8;

        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 8, 0xFFFFFF);

        double product = 1.0;
        AttackType[] atypes = getAttackTypes();
        SinType[] stypes = SinType.values();

        int totalRows = atypes.length + stypes.length;
        boolean[] overflows = new boolean[totalRows];
        float[] rawValues = new float[totalRows];
        int[] lyTexts = new int[totalRows];

        for (int i = 0; i < atypes.length; i++) {
            int ly = y + i * ROW_H;
            TextFieldWidget tf = physicalFields.get(i);
            float raw = tryParse(tf.getText());
            float v = profile.getPhysicalResistance(atypes[i]);
            product *= v;
            boolean overflow = !Float.isNaN(raw) && (raw < 0f || raw > CLAMP_MAX);
            overflows[i] = overflow;
            rawValues[i] = raw;

            drawFieldBorder(context, tf, overflow);

            Text label = Text.translatable("attack_type.attack_type." + atypes[i].name().toLowerCase());
            int lyText = ly + (FIELD_H - 8) / 2;
            lyTexts[i] = lyText;
            context.drawTextWithShadow(textRenderer, label, labelX, lyText, 0xCCCCCC);

            Text status = Text.translatable(v >= 1.0f ? "screen.attack_type.vulnerable" : "screen.attack_type.resist");
            context.drawTextWithShadow(textRenderer, status, statusX, lyText,
                    v >= 1.0f ? 0xFF8888 : 0x88FF88);
        }

        y += atypes.length * ROW_H + SECTION_GAP;
        int sinStartY = y;

        for (int i = 0; i < stypes.length; i++) {
            int idx = atypes.length + i;
            int ly = y + i * ROW_H;
            TextFieldWidget tf = sinFields.get(i);
            float raw = tryParse(tf.getText());
            float v = profile.getSinResistance(stypes[i]);
            product *= v;
            boolean overflow = !Float.isNaN(raw) && (raw < 0f || raw > CLAMP_MAX);
            overflows[idx] = overflow;
            rawValues[idx] = raw;

            drawFieldBorder(context, tf, overflow);

            Text label = Text.translatable("sin.attack_type." + stypes[i].name().toLowerCase());
            int lyText = ly + (FIELD_H - 8) / 2;
            lyTexts[idx] = lyText;
            context.drawTextWithShadow(textRenderer, label, labelX, lyText, 0xCCCCCC);

            Text status = Text.translatable(v >= 1.0f ? "screen.attack_type.vulnerable" : "screen.attack_type.resist");
            context.drawTextWithShadow(textRenderer, status, statusX, lyText,
                    v >= 1.0f ? 0xFF8888 : 0x88FF88);
        }

        y = sinStartY + stypes.length * ROW_H + SECTION_GAP - 4;

        boolean ok = product >= 1.0;
        Text status = Text.translatable(ok ? "screen.attack_type.ok" : "screen.attack_type.lt1");
        Text prodText = Text.translatable("screen.attack_type.total_product_line", product, status);
        int tw = textRenderer.getWidth(prodText);
        context.drawTextWithShadow(textRenderer, prodText,
                centerX - tw / 2, y, ok ? 0x66FF66 : 0xFF6666);

        super.render(context, mouseX, mouseY, delta);

        for (int i = 0; i < totalRows; i++) {
            if (overflows[i]) {
                float raw = rawValues[i];
                Text ovText = Text.literal(raw > CLAMP_MAX ? ">" : "<");
                Text stText = Text.translatable("screen.attack_type.vulnerable");
                int stW = textRenderer.getWidth(stText);
                context.drawTextWithShadow(textRenderer, ovText, statusX + stW + 2, lyTexts[i], 0xFFFF4444);
            }
        }
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