package com.example.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AutoFishClient implements ClientModInitializer {

    public static boolean enabled = false;
    private static KeyBinding toggleKey;

    private static int clickCooldown = 0;
    private static int recastDelay = 0;
    private static int hookWaitTicks = 0;

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

            FishingBobberEntity bobber = client.player.fishHook;

            if (bobber != null) {
                hookWaitTicks++;
                
                // Ignorujemy pierwsze 1.5 sekundy po zarzuceniu, żeby nie reagować na plusk przy rzucie
                if (hookWaitTicks > 30 && clickCooldown == 0) {
                    // Kiedy ryba bierze, spławik opada gwałtownie w dół (prędkość Y mocno spada)
                    if (bobber.getVelocity().y < -0.06) {
                        rightClick(client);
                        clickCooldown = 40; 
                        recastDelay = 25;   
                        hookWaitTicks = 0;
                    }
                }
            } else {
                hookWaitTicks = 0;
            }

            if (recastDelay > 0) {
                recastDelay--;
                if (recastDelay == 0 && isHoldingRod(client) && client.player.fishHook == null && clickCooldown == 0) {
                    rightClick(client);
                    clickCooldown = 30;
                }
            }

            if (isHoldingRod(client) && client.player.fishHook == null && recastDelay == 0 && clickCooldown == 0) {
                recastDelay = 15;
            }
        });
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
