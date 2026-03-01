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

public class SkyblockProfitTracker implements ClientModInitializer {

    public static final String MOD_ID = "skyblock-profit-tracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ModConfig config;
    public static ProfitSession session;
    public static BazaarPriceFetcher priceFetcher;
    public static ProfitHudOverlay hudOverlay;
    public static AreaDetector areaDetector;

    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Skyblock Profit Tracker initializing for 1.21.10...");

        config = ModConfig.load();

        session = new ProfitSession();
        priceFetcher = new BazaarPriceFetcher();
        hudOverlay = new ProfitHudOverlay();
        areaDetector = new AreaDetector();

        ChatMessageHandler.register();
        hudOverlay.register();
        TrackerCommand.register();

        // Client tick handler — only used for periodic checks, not timing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Check for idle timeout (uses System.currentTimeMillis() internally)
            session.checkTimeout();

            // Periodic Bazaar price refresh (every ~6000 ticks ~ 5 minutes)
            tickCounter++;
            if (tickCounter >= 6000) {
                tickCounter = 0;
                priceFetcher.fetchPricesAsync();
            }
        });

        priceFetcher.fetchPricesAsync();

        LOGGER.info("Skyblock Profit Tracker loaded! Commands: /pt or /profittracker");
    }
}
