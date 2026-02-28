package com.profittracker.price;

import com.google.gson.*;
import com.profittracker.SkyblockProfitTracker;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Fetches and caches Bazaar prices from the Hypixel API.
 * Mirrors BlingBling's updateGemCosts() which queries api.hypixel.net/skyblock/bazaar.
 *
 * Also fetches ore prices from Bazaar where available (e.g., enchanted diamonds).
 */
public class BazaarPriceFetcher {

    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Product ID -> price (instant sell or sell offer depending on config)
    private final ConcurrentHashMap<String, Double> bazaarPrices = new ConcurrentHashMap<>();
    private long lastFetchTime = 0;
    private boolean fetching = false;

    /**
     * Get the best price for a gemstone at a given tier.
     * Returns max(npcPrice, bazaarPrice) like BlingBling does.
     */
    public double getGemstonePrice(String gemName, int tier) {
        double npc = ItemPrices.gemstoneNpcPrice(tier);

        String mode = SkyblockProfitTracker.config.pricingMode;
        if ("npc".equals(mode)) {
            return npc;
        }

        String productId = ItemPrices.gemstoneBazaarId(gemName, tier);
        Double bz = bazaarPrices.get(productId);
        if (bz != null) {
            return Math.max(npc, bz);
        }
        return npc;
    }

    /**
     * Get the Bazaar price for an ore product ID, or fall back to NPC price.
     */
    public double getOrePrice(String itemNameLower) {
        double npc = ItemPrices.ORE_NPC_PRICES.getOrDefault(itemNameLower, 0.0);

        String mode = SkyblockProfitTracker.config.pricingMode;
        if ("npc".equals(mode)) return npc;

        // Try common Bazaar IDs for ores
        String bzId = oreToBazaarId(itemNameLower);
        if (bzId != null) {
            Double bz = bazaarPrices.get(bzId);
            if (bz != null) return Math.max(npc, bz);
        }
        return npc;
    }

    /**
     * Map ore names to their Bazaar product IDs where applicable.
     */
    private String oreToBazaarId(String itemNameLower) {
        return switch (itemNameLower) {
            case "diamond" -> "DIAMOND";
            case "coal" -> "COAL";
            case "iron ingot" -> "IRON_INGOT";
            case "gold ingot" -> "GOLD_INGOT";
            case "emerald" -> "EMERALD";
            case "lapis lazuli" -> "INK_SACK:4";
            case "redstone dust" -> "REDSTONE";
            case "obsidian" -> "OBSIDIAN";
            case "mithril" -> "MITHRIL_ORE";
            case "titanium" -> "TITANIUM_ORE";
            case "end stone" -> "ENDER_STONE";
            case "cobblestone" -> "COBBLESTONE";
            case "glacite" -> "GLACITE";
            case "umber" -> "UMBER";
            case "tungsten" -> "TUNGSTEN";
            default -> null;
        };
    }

    /**
     * Fetch prices asynchronously. Safe to call frequently; will skip if already fetching
     * or if the cache is still fresh.
     */
    public void fetchPricesAsync() {
        int refreshMs = SkyblockProfitTracker.config.priceRefreshMinutes * 60 * 1000;
        if (fetching || (System.currentTimeMillis() - lastFetchTime < refreshMs)) return;

        fetching = true;
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(BAZAAR_URL))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    parseBazaarResponse(response.body());
                    lastFetchTime = System.currentTimeMillis();
                    SkyblockProfitTracker.LOGGER.info("Bazaar prices updated ({} products)", bazaarPrices.size());
                } else {
                    SkyblockProfitTracker.LOGGER.warn("Bazaar API returned status {}", response.statusCode());
                }
            } catch (Exception e) {
                SkyblockProfitTracker.LOGGER.warn("Failed to fetch Bazaar prices: {}", e.getMessage());
            } finally {
                fetching = false;
            }
        });
    }

    private void parseBazaarResponse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("products")) return;

            boolean useSellOffer = "bazaar_buy".equals(SkyblockProfitTracker.config.pricingMode);
            JsonObject products = root.getAsJsonObject("products");

            for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                String productId = entry.getKey();
                JsonObject product = entry.getValue().getAsJsonObject();

                if (!product.has("quick_status")) continue;
                JsonObject qs = product.getAsJsonObject("quick_status");

                double price;
                if (useSellOffer) {
                    // "buyPrice" = price at which buy orders exist (what you get from sell offer)
                    price = qs.has("buyPrice") ? qs.get("buyPrice").getAsDouble() : 0;
                } else {
                    // "sellPrice" = instant sell price
                    price = qs.has("sellPrice") ? qs.get("sellPrice").getAsDouble() : 0;
                }

                if (price > 0) {
                    bazaarPrices.put(productId, price);
                }
            }
        } catch (Exception e) {
            SkyblockProfitTracker.LOGGER.warn("Failed to parse Bazaar response", e);
        }
    }

    public boolean hasPrices() {
        return !bazaarPrices.isEmpty();
    }

    public long getLastFetchTime() {
        return lastFetchTime;
    }
}
