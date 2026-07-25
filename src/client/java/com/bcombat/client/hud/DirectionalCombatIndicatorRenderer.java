package com.bcombat.client.hud;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.util.CombatConstants;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Renders the framework's lightweight, Bannerlord-inspired directional
 * combat indicator: four small chevron "blade" marks arranged around the
 * crosshair (up / left / right / down), each corresponding to one of the
 * four {@link AttackDirection}/{@link GuardDirection} values a player can
 * commit to. Purely a HUD overlay - it reads state, it never writes any.
 * <p>
 * Registered against Fabric API's {@code HudRenderCallback} (see {@code
 * BannerlordCombatClient#onInitializeClient}), which supplies a {@link
 * DrawContext} already operating in GUI-scaled coordinate space - using
 * {@link MinecraftClient#getWindow()}'s scaled width/height for the
 * crosshair center is therefore all that's needed for this indicator to
 * scale correctly with the player's GUI Scale option, with no separate
 * scaling math of its own.
 * <p>
 * Every piece of state this renders - {@link CombatController#getCombatState()},
 * {@link CombatController#getAttackDirection()}, {@link
 * CombatController#getGuardDirection()} - already exists and is already
 * kept current by the existing input/networking pipeline; this class adds
 * no new combat state of its own; it only visualizes what {@link
 * CombatController} already tracks for the local player.
 * <p>
 * Visibility: by default the indicator only shows while the local
 * player's {@code CombatState} {@linkplain CombatState#isCombatActive()
 * is combat-active}, fading in on entry and back out on exit over {@link
 * #FADE_STEP_PER_FRAME}-sized steps each rendered frame. Setting {@link
 * CombatConstants#DIRECTIONAL_INDICATOR_ALWAYS_VISIBLE} (via {@code
 * /bcombat config}) keeps it visible at all times instead.
 */
public final class DirectionalCombatIndicatorRenderer {

    /** Per-frame fade step; ~12 rendered frames for a full fade in/out. */
    private static final float FADE_STEP_PER_FRAME = 1f / 12f;

    /** Distance, in scaled GUI pixels, from the crosshair to each chevron's near edge. */
    private static final int RADIUS = 14;

    /** Idle (uncommitted, unheld) chevron color - dim, desaturated steel. */
    private static final int COLOR_IDLE = 0x80707070;

    /** Guard direction currently held (BLOCK_IDLE/ENTER_BLOCK) - cool steel blue. */
    private static final int COLOR_GUARD = 0xFF5B8FC7;

    /** Attack direction currently being wound up (PREPARING_ATTACK) - warm amber. */
    private static final int COLOR_ATTACK_PREPARING = 0xFFD98A3D;

    /** Attack direction that has committed to the strike (ATTACKING) - bright, urgent red-gold. */
    private static final int COLOR_ATTACK_COMMITTED = 0xFFE8432B;

    /** Current fade-in/out progress, 0 (hidden) to 1 (fully shown). Persists across frames. */
    private static float visibility = 0f;

    private DirectionalCombatIndicatorRenderer() {
        // Static render entry point, no instances.
    }

    /**
     * {@code HudRenderCallback} entry point. Registered once from {@code
     * BannerlordCombatClient#onInitializeClient()}.
     */
    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            visibility = 0f;
            return;
        }
        // Never draw over other screens (inventory, chat, pause menu, etc.)
        // - HudRenderCallback already only fires while no Screen is open,
        // but guard explicitly since debug overlays / F3 share this pass.
        if (client.currentScreen != null) {
            return;
        }

        CombatController controller = CombatControllerManager.getIfPresent(client.player);
        boolean combatActive = controller != null && controller.getCombatState().isCombatActive();
        boolean shouldShow = combatActive || CombatConstants.DIRECTIONAL_INDICATOR_ALWAYS_VISIBLE;

        visibility = shouldShow
                ? Math.min(1f, visibility + FADE_STEP_PER_FRAME)
                : Math.max(0f, visibility - FADE_STEP_PER_FRAME);

        if (visibility <= 0f || controller == null) {
            return;
        }

        int centerX = client.getWindow().getScaledWidth() / 2;
        int centerY = client.getWindow().getScaledHeight() / 2;

        CombatState state = controller.getCombatState();
        AttackDirection attackDirection = controller.getAttackDirection();
        GuardDirection guardDirection = controller.getGuardDirection();
        boolean committed = state == CombatState.ATTACKING;

        drawChevron(context, centerX, centerY, Direction.UP,
                colorFor(Direction.UP, attackDirection, guardDirection, committed));
        drawChevron(context, centerX, centerY, Direction.LEFT,
                colorFor(Direction.LEFT, attackDirection, guardDirection, committed));
        drawChevron(context, centerX, centerY, Direction.RIGHT,
                colorFor(Direction.RIGHT, attackDirection, guardDirection, committed));
        drawChevron(context, centerX, centerY, Direction.DOWN,
                colorFor(Direction.DOWN, attackDirection, guardDirection, committed));
    }

    /**
     * Resolves the display color for one of the four screen directions,
     * mapping {@code UP -> OVERHEAD/UP_GUARD}, {@code LEFT -> LEFT_SLASH/
     * LEFT_GUARD}, {@code RIGHT -> RIGHT_SLASH/RIGHT_GUARD}, {@code DOWN ->
     * THRUST/THRUST_GUARD} - the same directional layout Bannerlord's own
     * indicator uses. A committed attack (mid-{@code ATTACKING}) always
     * wins over a merely-prepared one, which in turn wins over a held
     * guard, since only one of the three can ever be true for a given
     * direction at once anyway (attack and block are mutually exclusive
     * states).
     */
    private static int colorFor(Direction direction, AttackDirection attackDirection,
                                 GuardDirection guardDirection, boolean committed) {
        if (matchesAttack(direction, attackDirection)) {
            return committed ? COLOR_ATTACK_COMMITTED : COLOR_ATTACK_PREPARING;
        }
        if (matchesGuard(direction, guardDirection)) {
            return COLOR_GUARD;
        }
        return COLOR_IDLE;
    }

    private static boolean matchesAttack(Direction direction, AttackDirection attackDirection) {
        return switch (direction) {
            case UP -> attackDirection == AttackDirection.OVERHEAD;
            case LEFT -> attackDirection == AttackDirection.LEFT_SLASH;
            case RIGHT -> attackDirection == AttackDirection.RIGHT_SLASH;
            case DOWN -> attackDirection == AttackDirection.THRUST;
        };
    }

    private static boolean matchesGuard(Direction direction, GuardDirection guardDirection) {
        return switch (direction) {
            case UP -> guardDirection == GuardDirection.UP_GUARD;
            case LEFT -> guardDirection == GuardDirection.LEFT_GUARD;
            case RIGHT -> guardDirection == GuardDirection.RIGHT_GUARD;
            case DOWN -> guardDirection == GuardDirection.THRUST_GUARD;
        };
    }

    /**
     * Draws one direction's chevron ("blade mark") as three stacked bars
     * of decreasing size pointing outward from the crosshair - an
     * arrowhead silhouette requiring only axis-aligned {@link
     * DrawContext#fill} calls, no rotated geometry, so it renders
     * identically regardless of GUI Scale without any extra matrix work.
     */
    private static void drawChevron(DrawContext context, int centerX, int centerY, Direction direction, int color) {
        int alpha = (int) (((color >>> 24) & 0xFF) * visibility);
        if (alpha <= 0) {
            return;
        }
        int rgb = color & 0x00FFFFFF;
        int shadedColor = (alpha << 24) | rgb;

        switch (direction) {
            case UP -> {
                for (int i = 0; i < 3; i++) {
                    int halfWidth = 6 - i * 2;
                    int y = centerY - RADIUS - i * 3;
                    context.fill(centerX - halfWidth, y, centerX + halfWidth, y + 2, shadedColor);
                }
            }
            case DOWN -> {
                for (int i = 0; i < 3; i++) {
                    int halfWidth = 6 - i * 2;
                    int y = centerY + RADIUS + i * 3;
                    context.fill(centerX - halfWidth, y, centerX + halfWidth, y + 2, shadedColor);
                }
            }
            case LEFT -> {
                for (int i = 0; i < 3; i++) {
                    int halfHeight = 6 - i * 2;
                    int x = centerX - RADIUS - i * 3;
                    context.fill(x, centerY - halfHeight, x + 2, centerY + halfHeight, shadedColor);
                }
            }
            case RIGHT -> {
                for (int i = 0; i < 3; i++) {
                    int halfHeight = 6 - i * 2;
                    int x = centerX + RADIUS + i * 3;
                    context.fill(x, centerY - halfHeight, x + 2, centerY + halfHeight, shadedColor);
                }
            }
        }
    }

    /** The four screen-space directions the indicator draws in, centered on the crosshair. */
    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
}
