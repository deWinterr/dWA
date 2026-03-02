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
 *
 * Stores BOTH instant-sell and sell-offer prices so the user can switch
 * pricing modes without needing a re-fetch.
 *
 * Hypixel Bazaar API quick_status fields:
 *   sellPrice = price of the lowest sell order (what you pay to INSTANT BUY)
 *   buyPrice  = price of the highest buy order (what you get to INSTANT SELL)
 */
public class BazaarPriceFetcher {

    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Product ID -> instant sell price (buyPrice in API = what you get selling instantly)
    private final ConcurrentHashMap<String, Double> instantSellPrices = new ConcurrentHashMap<>();
    // Product ID -> sell offer price (sellPrice in API = what sellers offer, you compete with them)
    private final ConcurrentHashMap<String, Double> sellOfferPrices = new ConcurrentHashMap<>();

    private long lastFetchTime = 0;
    private boolean fetching = false;

    /**
     * Get the best price for a gemstone at a given tier.
     * Priority: custom price > Bazaar > NPC.
     */
    public double getGemstonePrice(String gemName, int tier) {
        Double custom = SkyblockProfitTracker.config.customPrices.get(gemName.toLowerCase());
        if (custom != null) return custom;

        double npc = ItemPrices.gemstoneNpcPrice(tier);

        String mode = SkyblockProfitTracker.config.pricingMode;
        if ("npc".equals(mode)) {
            return npc;
        }

        String productId = ItemPrices.gemstoneBazaarId(gemName, tier);
        Double bz = getBazaarPrice(productId, mode);
        if (bz != null) {
            return Math.max(npc, bz);
        }
        return npc;
    }

    /**
     * Get the price for an ore item.
     * Priority: custom price > Bazaar > NPC.
     */
    public double getOrePrice(String itemNameLower) {
        Double custom = SkyblockProfitTracker.config.customPrices.get(itemNameLower);
        if (custom != null) return custom;

        double npc = ItemPrices.ORE_NPC_PRICES.getOrDefault(itemNameLower, 0.0);

        String mode = SkyblockProfitTracker.config.pricingMode;
        if ("npc".equals(mode)) return npc;

        String bzId = oreToBazaarId(itemNameLower);
        if (bzId != null) {
            Double bz = getBazaarPrice(bzId, mode);
            if (bz != null) return Math.max(npc, bz);
        }
        return npc;
    }

    /**
     * Returns the correct cached price for a product based on the pricing mode.
     */
    private Double getBazaarPrice(String productId, String mode) {
        if ("bazaar_buy".equals(mode)) {
            // Sell offer: user places a sell order, competing with other sellers
            return sellOfferPrices.get(productId);
        } else {
            // Instant sell: user accepts the highest buy order
            return instantSellPrices.get(productId);
        }
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
            case "nether quartz" -> "QUARTZ";
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
                    SkyblockProfitTracker.LOGGER.info("Bazaar prices updated ({} products)", instantSellPrices.size());
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

            JsonObject products = root.getAsJsonObject("products");

            for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                String productId = entry.getKey();
                JsonObject product = entry.getValue().getAsJsonObject();

                if (!product.has("quick_status")) continue;
                JsonObject qs = product.getAsJsonObject("quick_status");

                // buyPrice = highest buy order = what you get when you INSTANT SELL
                double instSell = qs.has("buyPrice") ? qs.get("buyPrice").getAsDouble() : 0;
                // sellPrice = lowest sell order = what sellers offer (you compete to SELL OFFER)
                double sellOffer = qs.has("sellPrice") ? qs.get("sellPrice").getAsDouble() : 0;

                if (instSell > 0) {
                    instantSellPrices.put(productId, instSell);
                }
                if (sellOffer > 0) {
                    sellOfferPrices.put(productId, sellOffer);
                }
            }
        } catch (Exception e) {
            SkyblockProfitTracker.LOGGER.warn("Failed to parse Bazaar response", e);
        }
    }

    public boolean hasPrices() {
        return !instantSellPrices.isEmpty();
    }

    public long getLastFetchTime() {
        return lastFetchTime;
    }
}
