package dev.ronovamod.cosmetics;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ronovamod.cosmetics.gui.CosmeticsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CosmeticsClient implements ClientModInitializer {
    public static final String MOD_ID = "ronova_cosmetics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** /ronona wird im Chat ausgeführt - der Chat schließt sich danach, deshalb öffnen wir das GUI einen Tick später. */
    private static boolean openScreenNextTick = false;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        CosmeticsConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openScreenNextTick) {
                openScreenNextTick = false;
                setScreen(client, new CosmeticsScreen());
            }
            CosmeticEffects.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> literal = clientLiteral("ronona");
            if (literal == null) {
                LOGGER.error("Fabric-Client-Befehle nicht gefunden - /ronona ist nicht verfügbar.");
                return;
            }
            dispatcher.register(literal.executes(ctx -> {
                openScreenNextTick = true;
                return 1;
            }));
        });

        LOGGER.info("Ronova Cosmetics geladen.");
    }

    /* --------------------------------------------------------------------------------------------
     * Kleine Kompatibilitäts-Helfer: Fabric und Mojang haben zwischen 26.1 und 26.2 einige Namen
     * geändert. Per Reflection funktionieren beide Varianten, ohne dass der Build bricht.
     * ------------------------------------------------------------------------------------------ */

    @SuppressWarnings("unchecked")
    private static LiteralArgumentBuilder<FabricClientCommandSource> clientLiteral(String name) {
        for (String cls : new String[]{
                "net.fabricmc.fabric.api.client.command.v2.ClientCommands",
                "net.fabricmc.fabric.api.client.command.v2.ClientCommandManager"}) {
            try {
                Method m = Class.forName(cls).getMethod("literal", String.class);
                return (LiteralArgumentBuilder<FabricClientCommandSource>) m.invoke(null, name);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        // 26.2+: mc.gui.setScreen(screen)
        try {
            Field guiField = Minecraft.class.getField("gui");
            Object gui = guiField.get(mc);
            Method set = gui.getClass().getMethod("setScreen", Screen.class);
            set.invoke(gui, screen);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        // ältere Variante: mc.setScreen(screen)
        try {
            Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, screen);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Konnte das Ronova-GUI nicht öffnen", e);
        }
    }

}
