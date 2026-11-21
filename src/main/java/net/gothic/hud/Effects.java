package net.gothic.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

/** Screen effects: pixel blood on hit, heartbeat + twitching vignette, cobwebs in the screen corners. */
public final class Effects {
    private static final Random RNG = new Random();
    private static final SoundEvent HEARTBEAT = SoundEvent.createVariableRangeEvent(GothicHudClient.id("heartbeat"));
    private static final Identifier WEB_TL = GothicHudClient.id("textures/gui/web_tl.png");
    private static final Identifier WEB_TR = GothicHudClient.id("textures/gui/web_tr.png");
    private static final Identifier WEB_BL = GothicHudClient.id("textures/gui/web_bl.png");
    private static final Identifier WEB_BR = GothicHudClient.id("textures/gui/web_br.png");
    private static final int[] BLOOD = { 0xB0141C, 0xC81C26, 0x8C0C14, 0xDC2C34 };

    private static final class Splash {
        long start, dur;
        int seed;
        float strength;
        final List<float[]> blots = new ArrayList<>();   // {nx, ny, radius}
    }

    private static final List<Splash> SPLASHES = new ArrayList<>();
    private static float lastHealth = -1f;
    private static int tick = 0;
    private static int nextBeat = 120;
    private static long beatAt = -100000L;
    private static long twitchUntil = 0L;
    private static long nextJitter = 0L;
    private static final float[] SIDE = { 1f, 1f, 1f, 1f };   // top, bottom, left, right thickness multipliers
    private static boolean running = false;
    private static float lowHealth = 0f;

    private Effects() {}

    // ------------------------------------------------------------------ logic (20 times per second)
    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            lastHealth = -1f;
            SPLASHES.clear();
            return;
        }
        if (mc.isPaused()) return;
        tick++;
        float hp = p.getHealth();
        float max = Math.max(1f, p.getMaxHealth());
        if (lastHealth >= 0f && hp < lastHealth - 0.01f && p.isAlive()) onHurt(lastHealth - hp);
        lastHealth = hp;
        running = p.isSprinting();
        lowHealth = Mth.clamp(1f - hp / max, 0f, 1f);

        if (p.isAlive() && tick >= nextBeat) beat(mc);

        double chance = running ? 0.035 : 0.006;
        chance *= 1.0 + lowHealth * 1.5;
        if (RNG.nextDouble() < chance) startTwitch();

        long now = Util.getMillis();
        SPLASHES.removeIf(s -> now - s.start > s.dur);
    }

    private static void beat(Minecraft mc) {
        float volume = running ? 0.9f : 0.6f;
        float pitch = 0.92f + RNG.nextFloat() * 0.16f;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(HEARTBEAT, pitch, volume));
        beatAt = Util.getMillis();
        // random next moment: rare when calm, frequent when sprinting or hurt
        int interval = running ? 60 + RNG.nextInt(100) : 200 + RNG.nextInt(360);
        interval = (int) (interval * (1.0f - 0.5f * lowHealth));
        nextBeat = tick + Math.max(30, interval);
        if (RNG.nextFloat() < 0.35f) startTwitch();
    }

    private static void startTwitch() {
        long now = Util.getMillis();
        twitchUntil = now + 140 + RNG.nextInt(260);
        nextJitter = 0L;
    }

    private static void onHurt(float damage) {
        long now = Util.getMillis();
        Splash s = new Splash();
        s.start = now;
        s.dur = (long) (2200 + Math.min(1800f, damage * 220f));
        s.seed = RNG.nextInt();
        s.strength = Mth.clamp(0.5f + damage / 12f, 0.5f, 1f);
        int n = 6 + RNG.nextInt(5);
        for (int i = 0; i < n; i++) {
            int side = RNG.nextInt(4);
            float t = RNG.nextFloat(), off = RNG.nextFloat() * 0.03f;
            float nx = side == 2 ? off : side == 3 ? 1f - off : t;
            float ny = side == 0 ? off : side == 1 ? 1f - off : t;
            s.blots.add(new float[] { nx, ny, 0.10f + RNG.nextFloat() * 0.16f });
        }
        SPLASHES.add(s);
        if (SPLASHES.size() > 4) SPLASHES.remove(0);
        beatAt = now;                       // strong pulse of the vignette
        startTwitch();
    }

    // ------------------------------------------------------------------ drawing (every frame)
    public static void extract(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int w = g.guiWidth(), h = g.guiHeight();
        long now = Util.getMillis();
        drawVignette(g, w, h, now);
        drawBlood(g, w, h, now);
        drawWebs(g, w, h);
    }

    private static void drawVignette(GuiGraphicsExtractor g, int w, int h, long now) {
        double t = now - beatAt;
        float env = 0f;
        if (t >= 0 && t < 1500) {
            env = (float) Math.exp(-t / 150.0);                                   // "lub"
            if (t > 240) env = Math.max(env, 0.75f * (float) Math.exp(-(t - 240) / 120.0)); // "dub"
        }
        boolean twitch = now < twitchUntil;
        if (twitch && now >= nextJitter) {
            for (int i = 0; i < 4; i++) SIDE[i] = 0.7f + RNG.nextFloat() * 0.9f;
            nextJitter = now + 45;
        } else if (!twitch) {
            for (int i = 0; i < 4; i++) SIDE[i] += (1f - SIDE[i]) * 0.2f;
        }
        float strength = 0.12f + 0.12f * lowHealth + 0.42f * env + (twitch ? 0.12f : 0f);
        final int rings = 12, thick = 6;
        for (int i = 0; i < rings; i++) {
            float a = strength * (float) Math.pow(1f - i / (float) rings, 2.0);
            int alpha = (int) (Math.min(1f, a) * 255f);
            if (alpha <= 2) continue;
            int col = (alpha << 24) | 0x0E0000;
            int t0 = (int) (i * thick * SIDE[0]), t1 = (int) ((i + 1) * thick * SIDE[0]);
            int b0 = (int) (i * thick * SIDE[1]), b1 = (int) ((i + 1) * thick * SIDE[1]);
            int l0 = (int) (i * thick * SIDE[2]), l1 = (int) ((i + 1) * thick * SIDE[2]);
            int r0 = (int) (i * thick * SIDE[3]), r1 = (int) ((i + 1) * thick * SIDE[3]);
            g.fill(0, t0, w, t1, col);
            g.fill(0, h - b1, w, h - b0, col);
            g.fill(l0, 0, l1, h, col);
            g.fill(w - r1, 0, w - r0, h, col);
        }
    }

    private static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static void drawBlood(GuiGraphicsExtractor g, int w, int h, long now) {
        final int cell = 3;                                   // one blood "pixel" = 3 GUI units
        int cols = (w + cell - 1) / cell, rows = (h + cell - 1) / cell;
        for (Splash s : SPLASHES) {
            float age = (now - s.start) / (float) s.dur;
            if (age < 0f || age >= 1f) continue;
            float life = 1f - age;
            int depth = Math.max(3, (int) (s.strength * 16));
            for (int cy = 0; cy < rows; cy++) {
                int ey = Math.min(cy, rows - 1 - cy);
                if (ey < depth) {
                    for (int cx = 0; cx < cols; cx++) bloodCell(g, s, cx, cy, cols, rows, depth, life, cell, w, h);
                } else {
                    for (int cx = 0; cx < depth; cx++) bloodCell(g, s, cx, cy, cols, rows, depth, life, cell, w, h);
                    for (int cx = cols - depth; cx < cols; cx++) bloodCell(g, s, cx, cy, cols, rows, depth, life, cell, w, h);
                }
            }
        }
    }

    private static void bloodCell(GuiGraphicsExtractor g, Splash s, int cx, int cy, int cols, int rows,
                                  int depth, float life, int cell, int w, int h) {
        int dEdge = Math.min(Math.min(cx, cols - 1 - cx), Math.min(cy, rows - 1 - cy));
        float edgeF = 1f - dEdge / (float) depth;
        float rnd = hash(cx, cy, s.seed);
        float px = cx * cell / (float) h, py = cy * cell / (float) h;
        float aspect = w / (float) h;
        float best = 0f;
        for (float[] b : s.blots) {
            float dx = px - b[0] * aspect, dy = py - b[1];
            float v = 1f - (float) Math.sqrt(dx * dx + dy * dy) / b[2];
            if (v > best) best = v;
        }
        float coverage = best * 0.9f + edgeF * 0.35f;
        float score = coverage * (0.55f + 0.9f * life) - rnd * 0.55f;
        if (score < 0.08f) return;
        float alpha = 0.30f + 0.45f * Math.min(1f, score * 1.6f);       // mostly translucent, like glass
        if (hash(cx, cy, s.seed + 77) < 0.25f) alpha = Math.min(0.85f, alpha + 0.25f);
        alpha *= Math.min(1f, life * 1.8f);
        int a = (int) (alpha * 255f);
        if (a <= 3) return;
        int rgb = BLOOD[(int) (hash(cx, cy, s.seed + 13) * BLOOD.length) % BLOOD.length];
        int x0 = cx * cell, y0 = cy * cell;
        g.fill(x0, y0, Math.min(w, x0 + cell), Math.min(h, y0 + cell), (a << 24) | rgb);
    }

    private static void drawWebs(GuiGraphicsExtractor g, int w, int h) {
        final int s = 96;
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_TL, 0, 0, 0f, 0f, s, s, s, s);
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_TR, w - s, 0, 0f, 0f, s, s, s, s);
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_BL, 0, h - s, 0f, 0f, s, s, s, s);
        g.blit(RenderPipelines.GUI_TEXTURED, WEB_BR, w - s, h - s, 0f, 0f, s, s, s, s);
    }
}
