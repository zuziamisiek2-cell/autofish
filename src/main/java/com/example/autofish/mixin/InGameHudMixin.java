package com.example.autofish.mixin;

import com.example.autofish.AutoFishClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        AutoFishClient.logIncomingText("ACTIONBAR", message);
    }

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void onSetTitle(Text title, CallbackInfo ci) {
        AutoFishClient.logIncomingText("TITLE", title);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void onSetSubtitle(Text subtitle, CallbackInfo ci) {
        AutoFishClient.logIncomingText("SUBTITLE", subtitle);
    }
}
