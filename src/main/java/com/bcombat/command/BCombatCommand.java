package com.bcombat.command;

import com.bcombat.combat.ai.AICombatController;
import com.bcombat.combat.ai.AICombatManager;
import com.bcombat.combat.ai.AIDifficultyPreset;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.config.BCombatConfig;
import com.bcombat.debug.CombatDebugLogger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

/**
 * The {@code /bcombat ai} debug command tree — the operator-facing
 * control surface for {@link AICombatManager}. Every branch is a thin
 * wrapper around that manager's existing public API; this class
 * contains no combat logic of its own, exactly like {@code
 * AICombatController} contains none beyond calling {@link
 * CombatController}'s public API.
 * <p>
 * Subcommands:
 * <ul>
 *     <li>{@code /bcombat ai enable <targets> [difficulty]} — enables
 *     AI-driven combat on every targeted {@link MobEntity}, at the given
 *     {@link AIDifficultyPreset} (default {@code NORMAL} if omitted).
 *     Non-{@code MobEntity} targets (players, item frames, etc.) are
 *     silently skipped with a feedback count, never an error, since a
 *     broad selector like {@code @e} is expected to sweep up entities
 *     this command can't act on.</li>
 *     <li>{@code /bcombat ai disable <targets>} — disables AI-driven
 *     combat on every targeted entity that currently has it enabled.</li>
 *     <li>{@code /bcombat ai difficulty <targets> <difficulty>} —
 *     re-enables (i.e. replaces) the AI controller for every targeted,
 *     already-AI-enabled entity at a new difficulty, without otherwise
 *     disturbing its combat state.</li>
 *     <li>{@code /bcombat ai list} — prints every currently AI-enabled
 *     combatant with its difficulty, combat state, and tactical intent,
 *     for at-a-glance debugging.</li>
 * </ul>
 * Requires permission level 2 (operator) — same bar vanilla sets for
 * other gameplay-altering debug commands like {@code /gamerule}.
 */
public final class BCombatCommand {

    private static final SuggestionProvider<ServerCommandSource> DIFFICULTY_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(
                    Arrays.stream(AIDifficultyPreset.values()).map(preset -> preset.name().toLowerCase(Locale.ROOT)),
                    builder
            );

    private BCombatCommand() {
        // Static registrar, no instances.
    }

    /**
     * Registers the entire {@code /bcombat} command tree. Must be
     * called exactly once, from {@code BannerlordCombat#onInitialize()}.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerTree(dispatcher));
    }

    private static void registerTree(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bcombat")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("ai")
                        .then(CommandManager.literal("enable")
                                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                        .executes(ctx -> enable(ctx, AIDifficultyPreset.NORMAL))
                                        .then(CommandManager.argument("difficulty", StringArgumentType.word())
                                                .suggests(DIFFICULTY_SUGGESTIONS)
                                                .executes(ctx -> enable(ctx, parseDifficulty(ctx))))))
                        .then(CommandManager.literal("disable")
                                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                        .executes(BCombatCommand::disable)))
                        .then(CommandManager.literal("difficulty")
                                .then(CommandManager.argument("targets", EntityArgumentType.entities())
                                        .then(CommandManager.argument("difficulty", StringArgumentType.word())
                                                .suggests(DIFFICULTY_SUGGESTIONS)
                                                .executes(ctx -> setDifficulty(ctx, parseDifficulty(ctx))))))
                        .then(CommandManager.literal("list")
                                .executes(BCombatCommand::list)))
                .then(CommandManager.literal("debug")
                        .executes(BCombatCommand::debugStatus)
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(BCombatCommand::debugSet)))
                .then(CommandManager.literal("config")
                        .then(CommandManager.literal("reload")
                                .executes(BCombatCommand::configReload))
                        .then(CommandManager.literal("save")
                                .executes(BCombatCommand::configSave))));
    }

    // ------------------------------------------------------------------
    // debug
    // ------------------------------------------------------------------

    /** {@code /bcombat debug} — reports whether debug logging is currently enabled. */
    private static int debugStatus(CommandContext<ServerCommandSource> ctx) {
        boolean enabled = CombatDebugLogger.isEnabled();
        ctx.getSource().sendFeedback(
                () -> feedback("Debug logging is currently " + (enabled ? "ON" : "OFF")), false);
        return enabled ? 1 : 0;
    }

    /** {@code /bcombat debug <true|false>} — toggles debug event logging on/off. */
    private static int debugSet(CommandContext<ServerCommandSource> ctx) {
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        CombatDebugLogger.setEnabled(enabled);
        ctx.getSource().sendFeedback(
                () -> feedback("Debug logging " + (enabled ? "enabled" : "disabled")), true);
        return 1;
    }

    // ------------------------------------------------------------------
    // config
    // ------------------------------------------------------------------

    /** {@code /bcombat config reload} — re-reads config/bcombat.json and re-applies it immediately. */
    private static int configReload(CommandContext<ServerCommandSource> ctx) {
        BCombatConfig.reload();
        ctx.getSource().sendFeedback(
                () -> feedback("Combat configuration reloaded from " + BCombatConfig.getConfigPath()), true);
        return 1;
    }

    /** {@code /bcombat config save} — writes the currently active tuning values back out to config/bcombat.json. */
    private static int configSave(CommandContext<ServerCommandSource> ctx) {
        BCombatConfig.save();
        ctx.getSource().sendFeedback(
                () -> feedback("Combat configuration saved to " + BCombatConfig.getConfigPath()), true);
        return 1;
    }

    // ------------------------------------------------------------------
    // enable
    // ------------------------------------------------------------------

    private static int enable(CommandContext<ServerCommandSource> ctx, AIDifficultyPreset difficulty) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(ctx, "targets");
        int enabled = 0;
        for (Entity target : targets) {
            if (target instanceof MobEntity mob) {
                AICombatManager.enable(mob, difficulty);
                enabled++;
            }
        }

        int skipped = targets.size() - enabled;
        int finalEnabled = enabled;
        ctx.getSource().sendFeedback(
                () -> feedback("Enabled AI combat (" + difficulty.name().toLowerCase(Locale.ROOT) + ") on "
                        + finalEnabled + " entit" + (finalEnabled == 1 ? "y" : "ies")
                        + (skipped > 0 ? ", skipped " + skipped + " non-mob target" + (skipped == 1 ? "" : "s") : "")),
                true);
        return enabled;
    }

    // ------------------------------------------------------------------
    // disable
    // ------------------------------------------------------------------

    private static int disable(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(ctx, "targets");
        int disabled = 0;
        for (Entity target : targets) {
            if (target instanceof MobEntity mob && AICombatManager.isEnabled(mob)) {
                AICombatManager.disable(mob);
                disabled++;
            }
        }

        int finalDisabled = disabled;
        ctx.getSource().sendFeedback(
                () -> feedback("Disabled AI combat on " + finalDisabled + " entit" + (finalDisabled == 1 ? "y" : "ies")),
                true);
        return disabled;
    }

    // ------------------------------------------------------------------
    // difficulty
    // ------------------------------------------------------------------

    private static int setDifficulty(CommandContext<ServerCommandSource> ctx, AIDifficultyPreset difficulty) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(ctx, "targets");
        int updated = 0;
        for (Entity target : targets) {
            // Re-enabling replaces the tracked AICombatController wholesale
            // (see AICombatManager#enable) - this is intentionally the
            // only mutation path, exactly like every other AI knob is
            // immutable once an AIDifficultyPreset is picked (see that
            // enum's class docs on why it's a plain fixed set of
            // constructor-supplied values rather than something mutated
            // in place).
            if (target instanceof MobEntity mob && AICombatManager.isEnabled(mob)) {
                AICombatManager.enable(mob, difficulty);
                updated++;
            }
        }

        int finalUpdated = updated;
        ctx.getSource().sendFeedback(
                () -> feedback("Set AI difficulty to " + difficulty.name().toLowerCase(Locale.ROOT)
                        + " on " + finalUpdated + " entit" + (finalUpdated == 1 ? "y" : "ies")),
                true);
        return updated;
    }

    // ------------------------------------------------------------------
    // list
    // ------------------------------------------------------------------

    private static int list(CommandContext<ServerCommandSource> ctx) {
        Collection<AICombatController> controllers = AICombatManager.controllers();
        if (controllers.isEmpty()) {
            ctx.getSource().sendFeedback(() -> feedback("No AI-enabled combatants."), false);
            return 0;
        }

        ctx.getSource().sendFeedback(() -> feedback(controllers.size() + " AI-enabled combatant(s):"), false);
        for (AICombatController ai : controllers) {
            MobEntity mob = ai.getEntity();
            CombatController controller = CombatControllerManager.getIfPresent(mob);
            String state = controller != null ? controller.getCombatState().name() : "UNKNOWN";
            String stamina = controller != null
                    ? Math.round(controller.getStaminaRatio() * 100) + "%"
                    : "?";
            ctx.getSource().sendFeedback(() -> feedback(
                    " - " + mob.getType().getTranslationKey() + " @ " + formatPos(mob)
                            + " | difficulty=" + ai.getDifficulty().name().toLowerCase(Locale.ROOT)
                            + " | state=" + state
                            + " | stamina=" + stamina
                            + " | intent=" + ai.getTacticalIntent().name().toLowerCase(Locale.ROOT)
            ), false);
        }
        return controllers.size();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static AIDifficultyPreset parseDifficulty(CommandContext<ServerCommandSource> ctx) {
        String raw = StringArgumentType.getString(ctx, "difficulty");
        try {
            return AIDifficultyPreset.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendError(feedback("Unknown difficulty '" + raw + "', defaulting to NORMAL. Valid values: "
                    + Arrays.toString(AIDifficultyPreset.values())));
            return AIDifficultyPreset.NORMAL;
        }
    }

    private static String formatPos(Entity entity) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", entity.getX(), entity.getY(), entity.getZ());
    }

    private static Text feedback(String message) {
        return Text.literal("[bcombat] " + message);
    }
}