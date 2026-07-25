package com.bcombat.client.feedback.sound;

import com.bcombat.client.feedback.config.FeedbackConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Central, configurable sound-cue dispatcher for every combat event this
 * framework reacts to. Every event this class plays a sound for is keyed
 * through {@link CombatSoundKey} and resolved through {@link
 * #SOUND_TABLE}, a single swappable map from key to {@code SoundEvent} +
 * base volume/pitch - so re-skinning the entire mod's combat audio (or
 * disabling a specific cue) never requires touching a call site, only
 * this table.
 * <p>
 * All playback is client-side only (via {@code World#playSound} with a
 * {@code null} source player, i.e. the local, non-attenuated overload
 * used for player-facing UI/feedback sounds), exactly like every other
 * subsystem in this framework - no packet is sent, no server state is
 * touched.
 */
public final class CombatSoundPlayer {

    private static final Random RANDOM = new Random();

    /** Swappable sound table: key -> (event, base volume, base pitch). Public for external re-skinning. */
    public static final Map<CombatSoundKey, SoundSpec> SOUND_TABLE = new EnumMap<>(CombatSoundKey.class);

    static {
        SOUND_TABLE.put(CombatSoundKey.HIT_LIGHT, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.9f, 1.1f));
        SOUND_TABLE.put(CombatSoundKey.HIT_HEAVY, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.85f));
        SOUND_TABLE.put(CombatSoundKey.HIT_FLESH, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_HURT, 0.8f, 1.0f));
        SOUND_TABLE.put(CombatSoundKey.HIT_ARMOR, new SoundSpec(net.minecraft.sound.SoundEvents.ITEM_ARMOR_EQUIP_IRON, 0.9f, 1.2f));
        SOUND_TABLE.put(CombatSoundKey.CRITICAL_HIT, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.1f, 1.0f));
        SOUND_TABLE.put(CombatSoundKey.ATTACK_MISS, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, 0.6f, 1.0f));
        SOUND_TABLE.put(CombatSoundKey.BLOCK, new SoundSpec(net.minecraft.sound.SoundEvents.ITEM_SHIELD_BLOCK, 0.9f, 1.0f));
        SOUND_TABLE.put(CombatSoundKey.PERFECT_BLOCK, new SoundSpec(net.minecraft.sound.SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 1.3f));
        SOUND_TABLE.put(CombatSoundKey.PARRY, new SoundSpec(net.minecraft.sound.SoundEvents.ITEM_TRIDENT_HIT_GROUND, 1.0f, 1.5f));
        SOUND_TABLE.put(CombatSoundKey.CHAMBER_START, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.4f));
        SOUND_TABLE.put(CombatSoundKey.CHAMBER_SUCCESS, new SoundSpec(net.minecraft.sound.SoundEvents.ITEM_TRIDENT_RETURN, 1.0f, 1.2f));
        SOUND_TABLE.put(CombatSoundKey.STAMINA_DEPLETED, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_BREATH, 0.7f, 0.7f));
        SOUND_TABLE.put(CombatSoundKey.EXHAUSTION_STARTED, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_BIG_FALL, 0.5f, 0.6f));
        SOUND_TABLE.put(CombatSoundKey.LANDING_IMPACT, new SoundSpec(net.minecraft.sound.SoundEvents.ENTITY_GENERIC_BIG_FALL, 1.0f, 0.9f));
    }

    private CombatSoundPlayer() {
        // Static utility, no instances.
    }

    /** Plays {@code key}'s configured sound centered at {@code entity}'s current position. */
    public static void play(CombatSoundKey key, LivingEntity entity) {
        if (entity == null) {
            return;
        }
        play(key, entity.getPos());
    }

    /** Plays {@code key}'s configured sound at an explicit world position. */
    public static void play(CombatSoundKey key, Vec3d pos) {
        if (!FeedbackConstants.FEEDBACK_ENABLED || !FeedbackConstants.COMBAT_SOUNDS_ENABLED || pos == null) {
            return;
        }
        SoundSpec spec = SOUND_TABLE.get(key);
        if (spec == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        float pitchJitter = 1.0f + (RANDOM.nextFloat() * 2.0f - 1.0f) * FeedbackConstants.SOUND_PITCH_VARIANCE;
        client.world.playSound(client.player, pos.x, pos.y, pos.z, spec.event(), SoundCategory.PLAYERS,
                spec.baseVolume() * FeedbackConstants.SOUND_VOLUME_MASTER, spec.basePitch() * pitchJitter);
    }

    /** Every combat feedback moment this framework can play a configurable sound for. */
    public enum CombatSoundKey {
        HIT_LIGHT,
        HIT_HEAVY,
        HIT_FLESH,
        HIT_ARMOR,
        CRITICAL_HIT,
        ATTACK_MISS,
        BLOCK,
        PERFECT_BLOCK,
        PARRY,
        CHAMBER_START,
        CHAMBER_SUCCESS,
        STAMINA_DEPLETED,
        EXHAUSTION_STARTED,
        LANDING_IMPACT
    }

    /** One entry in {@link #SOUND_TABLE}: the sound to play plus its base volume/pitch before jitter. */
    public record SoundSpec(SoundEvent event, float baseVolume, float basePitch) {
    }
}