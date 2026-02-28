package com.profittracker.tracker;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.price.ItemPrices;
import com.profittracker.util.FormatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a single mining session's items, profits, and timing.
 * Combines BigDiamond's profitTracker.js session logic with BlingBling's gemstone tracking.
 */
public class ProfitSession {

    // Item name (lowercase) -> count of raw items collected
    private final ConcurrentHashMap<String, Long> oreItems = new ConcurrentHashMap<>();

    // Gemstone name -> count at flawed tier (pristine procs)
    private final ConcurrentHashMap<String, Long> gemstoneItems = new ConcurrentHashMap<>();

    private long startTime = 0;        // Epoch ms when session started
    private long lastUpdateTime = 0;   // Epoch ms of last item gain
    private boolean active = false;
    private boolean paused = false;     // True when timeout reached but not reset
    private long pausedElapsed = 0;     // Elapsed time when paused (to freeze display)

    // Cached computed values
    private long totalProfit = 0;
    private long profitPerHour = 0;
    private long elapsedSeconds = 0;

    /**
     * Called every client tick to check for session timeout.
     */
    public void tick() {
        if (!active || paused) return;

        long now = System.currentTimeMillis();
        int timeoutMs = SkyblockProfitTracker.config.sessionTimeoutSeconds * 1000;

        if (lastUpdateTime > 0 && (now - lastUpdateTime) > timeoutMs) {
            pause();
        } else {
            recalculate();
        }
    }

    /**
     * Add raw ore items to the session (from sack messages).
     * Item name should be lowercase, matching keys in ORE_NPC_PRICES.
     */
    public void addOre(String itemNameLower, long amount) {
        if (!active) {
            start();
        }
        if (paused) resume(); // Must resume before updating lastUpdateTime so idle time is computed correctly
        oreItems.merge(itemNameLower, amount, Long::sum);
        lastUpdateTime = System.currentTimeMillis();
        recalculate();
    }

    /**
     * Add gemstone pristine procs (flawed tier) to the session.
     * gemName should be capitalized like "Ruby", "Sapphire", etc.
     */
    public void addGemstone(String gemName, long amount) {
        if (!active) {
            start();
        }
        if (paused) resume(); // Must resume before updating lastUpdateTime so idle time is computed correctly
        gemstoneItems.merge(gemName, amount, Long::sum);
        lastUpdateTime = System.currentTimeMillis();
        recalculate();
    }

    private void start() {
        if (!active) {
            startTime = System.currentTimeMillis();
            lastUpdateTime = startTime;
            active = true;
            paused = false;
            pausedElapsed = 0;
            SkyblockProfitTracker.priceFetcher.fetchPricesAsync();
        }
    }

    private void pause() {
        paused = true;
        pausedElapsed = (System.currentTimeMillis() - startTime) / 1000;
        // Subtract the timeout period (like BigDiamond does)
        pausedElapsed = Math.max(0, pausedElapsed - SkyblockProfitTracker.config.sessionTimeoutSeconds);
        recalculate();
        sendChatSummary();
    }

    private void resume() {
        // Extend startTime so idle time doesn't count
        long idleTime = System.currentTimeMillis() - lastUpdateTime;
        startTime += idleTime;
        paused = false;
        pausedElapsed = 0;
    }

    /**
     * Recalculate all derived values.
     */
    private void recalculate() {
        // Calculate elapsed time
        if (paused) {
            elapsedSeconds = pausedElapsed;
        } else if (active && startTime > 0) {
            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        } else {
            elapsedSeconds = 0;
        }

        // Calculate ore profit
        long oreProfit = 0;
        for (Map.Entry<String, Long> entry : oreItems.entrySet()) {
            double price = SkyblockProfitTracker.priceFetcher.getOrePrice(entry.getKey());
            oreProfit += (long) (entry.getValue() * price);
        }

        // Calculate gemstone profit
        long gemProfit = 0;
        for (Map.Entry<String, Long> entry : gemstoneItems.entrySet()) {
            // Pristine procs give Flawed gems (tier 1)
            double pricePerFlawed = SkyblockProfitTracker.priceFetcher.getGemstonePrice(entry.getKey(), 1);
            gemProfit += (long) (entry.getValue() * pricePerFlawed);
        }

        totalProfit = oreProfit + gemProfit;

        if (elapsedSeconds > 0) {
            profitPerHour = (long) ((double) totalProfit / elapsedSeconds * 3600);
        } else {
            profitPerHour = 0;
        }
    }

    /**
     * Reset the session completely.
     */
    public void reset() {
        oreItems.clear();
        gemstoneItems.clear();
        startTime = 0;
        lastUpdateTime = 0;
        active = false;
        paused = false;
        pausedElapsed = 0;
        totalProfit = 0;
        profitPerHour = 0;
        elapsedSeconds = 0;
    }

    /**
     * Send a summary to chat (like BigDiamond does on timeout).
     */
    private void sendChatSummary() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        String prefix = "§6[ProfitTracker]§r ";
        client.player.sendMessage(Text.literal(prefix + "§7No new items in " +
                SkyblockProfitTracker.config.sessionTimeoutSeconds + "s. Timer paused."), false);
        client.player.sendMessage(Text.literal(prefix + "§fSession Stats:"), false);

        // Ores
        for (Map.Entry<String, Long> entry : oreItems.entrySet()) {
            if (entry.getValue() > 0) {
                double price = SkyblockProfitTracker.priceFetcher.getOrePrice(entry.getKey());
                long itemProfit = (long) (entry.getValue() * price);
                client.player.sendMessage(Text.literal(prefix + "§f" +
                        FormatUtil.capitalize(entry.getKey()) + ": §a" +
                        FormatUtil.formatWithCommas(entry.getValue()) +
                        " §7($" + FormatUtil.formatWithCommas(itemProfit) + ")"), false);
            }
        }

        // Gemstones
        for (Map.Entry<String, Long> entry : gemstoneItems.entrySet()) {
            if (entry.getValue() > 0) {
                double price = SkyblockProfitTracker.priceFetcher.getGemstonePrice(entry.getKey(), 1);
                long itemProfit = (long) (entry.getValue() * price);
                client.player.sendMessage(Text.literal(prefix + "§d" +
                        entry.getKey() + " (Flawed): §a" +
                        FormatUtil.formatWithCommas(entry.getValue()) +
                        " §7($" + FormatUtil.formatWithCommas(itemProfit) + ")"), false);
            }
        }

        client.player.sendMessage(Text.literal(prefix + "§fTotal Profit: §a$" +
                FormatUtil.formatWithCommas(totalProfit)), false);
        client.player.sendMessage(Text.literal(prefix + "§fTime: §a" +
                FormatUtil.formatTime(elapsedSeconds)), false);
        client.player.sendMessage(Text.literal(prefix + "§fProfit/hr: §a$" +
                FormatUtil.formatWithCommas(profitPerHour)), false);
        client.player.sendMessage(Text.literal(prefix + "§7Mine more to resume, or /pt reset to start fresh."), false);
    }

    // ----- Getters for HUD -----

    public boolean isActive() { return active; }
    public boolean isPaused() { return paused; }
    public long getTotalProfit() { return totalProfit; }
    public long getProfitPerHour() { return profitPerHour; }
    public long getElapsedSeconds() { return elapsedSeconds; }

    public long getTotalOreItems() {
        return oreItems.values().stream().mapToLong(Long::longValue).sum();
    }

    public long getTotalGemstones() {
        return gemstoneItems.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Returns a sorted list of (name, count, profit) for HUD breakdown.
     * Combines ores and gemstones, sorted by profit descending.
     */
    public List<ItemBreakdownEntry> getTopItems(int maxItems) {
        List<ItemBreakdownEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Long> e : oreItems.entrySet()) {
            if (e.getValue() <= 0) continue;
            double price = SkyblockProfitTracker.priceFetcher.getOrePrice(e.getKey());
            long profit = (long) (e.getValue() * price);
            entries.add(new ItemBreakdownEntry(FormatUtil.capitalize(e.getKey()), e.getValue(), profit, false));
        }

        for (Map.Entry<String, Long> e : gemstoneItems.entrySet()) {
            if (e.getValue() <= 0) continue;
            double price = SkyblockProfitTracker.priceFetcher.getGemstonePrice(e.getKey(), 1);
            long profit = (long) (e.getValue() * price);
            entries.add(new ItemBreakdownEntry(e.getKey() + " §d(Flawed)", e.getValue(), profit, true));
        }

        entries.sort((a, b) -> Long.compare(b.profit, a.profit));
        if (entries.size() > maxItems) {
            entries = entries.subList(0, maxItems);
        }
        return entries;
    }

    public record ItemBreakdownEntry(String name, long count, long profit, boolean isGemstone) {}
}
