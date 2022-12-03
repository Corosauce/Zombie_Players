package com.corosus.zombie_players.config;

import com.corosus.coroutil.util.CULog;
import com.corosus.modconfig.ConfigComment;
import com.corosus.modconfig.ConfigMod;
import com.corosus.modconfig.IConfigCategory;
import com.corosus.zombie_players.Zombie_Players;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class ConfigZombiePlayers implements IConfigCategory {

	@ConfigComment("Only spawn a zombie player if the player died by a zombie directly")
	public static boolean requiresDeathByZombieToSpawnZombiePlayer = false;
	@ConfigComment("Finds visible chests near and randomly moves contents around, only zombie players spawned from actual players use this")
	public static boolean messUpChests = true;
	public static boolean opensDoors = true;
	//public static boolean pickupLoot = true;

	public static boolean pickupLootWhenHostile = false;
	public static boolean pickupLootWhenCalm = true;

	@ConfigComment("To help prevent endless multiplication of zombies if you die near your own spawn point")
	public static int distanceFromPlayerSpawnPointToPreventZombieSpawn = 16;

	@ConfigComment("Spawn zombie players naturally in the world, will spawn in every biome zombies do")
	public static boolean Spawning_spawnZombiePlayersNaturally = true;

	@ConfigComment("Only used it Spawning_spawnZombiePlayersNaturally is true. Weight of zombie players, higher = more likely to spawn, vanilla sets zombie as 100")
	public static int Spawning_weight = 40;

	@ConfigComment("Only used it Spawning_spawnZombiePlayersNaturally is true. Minecraft profile names to use when naturally spawning in zombie players")
	public static String Spawning_playerNamesToUse = "PhoenixfireLune, Corosus, Cojomax99, Mr_okushama, tterrag, medsouz, NotActuallyTerry, MrRube";

	@ConfigComment("Any player that joins your dedicated server will have their name added to the Spawning_playerNamesToUse list")
	public static boolean automaticallyAddJoinedPlayersToNamesList = false;

	@ConfigComment("Use at own risk, will not support")
	public static boolean enableAdvancedDeveloperConfigFiles = false;

	@ConfigComment("items that will calm the zombie and let you train them, #food is a special match that will allow anything that the player can eat since a generic food data tag doesnt seem to exist yet")
	public static String calmingItems = "#food, minecraft:porkchop, minecraft:mutton, minecraft:tropical_fish, minecraft:beef, minecraft:chicken, minecraft:rabbit, minecraft:bread";

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

		if (ConfigZombiePlayers.enableAdvancedDeveloperConfigFiles && !ConfigMod.instance.lookupRegistryNameToConfig.containsKey(Zombie_Players.configDev.getRegistryName())) {
			ConfigMod.addConfigFile(null, Zombie_Players.configDev);
		}

		Zombie_Players.listCalmingItems.clear();
		Zombie_Players.matchAnyFood = false;

		try {
			CULog.dbg("Processing calming items list for Zombie Players");
			String[] names2 = calmingItems.split(",");
			for (int i = 0; i < names2.length; i++) {
				names2[i] = names2[i].trim();

				{
					if (names2[i].equals("#food")) {
						CULog.dbg("Matching any food item for zombie players");
						Zombie_Players.matchAnyFood = true;
					} else {
						Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(names2[i]));
						if (item != null && item != Items.AIR) {
							CULog.dbg("adding: " + item.getRegistryName());
							Zombie_Players.listCalmingItems.add(item);
						}
					}
				}
			}
		} catch (Exception ex) {
			CULog.err("CRITICAL ERROR PARSING calmingItems CONFIG STRING FOR ZOMBIE PLAYERS");
			ex.printStackTrace();
		}
	}

}
