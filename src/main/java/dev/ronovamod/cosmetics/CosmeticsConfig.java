package dev.ronovamod.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Alle Schalter der Mod. Wird als config/ronova/cosmetics.json gespeichert. */
public final class CosmeticsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean halo = true;
    public boolean soulAura = true;
    public boolean crowSwarm = false;
    public boolean deathTrail = false;


    private static CosmeticsConfig instance = new CosmeticsConfig();

    public static CosmeticsConfig get() {
        return instance;
    }

    public static Path folder() {
        return FabricLoader.getInstance().getConfigDir().resolve("ronova");
    }


    public static void load() {
        Path file = folder().resolve("cosmetics.json");
        try {
            Files.createDirectories(folder());
            if (Files.exists(file)) {
                CosmeticsConfig loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), CosmeticsConfig.class);
                if (loaded != null) instance = loaded;
            }
        } catch (Exception e) {
            CosmeticsClient.LOGGER.warn("Konnte Ronova-Config nicht laden, nutze Standardwerte", e);
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(folder());
            Files.writeString(folder().resolve("cosmetics.json"), GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CosmeticsClient.LOGGER.warn("Konnte Ronova-Config nicht speichern", e);
        }
    }
}
