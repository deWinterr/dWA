package com.profittracker.tracker;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.util.FormatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a single mining session's items, profits, and timing.
 *
 * Uses tick-based timing (20 ticks = 1 second) instead of System.currentTimeMillis()
 * for reliable elapsed time and idle timeout detection.
 */
public class ProfitSession {

    // Item name (lowercase) -> count of raw items collected
    private final ConcurrentHashMap<String, Long> oreItems = new ConcurrentHashMap<>();

    // Gemstone name -> count at flawed tier (pristine procs)
    private final ConcurrentHashMap<String, Long> gemstoneItems = new ConcurrentHashMap<>();

    private boolean active = false;
    private boolean paused = false;

    // Tick-based timing (20 ticks = 1 second)
    private long activeTicks = 0;   // Total ticks while actively mining (excludes paused/idle)
    private int idleTicks = 0;      // Ticks since last item was received

    // Cached computed values
    private long totalProfit = 0;
    private long profitPerHour = 0;
    private long elapsedSeconds = 0;

    /**
     * Called every client tick (20 times/sec).
     */
    public void tick() {
        if (!active || paused) return;

        activeTicks++;
        idleTicks++;

        int timeoutTicks = SkyblockProfitTracker.config.sessionTimeoutSeconds * 20;
        if (idleTicks > timeoutTicks) {
            // Subtract the idle timeout period from active time
            activeTicks = Math.max(0, activeTicks - timeoutTicks);
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
        if (!active) start();
        if (paused) resume();
        oreItems.merge(itemNameLower, amount, Long::sum);
        idleTicks = 0;
        recalculate();
    }

    /**
     * Add gemstone pristine procs (flawed tier) to the session.
     * gemName should be capitalized like "Ruby", "Sapphire", etc.
     */
    public void addGemstone(String gemName, long amount) {
        if (!active) start();
        if (paused) resume();
        gemstoneItems.merge(gemName, amount, Long::sum);
        idleTicks = 0;
        recalculate();
    }

    private void start() {
        if (!active) {
            active = true;
            paused = false;
            activeTicks = 0;
            idleTicks = 0;
            SkyblockProfitTracker.priceFetcher.fetchPricesAsync();
        }
    }

    private void pause() {
        paused = true;
        recalculate();
        sendChatSummary();
    }

    private void resume() {
        paused = false;
        idleTicks = 0;
    }

    /**
     * Recalculate all derived values.
     */
    private void recalculate() {
        elapsedSeconds = activeTicks / 20;

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
        active = false;
        paused = false;
        activeTicks = 0;
        idleTicks = 0;
        totalProfit = 0;
        profitPerHour = 0;
        elapsedSeconds = 0;
    }

    /**
     * Send a summary to chat on timeout.
     */
    private void sendChatSummary() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        String prefix = "\u00a76[ProfitTracker]\u00a7r ";
        client.player.sendMessage(Text.literal(prefix + "\u00a77No new items in " +
                SkyblockProfitTracker.config.sessionTimeoutSeconds + "s. Timer paused."), false);
        client.player.sendMessage(Text.literal(prefix + "\u00a7fSession Stats:"), false);

        // Ores
        for (Map.Entry<String, Long> entry : oreItems.entrySet()) {
            if (entry.getValue() > 0) {
                double price = SkyblockProfitTracker.priceFetcher.getOrePrice(entry.getKey());
                long itemProfit = (long) (entry.getValue() * price);
                client.player.sendMessage(Text.literal(prefix + "\u00a7f" +
                        FormatUtil.capitalize(entry.getKey()) + ": \u00a7a" +
                        FormatUtil.formatWithCommas(entry.getValue()) +
                        " \u00a77($" + FormatUtil.formatWithCommas(itemProfit) + ")"), false);
            }
        }

        // Gemstones
        for (Map.Entry<String, Long> entry : gemstoneItems.entrySet()) {
            if (entry.getValue() > 0) {
                double price = SkyblockProfitTracker.priceFetcher.getGemstonePrice(entry.getKey(), 1);
                long itemProfit = (long) (entry.getValue() * price);
                client.player.sendMessage(Text.literal(prefix + "\u00a7d" +
                        entry.getKey() + " (Flawed): \u00a7a" +
                        FormatUtil.formatWithCommas(entry.getValue()) +
                        " \u00a77($" + FormatUtil.formatWithCommas(itemProfit) + ")"), false);
            }
        }

        client.player.sendMessage(Text.literal(prefix + "\u00a7fTotal Profit: \u00a7a$" +
                FormatUtil.formatWithCommas(totalProfit)), false);
        client.player.sendMessage(Text.literal(prefix + "\u00a7fTime: \u00a7a" +
                FormatUtil.formatTime(elapsedSeconds)), false);
        client.player.sendMessage(Text.literal(prefix + "\u00a7fProfit/hr: \u00a7a$" +
                FormatUtil.formatWithCommas(profitPerHour)), false);
        client.player.sendMessage(Text.literal(prefix + "\u00a77Mine more to resume, or /pt reset to start fresh."), false);
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
            entries.add(new ItemBreakdownEntry(e.getKey() + " \u00a7d(Flawed)", e.getValue(), profit, true));
        }

        entries.sort((a, b) -> Long.compare(b.profit, a.profit));
        if (entries.size() > maxItems) {
            entries = entries.subList(0, maxItems);
        }
        return entries;
    }

    public record ItemBreakdownEntry(String name, long count, long profit, boolean isGemstone) {}
}
