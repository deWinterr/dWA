package com.profittracker.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.profittracker.SkyblockProfitTracker;
import com.profittracker.config.ModConfig;
import com.profittracker.hud.HudPositionScreen;
import com.profittracker.util.FormatUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Registers all client-side commands.
 *
 * Commands:
 *   /pt or /profittracker    - Show help / toggle HUD
 *   /pt reset                - Reset session
 *   /pt hud                  - Toggle HUD visibility
 *   /pt move <x> <y>         - Move HUD position
 *   /pt scale <value>        - Set HUD scale (0.5 - 3.0)
 *   /pt pricing <mode>       - Set pricing mode (npc, bazaar_sell, bazaar_buy)
 *   /pt timeout <seconds>    - Set session timeout
 *   /pt breakdown            - Toggle item breakdown on HUD
 *   /pt stats                - Show current session stats in chat
 *   /pt prices               - Force refresh Bazaar prices
 *   /pt webhook <url>        - Set Discord webhook URL
 */
public class TrackerCommand {

    private static final String PREFIX = "\u00a76[ProfitTracker]\u00a7r ";

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Register both /pt and /profittracker
            for (String cmdName : new String[]{"pt", "profittracker"}) {
                dispatcher.register(ClientCommandManager.literal(cmdName)
                        .executes(ctx -> {
                            showHelp(ctx.getSource()::sendFeedback);
                            return 1;
                        })

                        .then(literal("reset").executes(ctx -> {
                            SkyblockProfitTracker.session.reset();
                            msg(ctx.getSource()::sendFeedback, "\u00a7aSession reset! Start mining to begin a new session.");
                            return 1;
                        }))

                        .then(literal("hud")
                                .executes(ctx -> {
                                    // Enable HUD and open the drag-to-position screen
                                    SkyblockProfitTracker.config.hudEnabled = true;
                                    SkyblockProfitTracker.config.save();
                                    MinecraftClient.getInstance().execute(() ->
                                            MinecraftClient.getInstance().setScreen(new HudPositionScreen()));
                                    return 1;
                                })
                                .then(literal("toggle").executes(ctx -> {
                                    ModConfig config = SkyblockProfitTracker.config;
                                    config.hudEnabled = !config.hudEnabled;
                                    config.save();
                                    msg(ctx.getSource()::sendFeedback,
                                            "HUD " + (config.hudEnabled ? "\u00a7aenabled" : "\u00a7cdisabled") + "\u00a7r.");
                                    return 1;
                                })))

                        .then(literal("move")
                                .then(argument("x", IntegerArgumentType.integer(0))
                                        .then(argument("y", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    SkyblockProfitTracker.config.hudX = x;
                                                    SkyblockProfitTracker.config.hudY = y;
                                                    SkyblockProfitTracker.config.save();
                                                    msg(ctx.getSource()::sendFeedback,
                                                            "HUD moved to \u00a7e" + x + ", " + y);
                                                    return 1;
                                                }))))

                        .then(literal("scale")
                                .then(argument("value", StringArgumentType.word())
                                        .executes(ctx -> {
                                            try {
                                                float scale = Float.parseFloat(StringArgumentType.getString(ctx, "value"));
                                                scale = Math.max(0.5f, Math.min(3.0f, scale));
                                                SkyblockProfitTracker.config.hudScale = scale;
                                                SkyblockProfitTracker.config.save();
                                                msg(ctx.getSource()::sendFeedback,
                                                        "HUD scale set to \u00a7e" + scale);
                                            } catch (NumberFormatException e) {
                                                msg(ctx.getSource()::sendFeedback,
                                                        "\u00a7cInvalid scale. Use a number between 0.5 and 3.0");
                                            }
                                            return 1;
                                        })))

                        .then(literal("pricing")
                                .then(argument("mode", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("npc");
                                            builder.suggest("bazaar_sell");
                                            builder.suggest("bazaar_buy");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String mode = StringArgumentType.getString(ctx, "mode");
                                            if (!mode.equals("npc") && !mode.equals("bazaar_sell") && !mode.equals("bazaar_buy")) {
                                                msg(ctx.getSource()::sendFeedback,
                                                        "\u00a7cInvalid mode. Use: npc, bazaar_sell, or bazaar_buy");
                                                return 0;
                                            }
                                            SkyblockProfitTracker.config.pricingMode = mode;
                                            SkyblockProfitTracker.config.save();
                                            if (!mode.equals("npc")) {
                                                SkyblockProfitTracker.priceFetcher.fetchPricesAsync();
                                            }
                                            String modeDisplay = switch (mode) {
                                                case "npc" -> "NPC Prices";
                                                case "bazaar_sell" -> "Bazaar Instant Sell";
                                                case "bazaar_buy" -> "Bazaar Sell Offer";
                                                default -> mode;
                                            };
                                            msg(ctx.getSource()::sendFeedback,
                                                    "Pricing mode set to \u00a7e" + modeDisplay);
                                            return 1;
                                        })))

                        .then(literal("timeout")
                                .then(argument("seconds", IntegerArgumentType.integer(10, 600))
                                        .executes(ctx -> {
                                            int secs = IntegerArgumentType.getInteger(ctx, "seconds");
                                            SkyblockProfitTracker.config.sessionTimeoutSeconds = secs;
                                            SkyblockProfitTracker.config.save();
                                            msg(ctx.getSource()::sendFeedback,
                                                    "Session timeout set to \u00a7e" + secs + "s");
                                            return 1;
                                        })))

                        .then(literal("breakdown").executes(ctx -> {
                            ModConfig config = SkyblockProfitTracker.config;
                            config.showItemBreakdown = !config.showItemBreakdown;
                            config.save();
                            msg(ctx.getSource()::sendFeedback,
                                    "Item breakdown " + (config.showItemBreakdown ? "\u00a7aenabled" : "\u00a7cdisabled"));
                            return 1;
                        }))

                        .then(literal("stats").executes(ctx -> {
                            var session = SkyblockProfitTracker.session;
                            if (!session.isActive()) {
                                msg(ctx.getSource()::sendFeedback, "\u00a77No active session. Start mining!");
                                return 1;
                            }
                            msg(ctx.getSource()::sendFeedback, "\u00a7f--- Session Stats ---");
                            msg(ctx.getSource()::sendFeedback,
                                    "\u00a77Profit: \u00a7a$" + FormatUtil.formatWithCommas(session.getTotalProfit()));
                            msg(ctx.getSource()::sendFeedback,
                                    "\u00a77$/hr: \u00a7a$" + FormatUtil.formatWithCommas(session.getProfitPerHour()));
                            msg(ctx.getSource()::sendFeedback,
                                    "\u00a77Time: \u00a7f" + FormatUtil.formatTime(session.getElapsedSeconds()));
                            msg(ctx.getSource()::sendFeedback,
                                    "\u00a77Ores: \u00a7f" + FormatUtil.formatWithCommas(session.getTotalOreItems()));
                            msg(ctx.getSource()::sendFeedback,
                                    "\u00a77Gems: \u00a7d" + FormatUtil.formatWithCommas(session.getTotalGemstones()));
                            return 1;
                        }))

                        .then(literal("prices").executes(ctx -> {
                            SkyblockProfitTracker.priceFetcher.fetchPricesAsync();
                            msg(ctx.getSource()::sendFeedback, "\u00a7eRefreshing Bazaar prices...");
                            return 1;
                        }))

                        .then(literal("webhook")
                                .then(argument("url", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String url = StringArgumentType.getString(ctx, "url");
                                            SkyblockProfitTracker.config.discordWebhook = url;
                                            SkyblockProfitTracker.config.save();
                                            String display = url.contains("https") ? "\u00a7a" + url : "\u00a7cInvalid URL";
                                            msg(ctx.getSource()::sendFeedback, "Webhook set to: " + display);
                                            return 1;
                                        })))
                );
            }
        });
    }

    private static void showHelp(java.util.function.Consumer<Text> send) {
        send.accept(Text.literal("\u00a76\u00a7l=== Skyblock Profit Tracker ==="));
        send.accept(Text.literal("\u00a7e/pt reset \u00a77- Reset session"));
        send.accept(Text.literal("\u00a7e/pt hud \u00a77- Open HUD position editor (drag to move)"));
        send.accept(Text.literal("\u00a7e/pt hud toggle \u00a77- Toggle HUD on/off"));
        send.accept(Text.literal("\u00a7e/pt scale <0.5-3.0> \u00a77- HUD scale"));
        send.accept(Text.literal("\u00a7e/pt pricing <mode> \u00a77- npc/bazaar_sell/bazaar_buy"));
        send.accept(Text.literal("\u00a7e/pt timeout <seconds> \u00a77- Idle timeout (10-600)"));
        send.accept(Text.literal("\u00a7e/pt breakdown \u00a77- Toggle item breakdown"));
        send.accept(Text.literal("\u00a7e/pt stats \u00a77- Show session stats in chat"));
        send.accept(Text.literal("\u00a7e/pt prices \u00a77- Force refresh Bazaar prices"));
        send.accept(Text.literal("\u00a7e/pt webhook <url> \u00a77- Set Discord webhook"));
        send.accept(Text.literal("\u00a78Tracks ores via Sack messages + gems via PRISTINE! procs."));
    }

    private static void msg(java.util.function.Consumer<Text> send, String text) {
        send.accept(Text.literal(PREFIX + text));
    }
}
