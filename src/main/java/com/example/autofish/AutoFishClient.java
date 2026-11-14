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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        List<String> plusColors = new ArrayList<>();

        title.visit((style, asString) -> {
            if (asString.contains("+")) {
                TextColor color = style.getColor();
                String hex = (color != null) ? String.format("#%06X", color.getRgb()) : "brak";
                plusColors.add(hex);
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (plusColors.isEmpty()) return;

        // Szukamy, na którym indeksie znajduje się czerwony wskaźnik #FF5858
        int redIndex = -1;
        for (int i = 0; i < plusColors.size(); i++) {
            if (plusColors.get(i).equalsIgnoreCase("#FF5858")) {
                redIndex = i;
                break;
            }
        }

        // Pokazujemy stan na czacie co 0.4 sekundy
        String debugStr = "Pozycja czerwonego: " + (redIndex != -1 ? "Indeks " + redIndex : "brak");
        long now = System.currentTimeMillis();
        if (!debugStr.equals(lastDebug) && (now - lastDebugTime > 400)) {
            lastDebug = debugStr;
            lastDebugTime = now;
            client.player.sendMessage(Text.literal("§e[AutoFish Indeks] " + debugStr + " | Wszystkie: " + plusColors), false);
        }

        // TUTAJ USTALIMY INDEKS DO KLIKNIĘCIA (np. gdy czerwony dojdzie do środka, np. indeks 4 lub 5)
        // Na razie testujemy i sprawdzamy na czacie, który indeks oznacza wygraną!
        /*
        if (redIndex == 4 && clickCooldown == 0) {
            rightClick(client);
            client.player.sendMessage(Text.literal("§a[AutoFish] Kliknięto na indeksie 4!"), false);
            clickCooldown = 50;
            recastDelay = 30;
        }
        */
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
