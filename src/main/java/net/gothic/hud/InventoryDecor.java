package net.gothic.hud;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Ornate frame that extends beyond the vanilla inventory background, with candle flames and dripping blood. */
final class InventoryDecor {
    private static final Identifier FRAME = GothicHudClient.id("textures/gui/inventory_frame.png");
    private static final int[] DRIP_X = { 34, 58, 71, 96, 120, 146, 170, 190, 213 };

    private InventoryDecor() {}

    static void draw(GuiGraphicsExtractor g, int left, int top) {
        long now = Util.getMillis();
        int fx = left - 40, fy = top - 45;                // the 256x256 frame has a 176x166 transparent window
        g.blit(RenderPipelines.GUI_TEXTURED, FRAME, fx, fy, 0f, 0f, 256, 256, 256, 256);
        flame(g, fx + 23, fy + 176, now, 0.0);
        flame(g, fx + 231, fy + 176, now, 1.7);
        for (int i = 0; i < DRIP_X.length; i++) drip(g, fx + DRIP_X[i], fy + 5, fy + 40, now, i);
        streak(g, fx + 9, fy + 50, now, 0);
        streak(g, fx + 246, fy + 70, now, 1);
    }

    private static float smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return (float) (t * t * (3 - 2 * t));
    }

    private static void flame(GuiGraphicsExtractor g, int x, int y, long now, double phase) {
        double t = now / 1000.0 * 7.0 + phase;
        int h = 4 + (int) Math.round(1.2 * Math.sin(t * 1.7) + 0.8 * Math.sin(t * 3.1 + 1.3));
        int sway = (int) Math.round(Math.sin(t * 0.9 + phase) * 0.9);
        g.fill(x - 4, y - h - 2, x + 4, y + 1, 0x18FFB050);              // soft glow
        g.fill(x - 1 + sway, y - h, x + 1 + sway, y, 0xCCFF8A28);         // orange body
        g.fill(x + sway, y - h + 1, x + 1 + sway, y, 0xFFFFE890);         // bright core
    }

    private static void drip(GuiGraphicsExtractor g, int x, int y0, int limitY, long now, int idx) {
        long cycle = 8000L + idx * 370L;
        double ph = ((now + idx * 1913L) % cycle) / (double) cycle;
        int maxLen = 14 + (idx * 7) % 17;
        int len = ph < 0.72 ? 3 + (int) ((maxLen - 3) * smooth(ph / 0.72)) : maxLen;
        if (ph < 0.84) {
            g.fill(x, y0, x + 2, y0 + len, 0xCCB0161E);
            g.fill(x, y0 + 2, x + 1, y0 + len, 0x99E04048);
            g.fill(x - 1, y0 + len, x + 3, y0 + len + 2, 0xE0C01822);
            g.fill(x, y0 + len + 2, x + 2, y0 + len + 3, 0xE0A01018);
        } else if (ph < 0.97) {
            double f = (ph - 0.84) / 0.13;
            int dy = y0 + maxLen + (int) (f * f * 34);
            if (dy + 3 > limitY) return;
            int a = (int) (220 * (1 - f));
            g.fill(x, dy, x + 2, dy + 3, (a << 24) | 0xB0161E);
        }
    }

    private static void streak(GuiGraphicsExtractor g, int x, int y0, long now, int idx) {
        double ph = ((now + idx * 2500L) % 14000L) / 14000.0;
        int len = 10 + (int) (70 * smooth(Math.min(1.0, ph / 0.8)));
        int a = ph > 0.9 ? (int) (204 * (1 - (ph - 0.9) / 0.1)) : 204;
        g.fill(x, y0, x + 2, y0 + len, (a << 24) | 0xA8141C);
        g.fill(x - 1, y0 + len, x + 3, y0 + len + 2, (a << 24) | 0xC01822);
    }
}
