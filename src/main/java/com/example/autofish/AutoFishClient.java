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
    private static String lastPrinted = "";

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
            if (color != null) {
                int rgb = color.getRgb();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                sb.append(asString).append(String.format("[%d,%d,%d] ", r, g, b));
            } else {
                sb.append(asString).append("[def] ");
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        String result = sb.toString().trim();
        // Wypisujemy na czat tylko wtedy, gdy stan się zmieni (żeby nie spamować)
        if (!result.equals(lastPrinted) && !result.isEmpty()) {
            lastPrinted = result;
            // Skracamy do 100 znaków żeby nie zaspamowało ekranu
            if (result.length() > 100) result = result.substring(0, 100);
            client.player.sendMessage(Text.literal("§e[Kolory] " + result), false);
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
