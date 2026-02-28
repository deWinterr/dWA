package com.profittracker.hud;

import com.profittracker.SkyblockProfitTracker;
import com.profittracker.config.ModConfig;
import com.profittracker.tracker.ProfitSession;
import com.profittracker.util.FormatUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen that lets the player drag the HUD overlay to reposition it.
 * Opened via /pt hud. Press ESC to save and close.
 */
public class HudPositionScreen extends Screen {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 4;

    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    private int previewWidth, previewHeight;

    public HudPositionScreen() {
        super(Text.literal("HUD Position"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent overlay so the game world is visible
        context.fill(0, 0, this.width, this.height, 0x50000000);

        ModConfig config = SkyblockProfitTracker.config;
        int x = config.hudX;
        int y = config.hudY;

        List<String> lines = buildPreviewLines();

        // Calculate dimensions
        int maxWidth = 0;
        for (String line : lines) {
            int w = this.textRenderer.getWidth(line);
            if (w > maxWidth) maxWidth = w;
        }
        int totalHeight = lines.size() * LINE_HEIGHT;
        previewWidth = maxWidth + PADDING * 2;
        previewHeight = totalHeight + PADDING * 2;

        // Check if mouse is hovering over HUD area
        boolean hovered = mouseX >= x - PADDING && mouseX <= x + maxWidth + PADDING
                && mouseY >= y - PADDING && mouseY <= y + totalHeight + PADDING;

        // Draw HUD background (brighter when hovered/dragging)
        int bgColor = (hovered || dragging) ? 0xC0000000 : 0x80000000;
        context.fill(x - PADDING, y - PADDING,
                x + maxWidth + PADDING, y + totalHeight + PADDING,
                bgColor);

        // Draw orange border when hovered or dragging
        if (hovered || dragging) {
            int bc = 0xFFFFAA00;
            // Top
            context.fill(x - PADDING, y - PADDING, x + maxWidth + PADDING, y - PADDING + 1, bc);
            // Bottom
            context.fill(x - PADDING, y + totalHeight + PADDING - 1, x + maxWidth + PADDING, y + totalHeight + PADDING, bc);
            // Left
            context.fill(x - PADDING, y - PADDING, x - PADDING + 1, y + totalHeight + PADDING, bc);
            // Right
            context.fill(x + maxWidth + PADDING - 1, y - PADDING, x + maxWidth + PADDING, y + totalHeight + PADDING, bc);
        }

        // Draw text lines
        int lineY = y;
        for (String line : lines) {
            context.drawTextWithShadow(this.textRenderer, line, x, lineY, 0xFFFFFFFF);
            lineY += LINE_HEIGHT;
        }

        // Instructions at bottom center
        String hint = dragging ? "\u00a7eRelease to place" : "\u00a7eClick and drag the HUD to move it. Press ESC to save.";
        int hintWidth = this.textRenderer.getWidth(hint);
        context.drawTextWithShadow(this.textRenderer, hint,
                (this.width - hintWidth) / 2, this.height - 20, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ModConfig config = SkyblockProfitTracker.config;
            int x = config.hudX;
            int y = config.hudY;

            if (mouseX >= x - PADDING && mouseX <= x - PADDING + previewWidth
                    && mouseY >= y - PADDING && mouseY <= y - PADDING + previewHeight) {
                dragging = true;
                dragOffsetX = (int) mouseX - x;
                dragOffsetY = (int) mouseY - y;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            ModConfig config = SkyblockProfitTracker.config;
            // Clamp position to keep HUD within screen bounds
            config.hudX = Math.max(PADDING, Math.min(this.width - previewWidth + PADDING, (int) mouseX - dragOffsetX));
            config.hudY = Math.max(PADDING, Math.min(this.height - previewHeight + PADDING, (int) mouseY - dragOffsetY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        SkyblockProfitTracker.config.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private List<String> buildPreviewLines() {
        ProfitSession session = SkyblockProfitTracker.session;
        ModConfig config = SkyblockProfitTracker.config;
        List<String> lines = new ArrayList<>();

        if (session != null && session.isActive()) {
            // Show real session data
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
        } else {
            // Show sample preview data so the user can see and position the HUD
            lines.add("\u00a76\u00a7l\u26cf Profit Tracker");
            lines.add("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
            lines.add("\u00a77Profit: \u00a7a$1,234,567");
            lines.add("\u00a77$/hr: \u00a7a$4,567,890");
            lines.add("\u00a77Time: \u00a7f1h 23m 45s");
            lines.add("\u00a77Items: \u00a7f12,345");
            lines.add("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
            lines.add("\u00a7fDiamond\u00a77: \u00a7a$890.0K \u00a78(x5,000)");
            lines.add("\u00a7dRuby \u00a7d(Flawed)\u00a77: \u00a7a$344.6K \u00a78(x1,234)");
            String mode = switch (config.pricingMode) {
                case "bazaar_sell" -> "\u00a78[BZ Instant Sell]";
                case "bazaar_buy" -> "\u00a78[BZ Sell Offer]";
                case "npc" -> "\u00a78[NPC Prices]";
                default -> "";
            };
            lines.add(mode);
        }

        return lines;
    }
}
