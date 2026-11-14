
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

public class AutoFishClient implements ClientModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;

    private static int clickCooldown = 0;
    private static int recastDelay = 0;
    private static int hookAge = 0;

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

            if (clickCooldown > 0) clickCooldown--;

            if (client.player.fishHook != null) {
                hookAge++;
            } else {
                hookAge = 0;
            }

            if (recastDelay > 0) {
                recastDelay--;
                if (recastDelay == 0 && isHoldingRod(client) && client.player.fishHook == null) {
                    rightClick(client);
                    clickCooldown = 30;
                }
            }

            if (isHoldingRod(client) && client.player.fishHook == null && recastDelay == 0 && clickCooldown == 0) {
                recastDelay = 25;
            }
        });
    }

    public static void processSubtitle(Text subtitle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!enabled || client.player == null || subtitle == null) return;
        if (!isHoldingRod(client) || clickCooldown > 0) return;
        
        if (client.player.fishHook == null || hookAge < 20) return;

        List<ColoredChar> chars = new ArrayList<>();
        subtitle.visit((style, asString) -> {
            for (int i = 0; i < asString.length(); i++) {
                chars.add(new ColoredChar(asString.charAt(i), style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        if (chars.isEmpty()) return;

        for (int i = 0; i < chars.size(); i++) {
            ColoredChar current = chars.get(i);
            TextColor color = current.style.getColor();

            if (color != null) {
                int rgb = color.getRgb();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                boolean isRed = (r > 190 && g < 85 && b < 85);

                if (isRed && (current.style.isBold() || isCursorSymbol(current.character))) {
                    rightClick(client);
                    clickCooldown = 40;
                    recastDelay = 25;
                    hookAge = 0;
                    break;
                }
            }
        }
    }

    private static boolean isCursorSymbol(char c) {
        return c != '●' && c != '•' && c != ' ' && c != '[' && c != ']';
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

    private record ColoredChar(char character, Style style) {}
}
