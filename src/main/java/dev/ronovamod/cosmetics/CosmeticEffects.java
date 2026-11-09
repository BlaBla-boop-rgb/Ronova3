package dev.ronovamod.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Partikel-Cosmetics rund um den eigenen Spieler.
 * Rein clientseitig: Du siehst sie (am besten in der 3rd-Person-Ansicht, F5), andere Spieler nicht.
 */
public final class CosmeticEffects {
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(0xC0182A, 0.9f);
    private static final DustParticleOptions EMBER = new DustParticleOptions(0xFF4A4A, 0.6f);
    private static final DustParticleOptions RAVEN = new DustParticleOptions(0x1A1018, 1.3f);

    private static int age = 0;
    private static boolean loggedStart = false;
    private static int lastLog = -1;
    private static double lastX, lastZ;

    private CosmeticEffects() {}

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || player.isSpectator() || player.isInvisible()) return;

        CosmeticsConfig cfg = CosmeticsConfig.get();
        age++;
        if (!loggedStart || age - lastLog >= 600) {
            loggedStart = true;
            lastLog = age;
            dev.ronovamod.cosmetics.CosmeticsClient.LOGGER.info(
                    "[Diagnose] Cosmetics-Tick läuft: halo={} aura={} raben={} spur={} | Partikel-Einstellung={} | Level={}",
                    cfg.halo, cfg.soulAura, cfg.crowSwarm, cfg.deathTrail,
                    particleSetting(mc), level.getClass().getName());
        }
        double x = player.getX(), y = player.getY(), z = player.getZ();
        double head = y + player.getBbHeight();

        // Blutroter Heiligenschein: rotierender Ring über dem Kopf
        if (cfg.halo && age % 2 == 0) {
            int points = 14;
            double spin = age * 0.05;
            for (int i = 0; i < points; i++) {
                double a = spin + (Math.PI * 2 / points) * i;
                level.addParticle(i % 4 == 0 ? EMBER : CRIMSON,
                        x + Math.cos(a) * 0.38, head + 0.45, z + Math.sin(a) * 0.38, 0, 0, 0);
            }
        }

        // Seelenaura: aufsteigende Seelen um die Füße
        if (cfg.soulAura && age % 6 == 0) {
            double a = level.getRandom().nextDouble() * Math.PI * 2;
            double r = 0.5 + level.getRandom().nextDouble() * 0.3;
            level.addParticle(ParticleTypes.SOUL, x + Math.cos(a) * r, y + 0.1, z + Math.sin(a) * r, 0, 0.03, 0);
        }

        // Rabenschwarm: drei dunkle "Raben", die um die Schultern kreisen
        if (cfg.crowSwarm) {
            for (int c = 0; c < 3; c++) {
                double a = age * 0.12 + c * (Math.PI * 2 / 3);
                double bob = Math.sin(age * 0.2 + c) * 0.15;
                double cx = x + Math.cos(a) * 1.1, cz = z + Math.sin(a) * 1.1;
                double cy = y + player.getBbHeight() * 0.8 + bob;
                level.addParticle(RAVEN, cx, cy, cz, 0, 0, 0);
                // Flügelschlag: zwei Punkte links/rechts, abwechselnd hoch/runter
                double flap = (age + c * 3) % 6 < 3 ? 0.12 : -0.05;
                double tx = -Math.sin(a) * 0.15, tz = Math.cos(a) * 0.15;
                level.addParticle(RAVEN, cx + tx, cy + flap, cz + tz, 0, 0, 0);
                level.addParticle(RAVEN, cx - tx, cy + flap, cz - tz, 0, 0, 0);
            }
        }

        // Todesspur: Seelenfeuer, wo du entlangläufst
        if (cfg.deathTrail) {
            double moved = Math.abs(x - lastX) + Math.abs(z - lastZ);
            if (moved > 0.05 && player.onGround() && age % 2 == 0) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.05, z, 0, 0.01, 0);
                level.addParticle(CRIMSON, x, y + 0.1, z, 0, 0, 0);
            }
        }
        lastX = x;
        lastZ = z;
    }

    /** Liest die Partikel-Einstellung (Alle/Verringert/Minimal) per Reflection, damit der Build nicht davon abhängt. */
    private static String particleSetting(Minecraft mc) {
        try {
            Object option = mc.options.getClass().getMethod("particles").invoke(mc.options);
            return String.valueOf(option.getClass().getMethod("get").invoke(option));
        } catch (Exception e) {
            return "unbekannt";
        }
    }
}
