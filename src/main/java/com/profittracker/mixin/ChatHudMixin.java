package com.profittracker.mixin;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.tracker.ChatParser;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ChatHud to intercept messages with their full Text objects,
 * preserving hover events needed for sack message parsing.
 *
 * Targets the addMessage method that receives the Text content.
 * The deduplication in ChatParser prevents double-counting alongside Fabric API events.
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    /**
     * Intercept addMessage(Text, MessageSignatureData, MessageIndicator).
     * This is the internal method that all chat messages flow through in 1.21.x.
     * If the method signature differs in your exact build, the Fabric API
     * ClientReceiveMessageEvents in ChatMessageHandler acts as the primary fallback.
     */
    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            require = 0  // Don't crash if method signature changed; Fabric API events are the fallback
    )
    private void onAddMessageFull(Text message, Object signatureData, Object indicator, CallbackInfo ci) {
        handleMessage(message);
    }

    /**
     * Fallback target for simpler addMessage(Text) if it exists.
     */
    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"),
            require = 0
    )
    private void onAddMessageSimple(Text message, CallbackInfo ci) {
        handleMessage(message);
    }

    private void handleMessage(Text message) {
        try {
            if (SkyblockProfitTracker.areaDetector != null
                    && SkyblockProfitTracker.areaDetector.isOnSkyblock()) {
                ChatParser.processMessage(message);
            }
        } catch (Exception e) {
            SkyblockProfitTracker.LOGGER.debug("Mixin: error processing message", e);
        }
    }
}
