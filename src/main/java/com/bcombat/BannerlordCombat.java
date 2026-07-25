package com.bcombat;

import com.bcombat.combat.damage.DamageService;
import com.bcombat.combat.damage.DefaultArmorRegistrations;
import com.bcombat.combat.weapon.DefaultWeaponRegistrations;

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

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}