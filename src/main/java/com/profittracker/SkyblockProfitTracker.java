package com.profittracker;

import com.profittracker.command.TrackerCommand;
import com.profittracker.config.ModConfig;
import com.profittracker.hud.ProfitHudOverlay;
import com.profittracker.price.BazaarPriceFetcher;
import com.profittracker.tracker.ChatMessageHandler;
import com.profittracker.tracker.ProfitSession;
import com.profittracker.util.AreaDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skyblock Profit Tracker - Fabric 1.21.10
 *
 * A client-side mod that tracks ore and gemstone mining profits in Hypixel SkyBlock.
 * Inspired by BigDiamond (ore tracking via sack messages) and BlingBlingAddons (gemstone
 * tracking via PRISTINE! procs with Bazaar pricing).
 *
 * Features:
 * - Automatic ore detection from [Sacks] chat messages (with enchanted item multipliers)
 * - Gemstone PRISTINE! proc tracking
 * - Real-time Bazaar price fetching (with NPC fallback)
 * - Configurable HUD overlay showing profit, $/hr, time, item breakdown
 * - Session timeout with auto-pause and summary
 * - Commands for all configuration
 * - Discord webhook support
 */
public class SkyblockProfitTracker implements ClientModInitializer {

    public static final String MOD_ID = "skyblock-profit-tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Global instances
    public static ModConfig config;
    public static ProfitSession session;
    public static BazaarPriceFetcher priceFetcher;
    public static ProfitHudOverlay hudOverlay;
    public static AreaDetector areaDetector;

    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Skyblock Profit Tracker initializing for 1.21.10...");

        // Load config
        config = ModConfig.load();

        // Initialize systems
        session = new ProfitSession();
        priceFetcher = new BazaarPriceFetcher();
        hudOverlay = new ProfitHudOverlay();
        areaDetector = new AreaDetector();

        // Register chat message listeners
        ChatMessageHandler.register();

        // Register HUD renderer
        hudOverlay.register();

        // Register commands (/pt, /profittracker)
        TrackerCommand.register();

        // Client tick handler for session management and periodic tasks
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Session timeout check
            session.tick();

            // Periodic Bazaar price refresh (every ~6000 ticks ≈ 5 minutes)
            tickCounter++;
            if (tickCounter >= 6000) {
                tickCounter = 0;
                priceFetcher.fetchPricesAsync();
            }
        });

        // Initial Bazaar price fetch
        priceFetcher.fetchPricesAsync();

        LOGGER.info("Skyblock Profit Tracker loaded! Commands: /pt or /profittracker");
    }
}
