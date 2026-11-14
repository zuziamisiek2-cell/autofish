
           package com.example.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.FishingRodItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AutoFishClient implements ClientModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;
    private static int recastDelay = 0;

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
                            true
                    );
                }
            }

            if (!enabled || client.player == null || client.interactionManager == null) return;

            // Automatyczne zarzucanie wędki, jeśli spławika nie ma w wodzie
            if (isHoldingRod(client) && client.player.fishHook == null && recastDelay == 0) {
                rightClick(client);
                recastDelay = 40; // Odczekaj 2 sekundy przed kolejnym rzutem
            }

            if (recastDelay > 0) {
                recastDelay--;
            }
        });
    }

    // Ta funkcja przechwytuje tekst wysyłany na pasek akcji (Action Bar) nad ekwipunkiem
    public static void processActionBar(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!enabled || client.player == null || text == null) return;

        String message = text.getString();
        
        // Wyświetlamy na czacie to, co wysyła serwer, żebyś widział, co plugin "widzi"
        client.player.sendMessage(Text.literal("§e[Debug RGMC] " + message), false);

        // Tutaj wstawimy automatyczne kliknięcie, gdy odkryjemy, jaki tekst wysyła serwer RGMC
        // np. if (message.contains("Kliknij") || message.contains("Złów")) { rightClick(client); }
    }

    private static boolean isHoldingRod(MinecraftClient client) {
        if (client.player == null) return false;
        return client.player.getMainHandStack().getItem() instanceof FishingRodItem
                || client.player.getOffHandStack().getItem() instanceof FishingRodItem;
    }

    private static void rightClick(MinecraftClient client) {
        if (client.interactionManager == null || client.player == null) return;
        Hand hand = client.player.getMainHandStack().getItem() instanceof FishingRodItem
                ? Hand.MAIN_HAND : Hand.OFF_HAND;
        client.interactionManager.interactItem(client.player, hand);
        client.player.swingHand(hand);
    }
}
