package com.corosus.zombie_players.config;

import CoroUtil.forge.CULog;
import com.corosus.zombie_players.Zombie_Players;
import modconfig.ConfigComment;
import modconfig.ConfigMod;
import modconfig.IConfigCategory;

import java.io.File;

public class ConfigZombiePlayers implements IConfigCategory {

	@ConfigComment("Only spawn a zombie player if the player died by a zombie directly")
	public static boolean requiresDeathByZombieToSpawnZombiePlayer = false;
	@ConfigComment("Finds visible chests near and randomly moves contents around, only zombie players spawned from actual players use this")
	public static boolean messUpChests = true;
	public static boolean opensDoors = true;
	public static boolean pickupLoot = true;
	@ConfigComment("To help prevent endless multiplication of zombies if you die near your own spawn point")
	public static int distanceFromPlayerSpawnPointToPreventZombieSpawn = 16;

	@ConfigComment("Spawn zombie players naturally in the world, will spawn in every biome zombies do")
	public static boolean Spawning_spawnZombiePlayersNaturally = false;

	@ConfigComment("Only used it Spawning_spawnZombiePlayersNaturally is true. Weight of zombie players, higher = more likely to spawn, vanilla sets zombie as 100")
	public static int Spawning_weight = 20;

	@ConfigComment("Only used it Spawning_spawnZombiePlayersNaturally is true. Minecraft profile names to use when naturally spawning in zombie players")
	public static String Spawning_playerNamesToUse = "PhoenixfireLune, Corosus, Cojomax99, Mr_okushama, tterrag, medsouz, SirTerryWrist, MrRube";

	@ConfigComment("Use at own risk, will not support")
	public static boolean enableAdvancedDeveloperConfigFiles = false;

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
		CULog.dbg("Updating Zombie Player names to use");
		String[] names = Spawning_playerNamesToUse.split(",");
		for (int i = 0; i < names.length; i++) {
			//remove spaces
			names[i] = names[i].trim();
		}
		Zombie_Players.zombiePlayerNames = names;

		if (ConfigZombiePlayers.enableAdvancedDeveloperConfigFiles && !ConfigMod.instance.configLookup.containsKey(Zombie_Players.configDev.getRegistryName())) {
			ConfigMod.addConfigFile(null, Zombie_Players.configDev);
		}
	}

}
