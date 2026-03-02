package com.profittracker.price;

import java.util.*;

/**
 * Static NPC sell prices for ores and gemstone NPC values.
 * Mirrors BigDiamond's PRICES object and BlingBling's NPC gemstone calculation.
 */
public class ItemPrices {

    /**
     * NPC sell prices per raw item (mirrors BigDiamond's PRICES).
     */
    public static final Map<String, Double> ORE_NPC_PRICES = new LinkedHashMap<>();
    static {
        ORE_NPC_PRICES.put("diamond", 8.0);
        ORE_NPC_PRICES.put("coal", 1.0);
        ORE_NPC_PRICES.put("iron ingot", 2.0);
        ORE_NPC_PRICES.put("gold ingot", 3.0);
        ORE_NPC_PRICES.put("emerald", 4.0);
        ORE_NPC_PRICES.put("lapis lazuli", 1.0);
        ORE_NPC_PRICES.put("redstone dust", 1.0);
        ORE_NPC_PRICES.put("obsidian", 7.0);
        ORE_NPC_PRICES.put("mithril", 8.0);
        ORE_NPC_PRICES.put("titanium", 20.0);
        ORE_NPC_PRICES.put("end stone", 2.0);
        ORE_NPC_PRICES.put("cobblestone", 0.5);
        ORE_NPC_PRICES.put("hard stone", 0.0);
        ORE_NPC_PRICES.put("glacite", 4.5);
        ORE_NPC_PRICES.put("umber", 3.0);
        ORE_NPC_PRICES.put("tungsten", 3.0);
        ORE_NPC_PRICES.put("nether quartz", 4.0);
    }

    /**
     * Gemstone types recognized from PRISTINE! messages and block metadata.
     * Mirrors BlingBling's gemstone list.
     */
    public static final List<String> GEMSTONE_TYPES = List.of(
            "Ruby", "Amethyst", "Jade", "Sapphire", "Amber",
            "Topaz", "Jasper", "Aquamarine", "Citrine", "Peridot", "Onyx"
    );

    /**
     * Gemstone tier names used in Bazaar product IDs.
     */
    public static final String[] GEMSTONE_TIERS = {"ROUGH", "FLAWED", "FINE", "FLAWLESS", "PERFECT"};

    /**
     * NPC sell price for a gemstone at a given tier.
     * Formula: 3 * 80^tier (from BlingBling's getGemCost).
     * Tier 0=Rough, 1=Flawed, 2=Fine, 3=Flawless, 4=Perfect
     */
    public static double gemstoneNpcPrice(int tier) {
        return 3.0 * Math.pow(80, tier);
    }

    /**
     * Build the Bazaar product ID for a gemstone.
     * e.g., "FLAWED_RUBY_GEM", "FINE_SAPPHIRE_GEM"
     */
    public static String gemstoneBazaarId(String gemName, int tier) {
        return GEMSTONE_TIERS[tier] + "_" + gemName.toUpperCase() + "_GEM";
    }

    /**
     * Returns true if the given item name (lowercase) is a tracked ore.
     */
    public static boolean isTrackedOre(String itemNameLower) {
        return ORE_NPC_PRICES.containsKey(itemNameLower);
    }

    /**
     * Returns true if the given name is a recognized gemstone type.
     */
    public static boolean isGemstone(String name) {
        for (String gem : GEMSTONE_TYPES) {
            if (gem.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
