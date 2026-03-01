package com.profittracker.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;

import java.util.Collection;
import java.util.List;

/**
 * Detects whether the player is in a valid SkyBlock mining area by reading the scoreboard sidebar.
 * Mirrors the approach used by BlingBlingAddons' LocationChecker.
 *
 * 1.21.10 compatible: uses client.world.getScoreboard() instead of player.getScoreboard().
 */
public class AreaDetector {

    private static final List<String> MINING_AREAS = List.of(
            "Dwarven", "Royal", "Palace", "Library", "Mist", "Cliffside",
            "Quarry", "Gateway", "Wall", "Forge", "Far", "Burrows", "Springs",
            "Upper", "Glacite",
            "Goblin", "Jungle", "Mithril", "Precursor", "Magma", "Crystal",
            "Khazad", "Divan", "City",
            "End", "Dragon's",
            "shaft"
    );

    private static final String SKYBLOCK_IDENTIFIER = "SKYBLOCK";

    private long lastCheckTime = 0;
    private boolean inMiningArea = false;
    private boolean onSkyblock = false;

    public boolean isInMiningArea() {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime > 1000) {
            lastCheckTime = now;
            checkScoreboard();
        }
        return inMiningArea;
    }

    public boolean isOnSkyblock() {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime > 1000) {
            lastCheckTime = now;
            checkScoreboard();
        }
        return onSkyblock;
    }

    private void checkScoreboard() {
        inMiningArea = false;
        onSkyblock = false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) return;

        // In 1.21.9+, use world.getScoreboard() instead of player.getScoreboard()
        Scoreboard scoreboard = client.world.getScoreboard();
        if (scoreboard == null) return;

        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        // Check the title for SKYBLOCK
        String title = sidebar.getDisplayName().getString();
        if (title.toUpperCase().contains(SKYBLOCK_IDENTIFIER)) {
            onSkyblock = true;
        }

        // Read all scoreboard entries
        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(sidebar);
        for (ScoreboardEntry entry : entries) {
            String line = entry.name().getString();
            String stripped = FormatUtil.stripFormatting(line);

            for (String area : MINING_AREAS) {
                if (stripped.contains(area)) {
                    inMiningArea = true;
                    return;
                }
            }
        }
    }
}
