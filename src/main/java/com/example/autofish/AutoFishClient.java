
           package com.example.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.FishingRodItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

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

            // Sprawdzanie aktywnych BossBarów (gdzie RGMC renderuje minigrę)
            checkBossBars(client);

            // Automatyczne zarzucanie wędki, jeśli spławika nie ma w wodzie
            if (isHoldingRod(client) && client.player.fishHook == null && recastDelay == 0) {
                rightClick(client);
                recastDelay = 40;
            }

            if (recastDelay > 0) {
                recastDelay--;
            }
        });
    }

    private void checkBossBars(MinecraftClient client) {
        try {
            BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
            // Pobieramy pole 'bossBars' z hud metodą refleksji, aby odczytać mapę aktywnych bossbarów
            Field field = BossBarHud.class.getDeclaredField("bossBars");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, ?> map = (Map<UUID, ?>) field.get(bossBarHud);

            for (Object entry : map.values()) {
                // entry to zazwyczaj ClientBossBar
                Field nameField = entry.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Text textName = (Text) nameField.get(entry);
                if (textName != null) {
                    String bossText = textName.getString();
                    // Wyświetlamy treść bossbara na czacie celowniczym raz na jakiś czas lub przy zmianie
                    // (Dla testów wypisujemy raz na 20 ticków żeby nie spamować)
                    if (client.player.age % 20 == 0) {
                        client.player.sendMessage(Text.literal("§b[BossBar Debug] " + bossText), false);
                    }

                    // Jeśli minigra daje sygnał do kliknięcia (np. zmiana koloru lub tekst), klikamy PPM:
                    // if (bossText.contains("...") || ...) { rightClick(client); }
                }
            }
        } catch (Exception ignored) {}
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
