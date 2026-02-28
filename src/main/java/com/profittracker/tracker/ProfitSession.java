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
 * Uses System.currentTimeMillis() for accurate wall-clock elapsed time
 * instead of tick-based counting, avoiding inaccuracies from tick rate
 * fluctuations or server lag.
 */
public class ProfitSession {

    private final ConcurrentHashMap<String, Long> oreItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> gemstoneItems = new ConcurrentHashMap<>();

    private boolean active = false;
    private boolean paused = false;

    // Wall-clock timing using System.currentTimeMillis()
    private long startTimeMs = 0;           // When session started
    private long activeTimeMs = 0;          // Accumulated active time (excluding pauses/idle)
    private long lastResumeTimeMs = 0;      // When last resumed (or started)
    private long lastItemTimeMs = 0;        // When last item was received

    // Cached computed values
    private long totalProfit = 0;
    private long profitPerHour = 0;
    private long elapsedSeconds = 0;

    /**
     * Called periodically to check for idle timeout.
     * No longer counts ticks — just checks wall-clock time since last item.
     */
    public void checkTimeout() {
        if (!active || paused) return;

        long now = System.currentTimeMillis();
        long idleMs = now - lastItemTimeMs;
        int timeoutMs = SkyblockProfitTracker.config.sessionTimeoutSeconds * 1000;

        if (idleMs > timeoutMs) {
            // Accumulate time up to when idle started (subtract idle period)
            long activeUpToIdle = lastItemTimeMs - lastResumeTimeMs;
            if (activeUpToIdle > 0) {
                activeTimeMs += activeUpToIdle;
            }
            lastResumeTimeMs = now;
            pause();
        } else {
            recalculate();
        }
    }

    public void addOre(String itemNameLower, long amount) {
        if (!active) start();
        if (paused) resume();
        oreItems.merge(itemNameLower, amount, Long::sum);
        lastItemTimeMs = System.currentTimeMillis();
        recalculate();
    }

    public void addGemstone(String gemName, long amount) {
        if (!active) start();
        if (paused) resume();
        gemstoneItems.merge(gemName, amount, Long::sum);
        lastItemTimeMs = System.currentTimeMillis();
        recalculate();
    }

    private void start() {
        if (!active) {
            long now = System.currentTimeMillis();
            active = true;
            paused = false;
            startTimeMs = now;
            lastResumeTimeMs = now;
            lastItemTimeMs = now;
            activeTimeMs = 0;
            SkyblockProfitTracker.priceFetcher.fetchPricesAsync();
        }
    }

    private void pause() {
        paused = true;
        recalculate();
        sendChatSummary();
    }

    private void resume() {
        long now = System.currentTimeMillis();
        paused = false;
        lastResumeTimeMs = now;
        lastItemTimeMs = now;
    }

    private void recalculate() {
        // Calculate total active time
        long totalActiveMs = activeTimeMs;
        if (active && !paused) {
            totalActiveMs += System.currentTimeMillis() - lastResumeTimeMs;
        }
        elapsedSeconds = totalActiveMs / 1000;

        // Calculate ore profit
        long oreProfit = 0;
        for (Map.Entry<String, Long> entry : oreItems.entrySet()) {
            double price = SkyblockProfitTracker.priceFetcher.getOrePrice(entry.getKey());
            oreProfit += (long) (entry.getValue() * price);
        }

        // Calculate gemstone profit using configured rarity tier
        long gemProfit = 0;
        int gemTier = SkyblockProfitTracker.config.gemstoneRarity;
        for (Map.Entry<String, Long> entry : gemstoneItems.entrySet()) {
            double pricePerGem = SkyblockProfitTracker.priceFetcher.getGemstonePrice(entry.getKey(), gemTier);
            // If tracking at a higher rarity, convert: PRISTINE! gives flawed (tier 1).
            // Flawed -> Fine = 80x, Flawed -> Flawless = 80x80x
            // But PRISTINE! count is in flawed units. To convert to higher tier count:
            // fine count = flawed count / 80, flawless count = flawed count / 6400
            // price stays per-unit at the selected tier
            long count = entry.getValue();
            if (gemTier == 2) {
                // Fine: 80 flawed = 1 fine
                gemProfit += (long) ((count / 80.0) * pricePerGem);
            } else if (gemTier == 3) {
                // Flawless: 6400 flawed = 1 flawless
                gemProfit += (long) ((count / 6400.0) * pricePerGem);
            } else {
                // Flawed (tier 1) - default
                gemProfit += (long) (count * pricePerGem);
            }
        }

        totalProfit = oreProfit + gemProfit;

        if (elapsedSeconds > 0) {
            profitPerHour = (long) ((double) totalProfit / elapsedSeconds * 3600);
        } else {
            profitPerHour = 0;
        }
    }

    public void reset() {
        oreItems.clear();
        gemstoneItems.clear();
        active = false;
        paused = false;
        startTimeMs = 0;
        activeTimeMs = 0;
        lastResumeTimeMs = 0;
        lastItemTimeMs = 0;
        totalProfit = 0;
        profitPerHour = 0;
        elapsedSeconds = 0;
    }

    private void sendChatSummary() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        String prefix = "\u00a76[ProfitTracker]\u00a7r ";
        client.player.sendMessage(Text.literal(prefix + "\u00a77No new items in " +
                SkyblockProfitTracker.config.sessionTimeoutSeconds + "s. Timer paused."), false);
        client.player.sendMessage(Text.literal(prefix + "\u00a7fSession Stats:"), false);

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

        int gemTier = SkyblockProfitTracker.config.gemstoneRarity;
        String tierName = gemTier == 3 ? "Flawless" : gemTier == 2 ? "Fine" : "Flawed";
        for (Map.Entry<String, Long> entry : gemstoneItems.entrySet()) {
            if (entry.getValue() > 0) {
                double price = SkyblockProfitTracker.priceFetcher.getGemstonePrice(entry.getKey(), gemTier);
                long count = entry.getValue();
                long itemProfit;
                if (gemTier == 2) {
                    itemProfit = (long) ((count / 80.0) * price);
                } else if (gemTier == 3) {
                    itemProfit = (long) ((count / 6400.0) * price);
                } else {
                    itemProfit = (long) (count * price);
                }
                client.player.sendMessage(Text.literal(prefix + "\u00a7d" +
                        entry.getKey() + " (" + tierName + "): \u00a7a" +
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

    public List<ItemBreakdownEntry> getTopItems(int maxItems) {
        List<ItemBreakdownEntry> entries = new ArrayList<>();
        int gemTier = SkyblockProfitTracker.config.gemstoneRarity;
        String tierName = gemTier == 3 ? "Flawless" : gemTier == 2 ? "Fine" : "Flawed";

        for (Map.Entry<String, Long> e : oreItems.entrySet()) {
            if (e.getValue() <= 0) continue;
            double price = SkyblockProfitTracker.priceFetcher.getOrePrice(e.getKey());
            long profit = (long) (e.getValue() * price);
            entries.add(new ItemBreakdownEntry(FormatUtil.capitalize(e.getKey()), e.getValue(), profit, false));
        }

        for (Map.Entry<String, Long> e : gemstoneItems.entrySet()) {
            if (e.getValue() <= 0) continue;
            double price = SkyblockProfitTracker.priceFetcher.getGemstonePrice(e.getKey(), gemTier);
            long count = e.getValue();
            long profit;
            if (gemTier == 2) {
                profit = (long) ((count / 80.0) * price);
            } else if (gemTier == 3) {
                profit = (long) ((count / 6400.0) * price);
            } else {
                profit = (long) (count * price);
            }
            entries.add(new ItemBreakdownEntry(e.getKey() + " \u00a7d(" + tierName + ")", count, profit, true));
        }

        entries.sort((a, b) -> Long.compare(b.profit, a.profit));
        if (entries.size() > maxItems) {
            entries = entries.subList(0, maxItems);
        }
        return entries;
    }

    public record ItemBreakdownEntry(String name, long count, long profit, boolean isGemstone) {}
}
