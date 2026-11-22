package net.gothic.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Ornate frame that extends beyond the vanilla inventory background, with candle flames and dripping blood. */
public final class InventoryDecor {
    private static final Identifier FRAME = GothicHudClient.id("textures/gui/inventory_frame.png");
    private static final Identifier WEB_TL = GothicHudClient.id("textures/gui/web_tl.png");
    private static final Identifier WEB_TR = GothicHudClient.id("textures/gui/web_tr.png");
    private static final Identifier WEB_BL = GothicHudClient.id("textures/gui/web_bl.png");
    private static final Identifier WEB_BR = GothicHudClient.id("textures/gui/web_br.png");
    private static final int[] DRIP_X = { 34, 58, 71, 96, 120, 146, 170, 190, 213 };

    private InventoryDecor() {}

    /** Cobwebs in the corners of the whole screen, only while the inventory is open. */
    public static void drawWebs(GuiGraphicsExtractor g, int w, int h) {
        final int s = 96;
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_TL, 0, 0, 0f, 0f, s, s, s, s);
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_TR, w - s, 0, 0f, 0f, s, s, s, s);
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_BL, 0, h - s, 0f, 0f, s, s, s, s);
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_BR, w - s, h - s, 0f, 0f, s, s, s, s);
    }

    /** Stone plinth under the player bust: covers the part of the niche below the waist. */
    private static void plinth(GuiGraphicsExtractor g, int left, int top) {
        int x0 = left + 26, x1 = left + 76, y0 = top + 56, y1 = top + 77;
        g.fill(x0, y0, x1, y1, 0xFF1E1E22);
        g.fill(x0, y0, x1, y0 + 2, 0xFF5E5E66);
        g.fill(x0, y0 + 2, x1, y0 + 3, 0xFF121215);
        for (int y = y0 + 7; y < y1; y += 7) g.fill(x0, y, x1, y + 1, 0xFF2A2A2F);
        g.fill(left + 50, top + 61, left + 52, top + 73, 0xFF9E926F);      // small cross
        g.fill(left + 46, top + 64, left + 56, top + 66, 0xFF9E926F);
    }

    public static void draw(GuiGraphicsExtractor g, int left, int top) {
        long now = System.currentTimeMillis();
        int fx = left - 40, fy = top - 45;                // the 256x256 frame has a 176x166 transparent window
        g.blit(RenderPipelines.GUI_TEXTURED, FRAME, fx, fy, 0f, 0f, 256, 256, 256, 256);
        plinth(g, left, top);
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
