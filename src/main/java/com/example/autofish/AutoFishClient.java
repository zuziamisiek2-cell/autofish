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
        if (!isHoldingRod(client) || clickCooldown > 0) return;

        AtomicBoolean shouldClick = new AtomicBoolean(false);

        title.visit((style, asString) -> {
            TextColor color = style.getColor();
            if (color != null && asString.contains("+")) {
                int rgb = color.getRgb();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Standardowe tło plusa to [150, 208, 255]. 
                // Jeśli kolor jakiegoś plusa znacząco odbiega od tła (np. inny odcień, jaskrawy, czerwony lub zielony), to jest to wskaźnik!
                boolean isStandardBackground = (r == 150 && g == 208 && b == 255) || (r == 32 && g == 82 && b == 255);
                
                if (!isStandardBackground) {
                    shouldClick.set(true);
                    return Optional.of(true);
                }
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (shouldClick.get()) {
            rightClick(client);
            client.player.sendMessage(Text.literal("§a[AutoFish] Złapano moment minigry!"), true);
            clickCooldown = 40; // Blokada na 2 sekundy po kliknięciu
            recastDelay = 25;
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
