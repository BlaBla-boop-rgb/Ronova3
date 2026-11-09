package dev.ronovamod.cosmetics.gui;

import dev.ronovamod.cosmetics.CosmeticsClient;
import dev.ronovamod.cosmetics.CosmeticsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Das /ronona-Menü. Eigene, schlichte Zeichnung (Rechtecke + Text), damit es auf 26.x stabil bleibt.
 */
public class CosmeticsScreen extends Screen {
    // Farben (ARGB)
    private static final int BACKDROP = 0xC0080306;
    private static final int PANEL = 0xF0140A0E;
    private static final int BORDER = 0xFF8B0A1A;
    private static final int ROW = 0xFF1E1015;
    private static final int ROW_HOVER = 0xFF2C141C;
    private static final int TEXT = 0xFFEDE3E6;
    private static final int MUTED = 0xFF9C8A90;
    private static final int ON = 0xFFD0203A;
    private static final int OFF = 0xFF3A2C30;
    private static final int GOLD = 0xFFC8A046;

    private static final int PANEL_W = 300;
    private static final int ROW_H = 22;

    private record Row(String label, Supplier<String> value, BooleanSupplier active, Runnable onClick, boolean section) {}

    private final List<Row> rows = new ArrayList<>();
    private int panelX, panelY, panelH;

    public CosmeticsScreen() {
        super(Component.literal("Ronova"));
    }

    @Override
    protected void init() {
        CosmeticsConfig c = CosmeticsConfig.get();
        rows.clear();

        rows.add(section("Cosmetics"));
        rows.add(toggle("Blutroter Heiligenschein", () -> c.halo, () -> c.halo = !c.halo));
        rows.add(toggle("Seelenaura", () -> c.soulAura, () -> c.soulAura = !c.soulAura));
        rows.add(toggle("Rabenschwarm", () -> c.crowSwarm, () -> c.crowSwarm = !c.crowSwarm));
        rows.add(toggle("Todesspur", () -> c.deathTrail, () -> c.deathTrail = !c.deathTrail));


        panelH = 44 + rows.size() * (ROW_H + 2) + 34;
        panelX = (this.width - PANEL_W) / 2;
        panelY = Math.max(6, (this.height - panelH) / 2);
    }

    private static Row section(String title) {
        return new Row(title, () -> "", () -> false, () -> {}, true);
    }

    private static Row toggle(String label, BooleanSupplier state, Runnable flip) {
        return new Row(label, () -> state.getAsBoolean() ? "AN" : "AUS", state, flip, false);
    }

    /* ------------------------------------------------------------------ zeichnen */

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        Font font = Minecraft.getInstance().font;

        g.fill(0, 0, this.width, this.height, BACKDROP);
        g.fill(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + panelH + 1, BORDER);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, PANEL);

        // Kopfzeile
        g.fill(panelX + 12, panelY + 12, panelX + 15, panelY + 28, BORDER);
        g.text(font, "☠ Ronova – Schatten des Todes", panelX + 22, panelY + 12, TEXT, true);
        g.text(font, "Cosmetics", panelX + 22, panelY + 23, MUTED, false);

        int y = panelY + 44;
        for (Row row : rows) {
            if (row.section()) {
                g.text(font, row.label().toUpperCase(), panelX + 14, y + 8, GOLD, false);
                g.fill(panelX + 14 + font.width(row.label().toUpperCase()) + 6, y + 12,
                        panelX + PANEL_W - 14, y + 13, OFF);
            } else {
                boolean hover = inside(mouseX, mouseY, panelX + 10, y, PANEL_W - 20, ROW_H);
                g.fill(panelX + 10, y, panelX + PANEL_W - 10, y + ROW_H, hover ? ROW_HOVER : ROW);
                g.text(font, row.label(), panelX + 18, y + 7, TEXT, false);

                String value = row.value().get();
                int pillW = Math.max(34, font.width(value) + 14);
                int pillX = panelX + PANEL_W - 16 - pillW;
                boolean isToggle = value.equals("AN") || value.equals("AUS");
                int pillColor = isToggle ? (row.active().getAsBoolean() ? ON : OFF) : OFF;
                g.fill(pillX, y + 4, pillX + pillW, y + ROW_H - 4, pillColor);
                g.text(font, value, pillX + (pillW - font.width(value)) / 2, y + 7, TEXT, false);
            }
            y += ROW_H + 2;
        }

        g.text(font, "Tipp: F5 für die 3rd-Person-Ansicht", panelX + 14, y + 6, MUTED, false);
        // Fertig-Knopf
        int bx = panelX + PANEL_W - 90, by = panelY + panelH - 26;
        boolean hover = inside(mouseX, mouseY, bx, by, 76, 18);
        g.fill(bx, by, bx + 76, by + 18, hover ? ON : BORDER);
        g.text(font, "Fertig", bx + (76 - font.width("Fertig")) / 2, by + 5, TEXT, false);
    }

    /* ------------------------------------------------------------------ klicken */

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        CosmeticsClient.LOGGER.info("[Diagnose] Klick im Menü: x={} y={} Taste={} | Panel x={} y={} b={} h={}",
                mx, my, event.button(), panelX, panelY, PANEL_W, panelH);

        int bx = panelX + PANEL_W - 90, by = panelY + panelH - 26;
        if (inside(mx, my, bx, by, 76, 18)) {
            onClose();
            return true;
        }

        int y = panelY + 44;
        for (Row row : rows) {
            if (!row.section() && inside(mx, my, panelX + 10, y, PANEL_W - 20, ROW_H)) {
                row.onClick().run();
                CosmeticsConfig.save();
                return true;
            }
            y += ROW_H + 2;
        }
        return false;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        CosmeticsConfig.save();
        super.onClose();
    }
}
