package com.bcombat.debug;

import com.bcombat.BannerlordCombat;
import com.bcombat.combat.collision.HitResult;
import com.bcombat.combat.damage.DamageResult;
import com.bcombat.combat.events.CombatEvents;
import net.minecraft.entity.LivingEntity;

import java.util.Locale;

/**
 * Central, toggleable debug logger for the combat framework. Subscribes
 * once to every event {@link CombatEvents} declares that's useful for
 * diagnosing combat feel while tuning — state transitions, attack
 * outcomes, defensive mechanics, stamina/exhaustion, and weapon/damage
 * events — and prints a single-line, human-readable summary of each to
 * the mod's logger, gated behind a runtime on/off flag.
 * <p>
 * Registration ({@link #register()}) and enablement ({@link
 * #setEnabled}) are deliberately separate: listeners are attached
 * exactly once at startup regardless of whether debug logging starts
 * enabled, so toggling it on/off at runtime via {@code /bcombat debug}
 * never needs to re-register anything — it just flips the gate every
 * already-attached listener checks before printing.
 * <p>
 * This class contains no combat logic of its own — it is a pure
 * observer, exactly like {@code DamageService}'s relationship to {@code
 * AttackHitEvent} — and never mutates any combat state.
 */
public final class CombatDebugLogger {

    private static volatile boolean enabled = false;
    private static boolean registered = false;

    private CombatDebugLogger() {
        // Static utility, no instances.
    }

    /**
     * Attaches every debug listener to {@link CombatEvents}. Safe to
     * call more than once — only the first call actually registers
     * anything. Must be called once from {@code
     * BannerlordCombat#onInitialize()}, independent of whether debug
     * logging starts enabled.
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CombatEvents.COMBAT_ENTER.register(e ->
                log("%s entered Combat Mode", name(e.player())));
        CombatEvents.COMBAT_EXIT.register(e ->
                log("%s exited Combat Mode", name(e.player())));

        CombatEvents.COMBAT_STATE_CHANGED.register(e ->
                log("%s state: %s -> %s", name(e.player()), e.previousState(), e.currentState()));

        CombatEvents.MOUNTED_STATE_CHANGED.register(e ->
                log("%s mounted: %s%s", name(e.combatant()), e.mounted(),
                        e.mount() != null ? " (" + e.mount().getType().toString() + ")" : ""));

        CombatEvents.ATTACK_DIRECTION_CHANGED.register(e ->
                log("%s attack direction: %s -> %s", name(e.player()), e.previousDirection(), e.currentDirection()));
        CombatEvents.ATTACK_RELEASED.register(e ->
                log("%s released attack, direction=%s", name(e.player()), e.direction()));
        CombatEvents.ATTACK_RECOVERY_STARTED.register(e ->
                log("%s entered recovery", name(e.player())));

        CombatEvents.ATTACK_HIT.register(e -> logHitResult("HIT", e.result()));
        CombatEvents.ATTACK_MISS.register(e -> logHitResult("MISS", e.result()));
        CombatEvents.ATTACK_BLOCKED.register(e -> logHitResult("BLOCKED", e.result()));

        CombatEvents.GUARD_DIRECTION_CHANGED.register(e ->
                log("%s guard direction: %s -> %s", name(e.player()), e.previousDirection(), e.currentDirection()));
        CombatEvents.BLOCK_STARTED.register(e ->
                log("%s raised guard", name(e.player())));
        CombatEvents.BLOCK_ENDED.register(e ->
                log("%s lowered guard", name(e.player())));

        CombatEvents.PERFECT_BLOCK.register(e ->
                log("%s PERFECT BLOCK vs %s (guard=%s, incoming=%s)",
                        name(e.defender()), name(e.attacker()), e.guardDirection(), e.attackDirection()));
        CombatEvents.PARRY.register(e ->
                log("%s PARRY vs %s (guard=%s, incoming=%s)",
                        name(e.defender()), name(e.attacker()), e.guardDirection(), e.attackDirection()));
        CombatEvents.CHAMBER_STARTED.register(e ->
                log("%s chamber attempt vs %s, direction=%s", name(e.defender()), name(e.attacker()), e.direction()));
        CombatEvents.CHAMBER_SUCCEEDED.register(e ->
                log("%s CHAMBER SUCCESS vs %s, direction=%s", name(e.defender()), name(e.attacker()), e.direction()));

        CombatEvents.WEAPON_CHANGED.register(e ->
                log("%s weapon changed: %s -> %s",
                        name(e.player()),
                        e.previousItem() != null ? e.previousItem() : "empty",
                        e.newItem() != null ? e.newItem() : "empty"));

        CombatEvents.DAMAGE_APPLIED.register(e -> logDamageResult(e.result()));
        CombatEvents.CRITICAL_HIT.register(e ->
                log("CRITICAL HIT: %s -> %s, %.1f damage (x%.2f)",
                        name(e.result().attacker()), name(e.result().target()),
                        e.result().finalDamage(), e.result().criticalMultiplier()));
        CombatEvents.STAGGER_TRIGGERED.register(e ->
                log("%s staggered by %.1f damage from %s",
                        name(e.result().target()), e.result().finalDamage(), name(e.result().attacker())));

        CombatEvents.STAMINA_DEPLETED.register(e ->
                log("%s stamina depleted", name(e.player())));
        CombatEvents.STAMINA_REGENERATED.register(e ->
                log("%s stamina fully regenerated", name(e.player())));
        CombatEvents.EXHAUSTION_STARTED.register(e ->
                log("%s is now EXHAUSTED", name(e.player())));
        CombatEvents.EXHAUSTION_ENDED.register(e ->
                log("%s exhaustion ended", name(e.player())));

        BannerlordCombat.LOGGER.info("[bcombat] Debug logger listeners registered");
    }

    /** Enables or disables debug logging output at runtime. */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /** @return true if debug logging is currently enabled. */
    public static boolean isEnabled() {
        return enabled;
    }

    // ------------------------------------------------------------------
    // Formatting helpers
    // ------------------------------------------------------------------

    private static void logHitResult(String outcome, HitResult result) {
        if (result.hit() || result.blocked()) {
            log("%s: %s -> %s, weapon=%s, direction=%s, location=%s, defense=%s",
                    outcome,
                    name(result.attacker()),
                    result.target() != null ? name(result.target()) : "none",
                    itemName(result),
                    result.direction(),
                    result.hitLocation(),
                    result.defenseResult());
        } else {
            log("%s: %s swung and found no target, weapon=%s, direction=%s",
                    outcome, name(result.attacker()), itemName(result), result.direction());
        }
    }

    private static void logDamageResult(DamageResult result) {
        log("DAMAGE: %s -> %s, bodyPart=%s, pre-armor=%.1f, post-armor=%.1f, final=%.1f%s",
                name(result.attacker()), name(result.target()), result.bodyPart(),
                result.preArmorDamage(), result.postArmorDamage(), result.finalDamage(),
                result.critical() ? " (CRIT)" : "");
    }

    private static String itemName(HitResult result) {
        return result.weaponItem() != null ? result.weaponItem().toString() : "unarmed";
    }

    private static String name(LivingEntity entity) {
        if (entity == null) {
            return "none";
        }
        return entity.getName().getString() + "#" + entity.getId();
    }

    private static void log(String format, Object... args) {
        if (!enabled) {
            return;
        }
        BannerlordCombat.LOGGER.info("[bcombat-debug] " + String.format(Locale.ROOT, format, args));
    }
}