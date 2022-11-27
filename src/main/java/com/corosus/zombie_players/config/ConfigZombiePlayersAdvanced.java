package com.corosus.zombie_players.config;

import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.IConfigCategory;

public class ConfigZombiePlayersAdvanced implements IConfigCategory {

	public static int calmTimePerUse = 20*60*60;
	public static double healPerUse = 7;

	public static double healPerHit = 3;
	public static double healPerKill = 10;

    public static double heal1HealthPerXTicks = 20*60*5;

	public static double stayNearHome_range0 = 8;
	public static double stayNearHome_range1 = 16;
	public static double stayNearHome_range2 = 32;
	public static double stayNearHome_range3 = 64;

	public static double calmItemSearchRange = 12;

	public static int chestSearchRange = 20;

	@ConfigComment("They already search more relaxed like if they dont need food, but this will fully lock them out unless they need it")
	public static boolean onlySeekFoodIfNeeded = false;

	public static int tickDelayBetweenPlaying = 20*60*60;

	public static boolean canPickupItemsWithMobGriefingOff = true;

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
