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
 * Combines BigDiamond's profitDisplay (profit, $/hr, items, time)
 * with BlingBling's coin tracker (uptime, $/hr display).
 *
 * 1.21.10 compatible: no MatrixStack push/pop/scale (removed in 1.21.9+).
 * Instead we pre-compute scaled coordinates and draw directly.
 */
public class ProfitHudOverlay {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 4;
    private static final int BG_COLOR = 0x80000000;

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

        // Skip during F3 debug screen
        if (client.getDebugHud().shouldShowDebugHud()) return;

        TextRenderer textRenderer = client.textRenderer;

        // Build all HUD lines
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

        // Draw background
        context.fill(x - PADDING, y - PADDING,
                x + maxWidth + PADDING, y + totalHeight + PADDING,
                BG_COLOR);

        // Draw text lines
        int lineY = y;
        for (String line : lines) {
            context.drawTextWithShadow(textRenderer, line, x, lineY, 0xFFFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }

    private List<String> buildLines(ProfitSession session, ModConfig config) {
        List<String> lines = new ArrayList<>();

        String pauseTag = session.isPaused() ? " \u00a7c(PAUSED)" : "";
        lines.add("\u00a76\u00a7l\u26cf Profit Tracker" + pauseTag);
        lines.add("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        lines.add("\u00a77Profit: \u00a7a$" + FormatUtil.formatWithCommas(session.getTotalProfit()));
        lines.add("\u00a77$/hr: \u00a7a$" + FormatUtil.formatWithCommas(session.getProfitPerHour()));
        lines.add("\u00a77Time: \u00a7f" + FormatUtil.formatTime(session.getElapsedSeconds()));

        long totalItems = session.getTotalOreItems() + session.getTotalGemstones();
        lines.add("\u00a77Items: \u00a7f" + FormatUtil.formatWithCommas(totalItems));

        if (config.showItemBreakdown) {
            List<ProfitSession.ItemBreakdownEntry> topItems = session.getTopItems(config.maxBreakdownItems);
            if (!topItems.isEmpty()) {
                lines.add("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
                for (ProfitSession.ItemBreakdownEntry entry : topItems) {
                    String color = entry.isGemstone() ? "\u00a7d" : "\u00a7f";
                    lines.add(color + entry.name() + "\u00a77: \u00a7a$" +
                            FormatUtil.formatNumber(entry.profit()) +
                            " \u00a78(x" + FormatUtil.formatWithCommas(entry.count()) + ")");
                }
            }
        }

        String mode = switch (config.pricingMode) {
            case "bazaar_sell" -> "\u00a78[BZ Instant Sell]";
            case "bazaar_buy" -> "\u00a78[BZ Sell Offer]";
            case "npc" -> "\u00a78[NPC Prices]";
            default -> "";
        };
        lines.add(mode);

        return lines;
    }
}
