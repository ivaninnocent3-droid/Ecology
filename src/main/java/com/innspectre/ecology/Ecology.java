package com.innspectre.ecology;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.innspectre.ecology.block.ModBlocks;
import com.innspectre.ecology.command.*;
import com.innspectre.ecology.need.*;
import com.innspectre.ecology.herd.*;

import com.innspectre.ecology.temperament.IndividualThreatTicker;

public class Ecology implements ModInitializer {
	public static final String MOD_ID = "ecology";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		NeedManager.initialize();
		NeedEvents.register();
		SurvivalEvents.register();
		NeedCommand.register();
		RecognitionCommand.register();
		HerdCommand.register();
		HerdDebugCommand.register();
		HerdEvents.register();
		HerdTicker.register();
		HerdAlarmTicker.register();
		IndividualThreatTicker.register();
		ModBlocks.initialize();
		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
