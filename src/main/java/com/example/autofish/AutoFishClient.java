package com.example.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class AutoFishClient implements ClientModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autofishcustom.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.autofishcustom"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("§7[AutoFish] Status: " + (enabled ? "§aWLACZONY" : "§cWYLACZONY")),
                            false
                    );
                }
            }
        });
    }

    public static void processTitle(Text title) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || title == null) return;

        // Bez żadnych warunków – wypisujemy na czat każdą klatkę tytułu z serwera!
        String rawText = title.getString();
        
        StringBuilder sb = new StringBuilder();
        title.visit((style, asString) -> {
            TextColor color = style.getColor();
            String hex = (color != null) ? String.format("#%06X", color.getRgb()) : "brak";
            sb.append(asString).append("(").append(hex).append(") ");
            return Optional.empty();
        }, Style.EMPTY);

        // Wysyłamy na czat, żeby na 100% zobaczyć, co serwer przekazuje do mixina
        client.player.sendMessage(Text.literal("§b[TYTUŁ] " + rawText), false);
        client.player.sendMessage(Text.literal("§e[KOLORY] " + sb.toString()), false);
    }
}
