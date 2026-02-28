package com.profittracker.tracker;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.price.ItemPrices;
import com.profittracker.util.FormatUtil;
import net.minecraft.text.*;

import java.util.List;
import java.util.regex.*;

/**
 * Parses incoming chat messages to extract mining gains.
 *
 * Handles two message types:
 * 1. Sack messages: "[Sacks] +X items. (Last Ys.)" with hover text containing individual items.
 *    (Mirrors BigDiamond's sackTracker)
 * 2. PRISTINE! messages: "PRISTINE! You found a Flawed X Gemstone x#!"
 *    (Mirrors BlingBling's chat listener)
 *
 * Includes deduplication to prevent double-counting when both the mixin and
 * Fabric API event fire for the same message.
 *
 * 1.21.10 compatible: HoverEvent is now an interface.
 * HoverEvent.ShowText implements HoverEvent and has a .text() accessor returning Text.
 */
public class ChatParser {

    // Simple deduplication
    private static final int DEDUP_SIZE = 16;
    private static final long[] recentHashes = new long[DEDUP_SIZE];
    private static int dedupeIndex = 0;

    // Regex for the sack message
    private static final Pattern SACK_PATTERN = Pattern.compile(
            "\\[Sacks] \\+([\\d,]+) items?\\.\\s*\\(Last (\\d+)s\\.\\)"
    );

    // Regex for individual items in sack hover text
    private static final Pattern SACK_ITEM_PATTERN = Pattern.compile(
            "\\+(\\d[\\d,]*) (.+?) \\("
    );

    // Regex for PRISTINE! messages
    private static final Pattern PRISTINE_PATTERN = Pattern.compile(
            "PRISTINE!.*?Flawed (\\w+) Gemstone.*?x(\\d+)"
    );

    /**
     * Process an incoming chat message.
     */
    public static boolean processMessage(Text message) {
        String plainText = message.getString();
        String stripped = FormatUtil.stripFormatting(plainText);

        // Dedup check
        long hash = stripped.hashCode() ^ (System.currentTimeMillis() / 500);
        for (long h : recentHashes) {
            if (h == hash) return false;
        }
        recentHashes[dedupeIndex % DEDUP_SIZE] = hash;
        dedupeIndex++;

        // Check for PRISTINE! message
        if (checkPristine(stripped)) return true;

        // Check for Sack message
        if (checkSackMessage(stripped, message)) return true;

        return false;
    }

    private static boolean checkPristine(String stripped) {
        Matcher m = PRISTINE_PATTERN.matcher(stripped);
        if (m.find()) {
            String gemName = m.group(1);
            int amount = Integer.parseInt(m.group(2));

            if (ItemPrices.isGemstone(gemName)) {
                SkyblockProfitTracker.session.addGemstone(gemName, amount);
                SkyblockProfitTracker.LOGGER.debug("PRISTINE: {} x{}", gemName, amount);
                return true;
            }
        }
        return false;
    }

    private static boolean checkSackMessage(String stripped, Text message) {
        Matcher sackMatch = SACK_PATTERN.matcher(stripped);
        if (!sackMatch.find()) return false;

        int lastSeconds = Integer.parseInt(sackMatch.group(2));

        // Extract hover text from message parts
        String hoverText = extractHoverText(message);
        if (hoverText == null || hoverText.isEmpty()) return false;

        String hoverStripped = FormatUtil.stripFormatting(hoverText);
        String[] lines = hoverStripped.split("\n");

        boolean foundSomething = false;

        for (String line : lines) {
            Matcher itemMatch = SACK_ITEM_PATTERN.matcher(line.trim());
            if (!itemMatch.find()) continue;

            long rawAmount = Long.parseLong(itemMatch.group(1).replace(",", ""));
            String fullItemName = itemMatch.group(2).trim();

            String baseName = fullItemName
                    .toLowerCase()
                    .replace("enchanted ", "")
                    .replace(" block", "");

            long adjustedAmount = rawAmount;

            if (fullItemName.contains("Enchanted")) {
                adjustedAmount *= 160;
                if (fullItemName.contains("Block")) {
                    adjustedAmount *= 160;
                }
            }

            if (ItemPrices.isTrackedOre(baseName)) {
                SkyblockProfitTracker.session.addOre(baseName, adjustedAmount);
                foundSomething = true;
                SkyblockProfitTracker.LOGGER.debug("Sack: {} x{} (raw: {} x{})",
                        baseName, adjustedAmount, fullItemName, rawAmount);
            }
        }

        return foundSomething;
    }

    /**
     * Recursively extract hover text from a Text object and its siblings.
     *
     * In 1.21.10, HoverEvent is an interface. HoverEvent.ShowText is a record
     * implementing it, with a .text() method that returns the Text content.
     * We use instanceof to safely handle this.
     */
    private static String extractHoverText(Text text) {
        // Check this text's style for hover event
        HoverEvent hover = text.getStyle().getHoverEvent();
        if (hover != null) {
            // In 1.21.5+, HoverEvent is an interface.
            // ShowText is a record: HoverEvent.ShowText(Text text)
            if (hover instanceof HoverEvent.ShowText showText) {
                Text hoverContent = showText.text();
                if (hoverContent != null) {
                    return hoverContent.getString();
                }
            }
        }

        // Check siblings
        List<Text> siblings = text.getSiblings();
        for (Text sibling : siblings) {
            String result = extractHoverText(sibling);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }

        return null;
    }
}
