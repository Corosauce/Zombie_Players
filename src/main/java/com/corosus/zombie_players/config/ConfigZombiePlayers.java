package com.corosus.zombie_players.config;

import modconfig.ConfigComment;
import modconfig.IConfigCategory;

import java.io.File;

public class ConfigZombiePlayers implements IConfigCategory {

	public static boolean messUpChests = true;

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
