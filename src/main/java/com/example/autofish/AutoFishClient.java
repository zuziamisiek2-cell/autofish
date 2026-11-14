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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoFishClient implements ClientModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;
    private static int recastDelay = 0;
    private static int clickCooldown = 0;
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

            if (clickCooldown > 0) clickCooldown--;

            // Automatyczne zarzucanie wędki po wyłowieniu
            if (isHoldingRod(client) && client.player.fishHook == null && recastDelay == 0 && clickCooldown == 0) {
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
        AtomicBoolean shouldClick = new AtomicBoolean(false);

        title.visit((style, asString) -> {
            TextColor color = style.getColor();
            String hex = (color != null) ? String.format("#%06X", color.getRgb()) : "brak";
            sb.append(asString).append("(").append(hex).append(") ");

            // Jeśli to plus "+" i jego kolor jest INNY niż tło (#96D0FF) oraz INNY niż wędka (#2052FF)
            if (asString.contains("+") && color != null) {
                if (!hex.equalsIgnoreCase("#96D0FF") && !hex.equalsIgnoreCase("#2052FF")) {
                    shouldClick.set(true);
                }
            }
            return Optional.empty();
        }, Style.EMPTY);

        // Zawsze pokazujemy na czacie co mod aktualnie widzi
        String currentDebug = sb.toString();
        long now = System.currentTimeMillis();
        if (!currentDebug.equals(lastDebug) && (now - lastDebugTime > 400) && !currentDebug.isEmpty()) {
            lastDebug = currentDebug;
            lastDebugTime = now;
            if (currentDebug.length() > 100) currentDebug = currentDebug.substring(0, 100);
            client.player.sendMessage(Text.literal("§e[AutoFish Debug] " + currentDebug), false);
        }

        // Jeśli wykryto zmianę koloru na jakimkolwiek plusie -> KLIK!
        if (shouldClick.get() && clickCooldown == 0) {
            rightClick(client);
            client.player.sendMessage(Text.literal("§a[AutoFish] WYŁAPANO MINIGRĘ I KLIKNIĘTO!"), false);
            clickCooldown = 50; 
            recastDelay = 30;
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
