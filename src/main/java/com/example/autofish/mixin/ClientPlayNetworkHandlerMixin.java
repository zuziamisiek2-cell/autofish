package com.example.autofish.mixin;

import com.example.autofish.AutoFishClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onTitle", at = @At("HEAD"))
    private void onTitle(TitleS2CPacket packet, CallbackInfo ci) {
        if (packet.getTitle() != null) {
            AutoFishClient.processPacketText(packet.getTitle(), "TITLE");
        }
    }

    @Inject(method = "onOverlayMessage", at = @At("HEAD"))
    private void onOverlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        if (packet.getText() != null) {
            AutoFishClient.processPacketText(packet.getText(), "ACTIONBAR");
        }
    }
}
