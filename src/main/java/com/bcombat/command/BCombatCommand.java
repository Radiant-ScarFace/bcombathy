package com.bcombat.command;

import com.bcombat.combat.ai.AICombatController;
import com.bcombat.combat.ai.AICombatManager;
import com.bcombat.combat.ai.AIDifficultyPreset;
import com.bcombat.combat.ai.CombatRole;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
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

    /** Tab-completion for the optional {@code role} argument on {@code /bcombat ai enable}, mirroring {@link #DIFFICULTY_SUGGESTIONS}. */
    private static final SuggestionProvider<ServerCommandSource> ROLE_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(
                    Arrays.stream(CombatRole.values()).map(role -> role.name().toLowerCase(Locale.ROOT)),
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
                                        .executes(ctx -> enable(ctx, AIDifficultyPreset.NORMAL, null, null))
                                        .then(CommandManager.argument("difficulty", StringArgumentType.word())
                                                .suggests(DIFFICULTY_SUGGESTIONS)
                                                .executes(ctx -> enable(ctx, parseDifficulty(ctx), null, null))
                                                .then(CommandManager.argument("role", StringArgumentType.word())
                                                        .suggests(ROLE_SUGGESTIONS)
                                                        .executes(ctx -> enable(ctx, parseDifficulty(ctx), parseRole(ctx), null))
                                                        .then(CommandManager.argument("squad", StringArgumentType.word())
                                                                .executes(ctx -> enable(ctx, parseDifficulty(ctx), parseRole(ctx),
                                                                        StringArgumentType.getString(ctx, "squad"))))))))
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
                .then(CommandManager.literal("combat")
                        .executes(ctx -> combatStatus(ctx, null))
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ctx -> combatStatus(ctx, EntityArgumentType.getEntity(ctx, "target")))))
                .then(CommandManager.literal("stamina")
                        .executes(ctx -> staminaStatus(ctx, null))
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ctx -> staminaStatus(ctx, EntityArgumentType.getEntity(ctx, "target")))))
                .then(CommandManager.literal("direction")
                        .executes(ctx -> directionStatus(ctx, null))
                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                .executes(ctx -> directionStatus(ctx, EntityArgumentType.getEntity(ctx, "target")))))
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
    // combat / stamina / direction
    // ------------------------------------------------------------------

    /**
     * {@code /bcombat combat [target]} — reports the {@link CombatController}
     * combat-state snapshot (state, movement mode, mounted status,
     * equipped weapon) for the executing player or an explicit target,
     * reading the exact same public API {@code CombatInputHandler} and
     * {@code AICombatController} already drive.
     */
    private static int combatStatus(CommandContext<ServerCommandSource> ctx, Entity explicitTarget) throws CommandSyntaxException {
        LivingEntity subject = resolveSubject(ctx, explicitTarget);
        if (subject == null) {
            ctx.getSource().sendError(feedback("No player to inspect - run as a player or supply a target."));
            return 0;
        }
        CombatController controller = CombatControllerManager.getIfPresent(subject);
        if (controller == null) {
            ctx.getSource().sendFeedback(() -> feedback(subject.getName().getString() + " has no active CombatController."), false);
            return 0;
        }

        String weapon = controller.getEquippedWeaponItem() != null
                ? controller.getEquippedWeaponItem().getTranslationKey()
                : "none";
        ctx.getSource().sendFeedback(() -> feedback(subject.getName().getString()
                + " | state=" + controller.getCombatState().name()
                + " | movement=" + controller.getMovementMode().name()
                + " | mounted=" + controller.isMounted()
                + " | weapon=" + weapon
                + " | authoritative=" + controller.isAuthoritative()), false);
        return 1;
    }

    /**
     * {@code /bcombat stamina [target]} — reports the {@link
     * CombatController} stamina snapshot (current/max/ratio/exhaustion)
     * for the executing player or an explicit target, reading the exact
     * same read-only accessors {@code AICombatController} already uses
     * to make retreat decisions.
     */
    private static int staminaStatus(CommandContext<ServerCommandSource> ctx, Entity explicitTarget) throws CommandSyntaxException {
        LivingEntity subject = resolveSubject(ctx, explicitTarget);
        if (subject == null) {
            ctx.getSource().sendError(feedback("No player to inspect - run as a player or supply a target."));
            return 0;
        }
        CombatController controller = CombatControllerManager.getIfPresent(subject);
        if (controller == null) {
            ctx.getSource().sendFeedback(() -> feedback(subject.getName().getString() + " has no active CombatController."), false);
            return 0;
        }

        ctx.getSource().sendFeedback(() -> feedback(subject.getName().getString()
                + " | stamina=" + String.format(Locale.ROOT, "%.1f", controller.getCurrentStamina())
                + "/" + String.format(Locale.ROOT, "%.1f", controller.getMaxStamina())
                + " (" + Math.round(controller.getStaminaRatio() * 100) + "%)"
                + " | exhausted=" + controller.isExhausted()), false);
        return 1;
    }

    /**
     * {@code /bcombat direction [target]} — reports the {@link
     * CombatController} committed attack direction and locked guard
     * direction for the executing player or an explicit target, exactly
     * as {@code AttackDirectionTracker}/{@code GuardDirectionTracker}
     * resolve and {@code CombatController#updateAttackDirection}/{@code
     * #updateGuardDirection} store them.
     */
    private static int directionStatus(CommandContext<ServerCommandSource> ctx, Entity explicitTarget) throws CommandSyntaxException {
        LivingEntity subject = resolveSubject(ctx, explicitTarget);
        if (subject == null) {
            ctx.getSource().sendError(feedback("No player to inspect - run as a player or supply a target."));
            return 0;
        }
        CombatController controller = CombatControllerManager.getIfPresent(subject);
        if (controller == null) {
            ctx.getSource().sendFeedback(() -> feedback(subject.getName().getString() + " has no active CombatController."), false);
            return 0;
        }

        ctx.getSource().sendFeedback(() -> feedback(subject.getName().getString()
                + " | state=" + controller.getCombatState().name()
                + " | attackDirection=" + controller.getAttackDirection().name()
                + " | guardDirection=" + controller.getGuardDirection().name()), false);
        return 1;
    }

    /**
     * Resolves the subject a {@code combat}/{@code stamina}/{@code
     * direction} invocation should report on: the explicit target
     * argument if one was supplied, otherwise the command source's own
     * player (so a player can quickly self-check without typing a
     * selector). Returns {@code null} (never throws) if neither is
     * available, e.g. run from the console with no target.
     */
    private static LivingEntity resolveSubject(CommandContext<ServerCommandSource> ctx, Entity explicitTarget) {
        if (explicitTarget instanceof LivingEntity livingTarget) {
            return livingTarget;
        }
        ServerPlayerEntity self = ctx.getSource().getPlayer();
        return self;
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

    /**
     * {@code /bcombat ai enable <targets> [difficulty] [role] [squad]} —
     * a {@code null} {@code squadId} (the two/three-argument command
     * forms) enables plain solo AI combat via {@link
     * AICombatManager#enable(MobEntity, AIDifficultyPreset)}, exactly as
     * before. Supplying a squad name additionally opts every targeted
     * mob into that squad at the given (or default {@link
     * CombatRole#AGGRESSOR}) role via {@link
     * AICombatManager#enable(MobEntity, AIDifficultyPreset, CombatRole, String)}.
     */
    private static int enable(CommandContext<ServerCommandSource> ctx, AIDifficultyPreset difficulty,
                              CombatRole role, String squadId) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgumentType.getEntities(ctx, "targets");
        boolean wantsSquad = squadId != null && !squadId.isBlank();
        int enabled = 0;
        for (Entity target : targets) {
            if (target instanceof MobEntity mob) {
                if (wantsSquad) {
                    AICombatManager.enable(mob, difficulty, role, squadId);
                } else {
                    AICombatManager.enable(mob, difficulty);
                }
                enabled++;
            }
        }

        int skipped = targets.size() - enabled;
        int finalEnabled = enabled;
        String roleSuffix = wantsSquad
                ? " | role=" + (role != null ? role.name().toLowerCase(Locale.ROOT) : CombatRole.AGGRESSOR.name().toLowerCase(Locale.ROOT))
                + " | squad=" + squadId
                : "";
        ctx.getSource().sendFeedback(
                () -> feedback("Enabled AI combat (" + difficulty.name().toLowerCase(Locale.ROOT) + ") on "
                        + finalEnabled + " entit" + (finalEnabled == 1 ? "y" : "ies") + roleSuffix
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
            String squadSuffix = ai.getSquadId() != null
                    ? " | role=" + ai.getRole().name().toLowerCase(Locale.ROOT) + " | squad=" + ai.getSquadId()
                    : "";
            ctx.getSource().sendFeedback(() -> feedback(
                    " - " + mob.getType().getTranslationKey() + " @ " + formatPos(mob)
                            + " | difficulty=" + ai.getDifficulty().name().toLowerCase(Locale.ROOT)
                            + " | state=" + state
                            + " | stamina=" + stamina
                            + " | intent=" + ai.getTacticalIntent().name().toLowerCase(Locale.ROOT)
                            + squadSuffix
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

    private static CombatRole parseRole(CommandContext<ServerCommandSource> ctx) {
        String raw = StringArgumentType.getString(ctx, "role");
        try {
            return CombatRole.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendError(feedback("Unknown role '" + raw + "', defaulting to aggressor. Valid values: "
                    + Arrays.toString(CombatRole.values())));
            return CombatRole.AGGRESSOR;
        }
    }

    private static String formatPos(Entity entity) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", entity.getX(), entity.getY(), entity.getZ());
    }

    private static Text feedback(String message) {
        return Text.literal("[bcombat] " + message);
    }
}