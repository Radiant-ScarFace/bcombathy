package com.bcombat.combat.mounted;

import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.events.MountedStateChangedEvent;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/**
 * The dedicated per-combatant controller for mount detection. Owned by
 * {@code CombatController} exactly the same way it owns {@link
 * com.bcombat.combat.weapon.WeaponController} and {@link
 * com.bcombat.combat.collision.CollisionController} — this class has no
 * knowledge of {@code CombatStateManager}, attack timing, or damage,
 * only of whether {@code player} is currently riding a recognized combat
 * mount and how fast that mount is moving, which keeps it trivially
 * testable. {@link MountedCombatModifiers} is the stateless companion
 * that turns what this class reports into the actual numeric bonuses;
 * this class only ever reports state, never computes gameplay-facing
 * modifiers itself.
 * <p>
 * Detection is polling-based (compared once per tick against the
 * previous vehicle) rather than event-driven, mirroring {@link
 * com.bcombat.combat.weapon.WeaponController}'s own reasoning: vanilla
 * has no "started/stopped riding" callback this framework can hook, so
 * {@link #tick()} simply re-reads {@code player.getVehicle()} every
 * tick and diffs it against what was seen last tick.
 * <p>
 * <b>Client/server correctness (see the class-level verification this
 * phase specifically calls for):</b> exactly one {@link
 * MountedCombatController} instance exists per {@code CombatController}
 * instance (constructed once in {@code CombatController}'s constructor,
 * never re-created), and {@code CombatController#tick()} calls {@link
 * #tick()} exactly once per invocation — there is no second call site
 * anywhere in the framework. Since a player can have up to two {@code
 * CombatController} instances in the same JVM (the server's
 * authoritative one and, on an integrated singleplayer server, the
 * client's own predictive one — see {@code CombatControllerManager}'s
 * class docs), each such instance owns its own independent {@link
 * MountedCombatController} with its own independent {@code mounted}/
 * {@code mount} fields, so a transition detected by one instance can
 * never double-count against the other's state. Within a single
 * instance, {@link #tick()} only ever fires {@link
 * CombatEvents#MOUNTED_STATE_CHANGED} when the freshly-read vehicle
 * state actually differs from what was already stored (see the
 * early-return in {@link #tick()}), so a transition is reported exactly
 * once regardless of how many ticks elapse before the next one — not
 * once per tick, and never twice for the same transition. This is
 * deliberately unguarded by {@code authoritative} (unlike, say, {@link
 * com.bcombat.combat.movement.MovementModifierManager}'s attribute
 * mutations): mount state is derived purely from {@code
 * player.getVehicle()} and the vehicle's own velocity, both of which
 * vanilla already keeps in sync across every client and the server, so
 * every {@code CombatController} instance — authoritative or not —
 * computes the same result independently and safely, exactly like
 * {@code WeaponController}'s equipped-item detection already does.
 */
public final class MountedCombatController {

    private final LivingEntity player;

    private boolean mounted;
    private Entity mount;

    public MountedCombatController(LivingEntity player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    /**
     * Re-reads {@code player.getVehicle()} and, if the resulting mounted
     * state (or the specific vehicle ridden) differs from what was
     * recorded last tick, updates state and fires exactly one {@link
     * CombatEvents#MOUNTED_STATE_CHANGED}. A no-op — including no event
     * — on every tick nothing changed, which is what guarantees a given
     * transition is only ever reported once. Must be called once per
     * tick; see this class's docs for why calling it more than once per
     * tick is never necessary or done anywhere in this framework.
     */
    public void tick() {
        Entity vehicle = player.getVehicle();
        boolean recognized = vehicle != null && isRecognizedMount(vehicle);
        boolean nowMounted = CombatConstants.MOUNTED_COMBAT_ENABLED
                && vehicle != null
                && (!CombatConstants.MOUNTED_REQUIRE_RECOGNIZED_MOUNT || recognized);
        Entity nowMount = nowMounted ? vehicle : null;

        if (nowMounted == mounted && nowMount == mount) {
            // No actual transition - the single guard that keeps a
            // transition from ever being reported more than once, and
            // keeps every ordinary tick where nothing changed a no-op.
            return;
        }

        mounted = nowMounted;
        mount = nowMount;

        CombatEvents.MOUNTED_STATE_CHANGED.invoker()
                .onMountedStateChanged(new MountedStateChangedEvent(player, mounted, mount));
    }

    /** @return true if this combatant is currently mounted on a recognized combat mount. */
    public boolean isMounted() {
        return mounted;
    }

    /** @return the entity currently ridden, or {@code null} if not mounted. */
    public Entity getMount() {
        return mount;
    }

    /**
     * @return the mount's current horizontal speed, in blocks/tick, or
     * {@code 0.0} if not mounted. Horizontal-only (ignores vertical
     * velocity) since {@link CombatConstants#MOUNTED_CHARGE_SPEED_THRESHOLD}
     * and every couch-lance speed gate are about ground charge speed, not
     * a mount jumping or falling.
     */
    public double getMountSpeed() {
        if (!mounted || mount == null) {
            return 0.0;
        }
        Vec3d velocity = mount.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    /**
     * @return true if {@code vehicle} is one of the animal types this
     * framework recognizes as a legitimate combat mount. Deliberately a
     * small, explicit allow-list (horses/donkeys/mules and their
     * undead variants via {@link AbstractHorseEntity}, plus camels,
     * saddled pigs, and striders) rather than "any entity with a
     * passenger seat", so sitting in a boat or minecart never
     * accidentally grants mounted combat bonuses.
     */
    private static boolean isRecognizedMount(Entity vehicle) {
        if (vehicle instanceof AbstractHorseEntity) {
            return true;
        }
        if (vehicle instanceof CamelEntity) {
            return true;
        }
        if (vehicle instanceof StriderEntity) {
            return true;
        }
        if (vehicle instanceof PigEntity pig) {
            return pig.isSaddled();
        }
        // Any other MobEntity a future content pack wants recognized as
        // a mount can be added above without touching call sites - this
        // method is the single place that decision is made.
        return vehicle instanceof MobEntity && false;
    }
}