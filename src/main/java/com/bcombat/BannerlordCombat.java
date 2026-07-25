package com.bcombat;

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

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}