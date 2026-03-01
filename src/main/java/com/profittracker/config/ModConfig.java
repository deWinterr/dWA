package com.profittracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.profittracker.SkyblockProfitTracker;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {

    // HUD position and scale
    public int hudX = 5;
    public int hudY = 5;
    public float hudScale = 1.0f;
    public boolean hudEnabled = true;

    // Pricing mode: "bazaar_sell" (instant sell), "bazaar_buy" (sell offer), "npc"
    public String pricingMode = "bazaar_sell";

    // Session timeout in seconds before auto-pausing (applies to gemstones;
    // ores always use a fixed 60s timeout since sack messages arrive every ~30s)
    public int sessionTimeoutSeconds = 60;

    // Whether to show item breakdown on HUD
    public boolean showItemBreakdown = true;

    // Maximum items to show in HUD breakdown
    public int maxBreakdownItems = 5;

    // Bazaar price refresh interval in minutes
    public int priceRefreshMinutes = 5;

    // Manual price overrides: item name (lowercase) -> price per raw item
    public Map<String, Double> customPrices = new LinkedHashMap<>();

    // --- Display toggles ---
    public boolean showProfit = true;
    public boolean showProfitPerHour = true;
    public boolean showTime = true;
    public boolean showItems = true;
    public boolean showPricingMode = true;

    // --- RGB colors (0xRRGGBB format, no alpha) ---
    public int labelColor = 0xAAAAAA;       // gray
    public int valueColor = 0x55FF55;       // green
    public int timeColor = 0xFFFFFF;        // white
    public int separatorColor = 0x555555;   // dark gray
    public int chatPrefixColor = 0xFFAA00;  // gold — color of [ProfitTracker] in chat

    // --- Gemstone rarity for pricing ---
    // 1 = Flawed (default, from PRISTINE! drops), 2 = Fine, 3 = Flawless
    public int gemstoneRarity = 1;

    // --- HUD style ---
    public boolean showSeparators = true;

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
