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
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addMessage(
                            Text.literal("§7[AutoFish] Status: " + (enabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY"))
                    );
                }
            }

            if (!enabled || client.player == null || client.interactionManager == null) return;

            if (clickCooldown > 0) clickCooldown--;

            if (isHoldingRod(client) && client.player.fishHook == null && recastDelay == 0 && clickCooldown == 0) {
                rightClick(client);
                recastDelay = 40;
            }

            if (recastDelay > 0) {
                recastDelay--;
            }
        });
    }

    public static void processPacketText(Text text, String packetType) {
        if (text == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;

        List<String> plusColors = new ArrayList<>();

        text.visit((style, asString) -> {
            if (asString.contains("+")) {
                TextColor color = style.getColor();
                String hex = (color != null) ? String.format("#%06X", color.getRgb()) : "brak";
                plusColors.add(hex);
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (plusColors.isEmpty()) return;

        int indicatorIndex = -1;
        for (int i = 0; i < plusColors.size(); i++) {
            if (!plusColors.get(i).equalsIgnoreCase("#96D0FF")) {
                indicatorIndex = i;
                break;
            }
        }

        String debugStr = "[" + packetType + "] Indeks: " + (indicatorIndex != -1 ? indicatorIndex : "brak");
        long now = System.currentTimeMillis();
        if (!debugStr.equals(lastDebug) && (now - lastDebugTime > 250)) {
            lastDebug = debugStr;
            lastDebugTime = now;
            client.inGameHud.getChatHud().addMessage(Text.literal("§e[AutoFish Packet] " + debugStr + " | Pasek: " + plusColors));
        }

        if (enabled && client.player != null && isHoldingRod(client) && clickCooldown == 0 && indicatorIndex != -1) {
            if (indicatorIndex == 4) {
                rightClick(client);
                client.inGameHud.getChatHud().addMessage(Text.literal("§a[AutoFish] Trafiono w cel z pakietu! Kliknięto PPM."));
                clickCooldown = 60;
                recastDelay = 30;
            }
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
