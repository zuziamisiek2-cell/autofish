package com.example.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.FishingRodItem;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AutoFishClient implements ClientModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;
    private static int recastDelay = 0;
    private static String lastDebug = "";
    private static long lastDebugTime = 0;

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

            if (!enabled || client.player == null || client.interactionManager == null) return;

            // Automatyczne zarzucanie wędki po wyłowieniu
            if (isHoldingRod(client) && client.player.fishHook == null && recastDelay == 0) {
                rightClick(client);
                recastDelay = 40;
            }

            if (recastDelay > 0) {
                recastDelay--;
            }
        });
    }

    public static void processTitle(Text title) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!enabled || client.player == null || title == null) return;
        if (!isHoldingRod(client)) return;

        StringBuilder sb = new StringBuilder();
        title.visit((style, asString) -> {
            TextColor color = style.getColor();
            String hex = (color != null) ? String.format("#%06X", color.getRgb()) : "brak";
            sb.append(asString).append("(").append(hex).append(") ");
            return java.util.Optional.empty();
        }, Style.EMPTY);

        String currentDebug = sb.toString();
        long now = System.currentTimeMillis();
        
        // Wypisujemy na czat stan paska co 0.5 sekundy, jeśli uległ zmianie
        if (!currentDebug.equals(lastDebug) && (now - lastDebugTime > 500)) {
            lastDebug = currentDebug;
            lastDebugTime = now;
            if (currentDebug.length() > 110) currentDebug = currentDebug.substring(0, 110);
            client.player.sendMessage(Text.literal("§e[Analiza HEX] " + currentDebug), false);
        }
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
