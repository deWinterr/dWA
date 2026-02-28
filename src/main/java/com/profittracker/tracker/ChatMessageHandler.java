package com.profittracker.tracker;

import com.profittracker.SkyblockProfitTracker;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;

/**
 * Registers chat message listeners using Fabric API.
 * Delegates to ChatParser for actual message analysis.
 */
public class ChatMessageHandler {

    public static void register() {
        // Listen for game messages (system/action bar messages including sack notifications)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return; // Skip action bar messages

            try {
                // Only process if we're in a mining area (or always if area detection fails)
                if (SkyblockProfitTracker.areaDetector.isOnSkyblock()) {
                    ChatParser.processMessage(message);
                }
            } catch (Exception e) {
                SkyblockProfitTracker.LOGGER.debug("Error processing game message", e);
            }
        });

        // Also listen for regular chat messages (PRISTINE! messages come as chat)
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            try {
                if (SkyblockProfitTracker.areaDetector.isOnSkyblock()) {
                    ChatParser.processMessage(message);
                }
            } catch (Exception e) {
                SkyblockProfitTracker.LOGGER.debug("Error processing chat message", e);
            }
        });

        SkyblockProfitTracker.LOGGER.info("Chat message handlers registered");
    }
}
