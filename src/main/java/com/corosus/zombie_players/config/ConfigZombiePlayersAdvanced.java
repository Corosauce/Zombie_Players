package com.corosus.zombie_players.config;

import CoroUtil.forge.CULog;
import com.corosus.zombie_players.Zombie_Players;
import modconfig.ConfigComment;
import modconfig.IConfigCategory;

public class ConfigZombiePlayersAdvanced implements IConfigCategory {

	public static int calmTimePerUse = 20*60*20;

	public static double healPerHit = 3;
	public static double healPerKill = 10;

	public static float range1 = 16;
	public static float range2 = 32;
	public static float range3 = 64;

	@Override
	public String getName() {
		return "Zombie_Players_Advanced";
	}

	@Override
	public String getRegistryName() {
		return "zombie_players_adv";
	}

	@Override
	public String getConfigFileName() {
		return getName();
	}

	@Override
	public String getCategory() {
		return "zombie_players_adv";
	}

	@Override
	public void hookUpdatedValues() {

	}

}
