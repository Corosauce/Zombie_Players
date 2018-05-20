package com.corosus.zombie_players.config;

import modconfig.ConfigComment;
import modconfig.IConfigCategory;

import java.io.File;

public class ConfigZombiePlayers implements IConfigCategory {

	@ConfigComment("Only spawn a zombie player if the player died by a zombie directly")
	public static boolean requiresDeathByZombieToSpawnZombiePlayer = false;
	@ConfigComment("Finds visible chests near and randomly moves contents around")
	public static boolean messUpChests = true;
	public static boolean opensDoors = true;
	@ConfigComment("To help prevent endless multiplication of zombies if you die near your own spawn point")
	public static int distanceFromPlayerSpawnPointToPreventZombieSpawn = 16;

	@Override
	public String getName() {
		return "Zombie_Players";
	}

	@Override
	public String getRegistryName() {
		return "zombie_players";
	}

	@Override
	public String getConfigFileName() {
		return getName();
	}

	@Override
	public String getCategory() {
		return "zombie_players";
	}

	@Override
	public void hookUpdatedValues() {
		
	}

}
