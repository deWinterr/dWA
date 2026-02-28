package com.profittracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.profittracker.SkyblockProfitTracker;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mod configuration. Saved to config/skyblock-profit-tracker.json.
 * Mirrors BigDiamond's config (HUD position, scale) plus BlingBling's pricing options.
 */
public class ModConfig {

    // HUD position and scale (like BigDiamond's config)
    public int hudX = 5;
    public int hudY = 5;
    public float hudScale = 1.0f;
    public boolean hudEnabled = true;

    // Pricing mode: "bazaar_sell" (instant sell), "bazaar_buy" (sell offer), "npc"
    public String pricingMode = "bazaar_sell";

    // Session timeout in seconds before auto-pausing (like BigDiamond's 60s timeout)
    public int sessionTimeoutSeconds = 60;

    // Whether to include rough gemstone estimates (like BlingBling's roughGems option)
    public boolean includeRoughEstimate = false;

    // Whether to show item breakdown on HUD
    public boolean showItemBreakdown = true;

    // Maximum items to show in HUD breakdown
    public int maxBreakdownItems = 5;

    // Bazaar price refresh interval in minutes
    public int priceRefreshMinutes = 5;

    // Discord webhook (like BigDiamond)
    public String discordWebhook = "";

    // Manual price overrides: item name (lowercase) -> price per raw item
    // These take priority over Bazaar and NPC prices
    public Map<String, Double> customPrices = new LinkedHashMap<>();

    // ----- Persistence -----

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("skyblock-profit-tracker.json");
    }

    public static ModConfig load() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) return config;
            } catch (Exception e) {
                SkyblockProfitTracker.LOGGER.warn("Failed to load config, using defaults", e);
            }
        }
        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            SkyblockProfitTracker.LOGGER.warn("Failed to save config", e);
        }
    }
}
