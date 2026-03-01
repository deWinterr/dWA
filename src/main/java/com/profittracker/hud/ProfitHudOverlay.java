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
 *
 * No title, no background — just clean text with full RGB color support
 * and configurable scale via hudScale (scroll in HUD editor).
 */
public class ProfitHudOverlay {

    private static final int LINE_HEIGHT = 11;

    /** A single colored text segment within a HUD line. */
    public record TextSegment(String text, int rgb) {}

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

        List<List<TextSegment>> lines = buildLines(session, config);
        if (lines.isEmpty()) return;

        float scale = config.hudScale;
        int x = config.hudX;
        int y = config.hudY;

        // Apply scale via matrix transformation (Matrix3x2fStack in 1.21.10)
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);

        int lineY = 0;
        for (List<TextSegment> line : lines) {
            int segX = 0;
            for (TextSegment seg : line) {
                context.drawTextWithShadow(textRenderer, seg.text, segX, lineY, 0xFF000000 | seg.rgb);
                segX += textRenderer.getWidth(seg.text);
            }
            lineY += LINE_HEIGHT;
        }

        context.getMatrices().popMatrix();
    }

    /** Build all HUD lines as lists of colored text segments. */
    public static List<List<TextSegment>> buildLines(ProfitSession session, ModConfig config) {
        List<List<TextSegment>> lines = new ArrayList<>();

        int lc = config.labelColor;
        int vc = config.valueColor;
        int tc = config.timeColor;
        int sc = config.separatorColor;

        // Paused indicator (replaces old title)
        if (session.isPaused()) {
            lines.add(List.of(new TextSegment("(PAUSED)", 0xFF5555)));
        }

        if (config.showProfit) {
            lines.add(List.of(
                    new TextSegment("Profit: ", lc),
                    new TextSegment("$" + FormatUtil.formatWithCommas(session.getTotalProfit()), vc)
            ));
        }

        if (config.showProfitPerHour) {
            lines.add(List.of(
                    new TextSegment("$/hr: ", lc),
                    new TextSegment("$" + FormatUtil.formatWithCommas(session.getProfitPerHour()), vc)
            ));
        }

        if (config.showTime) {
            lines.add(List.of(
                    new TextSegment("Time: ", lc),
                    new TextSegment(FormatUtil.formatTime(session.getElapsedSeconds()), tc)
            ));
        }

        if (config.showItems) {
            long totalItems = session.getTotalOreItems() + session.getTotalGemstones();
            lines.add(List.of(
                    new TextSegment("Items: ", lc),
                    new TextSegment(FormatUtil.formatWithCommas(totalItems), tc)
            ));
        }

        if (config.showItemBreakdown) {
            List<ProfitSession.ItemBreakdownEntry> topItems = session.getTopItems(config.maxBreakdownItems);
            if (!topItems.isEmpty()) {
                if (config.showSeparators) {
                    lines.add(List.of(new TextSegment("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500", sc)));
                }
                for (ProfitSession.ItemBreakdownEntry entry : topItems) {
                    int nameColor = entry.isGemstone() ? 0xFF55FF : 0xFFFFFF;
                    lines.add(List.of(
                            new TextSegment(entry.name(), nameColor),
                            new TextSegment(": ", 0xAAAAAA),
                            new TextSegment("$" + FormatUtil.formatNumber(entry.profit()), vc),
                            new TextSegment(" (x" + FormatUtil.formatWithCommas(entry.count()) + ")", 0x555555)
                    ));
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
                case "bazaar_sell" -> "[BZ Sell |" + tierName + "]";
                case "bazaar_buy" -> "[BZ Offer |" + tierName + "]";
                case "npc" -> "[NPC |" + tierName + "]";
                default -> "";
            };
            if (!mode.isEmpty()) {
                lines.add(List.of(new TextSegment(mode, sc)));
            }
        }

        return lines;
    }
}
