package com.profittracker.hud;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.config.ModConfig;
import com.profittracker.tracker.ProfitSession;
import com.profittracker.util.FormatUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the mining profit HUD overlay on screen.
 * Now supports configurable text colors, display toggles,
 * and gemstone rarity display.
 */
public class ProfitHudOverlay {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 6;

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext context, RenderTickCounter tickCounter) {
        ModConfig config = SkyblockProfitTracker.config;
        if (!config.hudEnabled) return;

        ProfitSession session = SkyblockProfitTracker.session;
        if (!session.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (client.getDebugHud().shouldShowDebugHud()) return;

        TextRenderer textRenderer = client.textRenderer;

        List<String> lines = buildLines(session, config);
        if (lines.isEmpty()) return;

        int x = config.hudX;
        int y = config.hudY;

        // Calculate max width for background
        int maxWidth = 0;
        for (String line : lines) {
            int w = textRenderer.getWidth(line);
            if (w > maxWidth) maxWidth = w;
        }

        int totalHeight = lines.size() * LINE_HEIGHT;

        // Draw background with configured color
        context.fill(x - PADDING, y - PADDING,
                x + maxWidth + PADDING, y + totalHeight + PADDING,
                config.bgColor);

        // Accent line at top (salmon pink)
        context.fill(x - PADDING, y - PADDING,
                x + maxWidth + PADDING, y - PADDING + 2,
                0xFFFA8072);

        // Draw text lines
        int lineY = y;
        for (String line : lines) {
            context.drawTextWithShadow(textRenderer, line, x, lineY, 0xFFFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }

    private List<String> buildLines(ProfitSession session, ModConfig config) {
        List<String> lines = new ArrayList<>();

        // Color code shortcuts
        String tc = "\u00a7" + config.titleColor;    // title
        String lc = "\u00a7" + config.labelColor;    // label
        String vc = "\u00a7" + config.valueColor;    // value
        String tmC = "\u00a7" + config.timeColor;    // time
        String sc = "\u00a7" + config.separatorColor; // separator

        // Title
        String pauseTag = session.isPaused() ? " \u00a7c(PAUSED)" : "";
        lines.add(tc + "\u00a7l\u26cf Profit Tracker" + pauseTag);

        if (config.showSeparators) {
            lines.add(sc + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        }

        if (config.showProfit) {
            lines.add(lc + "Profit: " + vc + "$" + FormatUtil.formatWithCommas(session.getTotalProfit()));
        }

        if (config.showProfitPerHour) {
            lines.add(lc + "$/hr: " + vc + "$" + FormatUtil.formatWithCommas(session.getProfitPerHour()));
        }

        if (config.showTime) {
            lines.add(lc + "Time: " + tmC + FormatUtil.formatTime(session.getElapsedSeconds()));
        }

        if (config.showItems) {
            long totalItems = session.getTotalOreItems() + session.getTotalGemstones();
            lines.add(lc + "Items: " + tmC + FormatUtil.formatWithCommas(totalItems));
        }

        if (config.showItemBreakdown) {
            List<ProfitSession.ItemBreakdownEntry> topItems = session.getTopItems(config.maxBreakdownItems);
            if (!topItems.isEmpty()) {
                if (config.showSeparators) {
                    lines.add(sc + "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
                }
                for (ProfitSession.ItemBreakdownEntry entry : topItems) {
                    String color = entry.isGemstone() ? "\u00a7d" : "\u00a7f";
                    lines.add(color + entry.name() + "\u00a77: " + vc + "$" +
                            FormatUtil.formatNumber(entry.profit()) +
                            " \u00a78(x" + FormatUtil.formatWithCommas(entry.count()) + ")");
                }
            }
        }

        if (config.showPricingMode) {
            String tierName = switch (config.gemstoneRarity) {
                case 2 -> " Fine";
                case 3 -> " Flawless";
                default -> " Flawed";
            };
            String mode = switch (config.pricingMode) {
                case "bazaar_sell" -> sc + "[BZ Sell |" + tierName + "]";
                case "bazaar_buy" -> sc + "[BZ Offer |" + tierName + "]";
                case "npc" -> sc + "[NPC |" + tierName + "]";
                default -> "";
            };
            lines.add(mode);
        }

        return lines;
    }
}
