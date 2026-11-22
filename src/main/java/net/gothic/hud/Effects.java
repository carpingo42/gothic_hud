package net.gothic.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        int corners;                                     // bit mask of the screen corners
        final List<float[]> blots = new ArrayList<>();   // {corner, i, j, radius} in cells from the corner
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

        long now = System.currentTimeMillis();
        SPLASHES.removeIf(s -> now - s.start > s.dur);
    }

    private static void beat(Minecraft mc) {
        float volume = running ? 0.9f : 0.6f;
        float pitch = 0.92f + RNG.nextFloat() * 0.16f;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(HEARTBEAT, pitch, volume));
        beatAt = System.currentTimeMillis();
        // random next moment: rare when calm, frequent when sprinting or hurt
        int interval = running ? 60 + RNG.nextInt(100) : 200 + RNG.nextInt(360);
        interval = (int) (interval * (1.0f - 0.5f * lowHealth));
        nextBeat = tick + Math.max(30, interval);
        if (RNG.nextFloat() < 0.35f) startTwitch();
    }

    private static void startTwitch() {
        long now = System.currentTimeMillis();
        twitchUntil = now + 140 + RNG.nextInt(260);
        nextJitter = 0L;
    }

    private static void onHurt(float damage) {
        long now = System.currentTimeMillis();
        Splash s = new Splash();
        s.start = now;
        s.dur = (long) (2200 + Math.min(1800f, damage * 220f));
        s.seed = RNG.nextInt();
        s.strength = Mth.clamp(0.5f + damage / 12f, 0.5f, 1f);
        // blood only in the screen corners
        int count = 2 + RNG.nextInt(3);
        while (Integer.bitCount(s.corners) < count) s.corners |= 1 << RNG.nextInt(4);
        for (int c = 0; c < 4; c++) {
            if ((s.corners & (1 << c)) == 0) continue;
            int blots = 1 + RNG.nextInt(2);
            for (int k = 0; k < blots; k++) {
                float r = (5f + RNG.nextFloat() * 5f) * (0.7f + 0.5f * s.strength);
                s.blots.add(new float[] { c, RNG.nextFloat() * 3f, RNG.nextFloat() * 3f, r });
            }
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
        long now = System.currentTimeMillis();
        drawVignette(g, w, h, now);
        drawBlood(g, w, h, now);
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
        final int cell = 6;                                   // chunky pixels, few of them
        final int n = 16;                                     // corner square, in cells
        int cols = (w + cell - 1) / cell, rows = (h + cell - 1) / cell;
        for (Splash s : SPLASHES) {
            float age = (now - s.start) / (float) s.dur;
            if (age < 0f || age >= 1f) continue;
            float life = 1f - age;
            for (int corner = 0; corner < 4; corner++) {
                if ((s.corners & (1 << corner)) == 0) continue;
                boolean right = (corner & 1) != 0, bottom = (corner & 2) != 0;
                for (int j = 0; j < n; j++) {
                    for (int i = 0; i < n; i++) {
                        int cx = right ? cols - 1 - i : i, cy = bottom ? rows - 1 - j : j;
                        bloodCell(g, s, corner, i, j, cx, cy, life, cell, w, h);
                    }
                }
            }
        }
    }

    private static void bloodCell(GuiGraphicsExtractor g, Splash s, int corner, int i, int j, int cx, int cy,
                                  float life, int cell, int w, int h) {
        float best = 0f;
        for (float[] b : s.blots) {
            if ((int) b[0] != corner) continue;
            float dx = i - b[1], dy = j - b[2];
            float v = 1f - (float) Math.sqrt(dx * dx + dy * dy) / b[3];
            if (v > best) best = v;
        }
        float rnd = hash(cx, cy, s.seed);
        float score = best * (0.6f + 0.7f * life) - rnd * 0.45f;
        if (score < 0.06f) return;
        float alpha = (0.26f + 0.18f * Math.min(1f, score * 2f)) * Math.min(1f, life * 1.8f);   // always translucent
        int a = (int) (alpha * 255f);
        if (a <= 3) return;
        int rgb = hash(cx, cy, s.seed + 13) < 0.5f ? 0xB0141C : 0x8C0C14;
        int x0 = cx * cell, y0 = cy * cell;
        g.fill(x0, y0, Math.min(w, x0 + cell), Math.min(h, y0 + cell), (a << 24) | rgb);
    }
}
