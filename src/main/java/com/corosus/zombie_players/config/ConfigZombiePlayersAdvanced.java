package com.corosus.zombie_players.config;

import CoroUtil.forge.CULog;
import com.corosus.zombie_players.Zombie_Players;
import modconfig.ConfigComment;
import modconfig.IConfigCategory;
import net.minecraft.item.Item;

public class ConfigZombiePlayersAdvanced implements IConfigCategory {

	public static int calmTimePerUse = 20*60*60;

	public static double healPerHit = 3;
	public static double healPerKill = 10;

	public static float stayNearHome_range1 = 16;
	public static float stayNearHome_range2 = 32;
	public static float stayNearHome_range3 = 64;

	public static double calmItemSearchRange = 12;

	public static boolean onlySeekFoodIfNeeded = false;

	public static String calmingItems = "minecraft:porkchop, minecraft:mutton, minecraft:fish, minecraft:beef, minecraft:chicken, minecraft:rabbit";

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
		Zombie_Players.calmingItems.clear();

		String[] names = calmingItems.split(",");
		for (int i = 0; i < names.length; i++) {
			//remove spaces
			names[i] = names[i].trim();
			Item item = Item.getByNameOrId(names[i]);
			if (item != null) {
				CULog.dbg("adding " + item.getUnlocalizedName());
				Zombie_Players.calmingItems.add(item);
			}
		}
	}

}
