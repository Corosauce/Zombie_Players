package com.corosus.zombie_players.config;

import CoroUtil.forge.CULog;
import com.corosus.zombie_players.Zombie_Players;
import modconfig.ConfigComment;
import modconfig.IConfigCategory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;

public class ConfigZombiePlayersAdvanced implements IConfigCategory {

	public static int calmTimePerUse = 20*60*60;
	public static double healPerUse = 7;

	public static double healPerHit = 3;
	public static double healPerKill = 10;

    public static double heal1HealthPerXTicks = 20*60*5;

	public static float stayNearHome_range1 = 16;
	public static float stayNearHome_range2 = 32;
	public static float stayNearHome_range3 = 64;

	public static double calmItemSearchRange = 12;

	public static int chestSearchRange = 20;

	@ConfigComment("They already search more relaxed like if they dont need food, but this will fully lock them out unless they need it")
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
		Zombie_Players.listCalmingItems.clear();

		try {
			CULog.dbg("Processing calming items list for Zombie Players");
			String[] names = calmingItems.split(",");
			for (int i = 0; i < names.length; i++) {
				//remove spaces
				names[i] = names[i].trim();

				if (names[i].contains("ore:")) {
					String oreDictName = names[i].split(":")[1];
					CULog.dbg("processing ore dictionary entry: " + oreDictName);
					NonNullList<ItemStack> stacks = OreDictionary.getOres(oreDictName);
					if (stacks.size() == 0) {
						CULog.dbg("none found for ore dictionary name: " + oreDictName);
					}
					for (ItemStack stack : stacks) {
						CULog.dbg("adding ore dict'd item: " + stack.getItem().getRegistryName());
						Zombie_Players.listCalmingItems.add(stack.getItem());
					}
				} else {
					Item item = Item.getByNameOrId(names[i]);
					if (item != null) {
						CULog.dbg("adding: " + item.getRegistryName());
						Zombie_Players.listCalmingItems.add(item);
					}
				}
			}
		} catch (Exception ex) {
			CULog.err("CRITICAL ERROR PARSING calmingItems CONFIG STRING FOR ZOMBIE PLAYERS");
			ex.printStackTrace();
		}
	}

}
