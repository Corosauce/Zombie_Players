package com.corosus.zombie_players.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum EnumBlockBreakBehaviorType {

		BREAK_NORMAL, HARVEST, BREAK_ALL_BUT_BOTTOM, BREAK_VEINMINE_TREE;

		private static final Map<Integer, EnumBlockBreakBehaviorType> lookup = new HashMap<Integer, EnumBlockBreakBehaviorType>();
		static { for(EnumBlockBreakBehaviorType e : EnumSet.allOf(EnumBlockBreakBehaviorType.class)) { lookup.put(e.ordinal(), e); } }
		public static EnumBlockBreakBehaviorType get(int intValue) { return lookup.get(intValue); }
	}