package com.bcombat;

import com.bcombat.combat.damage.DamageService;
import com.bcombat.combat.damage.DefaultArmorRegistrations;
import com.bcombat.combat.weapon.DefaultWeaponRegistrations;
import com.bcombat.command.BCombatCommand;
import com.bcombat.config.BCombatConfig;
import com.bcombat.debug.CombatDebugLogger;
import com.bcombat.server.ServerCombatLifecycleHandler;
import com.bcombat.server.ServerCombatTickHandler;

import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BannerlordCombat implements ModInitializer {
	public static final String MOD_ID = "bcombat";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Loads config/bcombat.json into CombatConstants (creating the
		// file with current defaults if it doesn't exist yet). Must run
		// FIRST, before any system below reads a CombatConstants value
		// during its own setup - see BCombatConfig's class docs.
		BCombatConfig.load();

		// Registers the example vanilla-item weapon mappings so the weapon
		// framework (WeaponRegistry/WeaponController/CombatController) is
		// exercisable in-game. Common-side (not client-only) since weapon
		// resolution in CombatController is written to run server-side too
		// once networking exists. See DefaultWeaponRegistrations' javadoc
		// for why these are placeholder/example stats, not balancing.
		DefaultWeaponRegistrations.register();

		// Registers the example vanilla-item armor mappings so the armor
		// framework (ArmorRegistry/ArmorResolver) is exercisable in-game,
		// the same way DefaultWeaponRegistrations does for weapons. See
		// its javadoc for why these are example stats, not balancing.
		DefaultArmorRegistrations.register();

		// Wires the damage & armor framework into combat by subscribing
		// to the existing AttackHitEvent - see DamageService's class
		// docs for why this is the entire integration point and why it
		// keeps damage calculation fully decoupled from collision
		// detection.
		DamageService.register();

		// Drives every tracked server-side CombatController (players and
		// AI-enabled mobs alike) once per server tick, after first
		// letting AICombatManager make this tick's AI decisions - see
		// ServerCombatTickHandler's class docs for the full ordering
		// rationale. This is the server-side analogue of the client's
		// CombatInputHandler#onClientTick.
		ServerCombatTickHandler.register();

		// Creates and destroys CombatController/AICombatController
		// instances at every point in their lifecycle - player join/
		// disconnect/respawn, AI-mob death/unload, and full registry
		// clears on server shutdown - so the tick loop above never ticks
		// a stale or missing controller. See its class docs for exactly
		// which event owns which slice of that lifecycle.
		ServerCombatLifecycleHandler.register();

		// Attaches every debug-log listener to CombatEvents (inert until
		// enabled via /bcombat debug true). See CombatDebugLogger's
		// class docs for why registration and enablement are separate.
		CombatDebugLogger.register();

		// Registers the /bcombat command tree - ai enable/disable/
		// difficulty/list, debug on/off, and config reload/save - the
		// operator-facing control surface for AICombatManager,
		// CombatDebugLogger, and BCombatConfig. See BCombatCommand's
		// class docs.
		BCombatCommand.register();

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}