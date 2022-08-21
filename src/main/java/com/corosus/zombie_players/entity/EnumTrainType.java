package com.corosus.zombie_players.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum EnumTrainType {

		BLOCK_LEFT_CLICK, BLOCK_RIGHT_CLICK, ENTITY_LEFT_CLICK, ENTITY_RIGHT_CLICK;

		private static final Map<Integer, EnumTrainType> lookup = new HashMap<Integer, EnumTrainType>();
		static { for(EnumTrainType e : EnumSet.allOf(EnumTrainType.class)) { lookup.put(e.ordinal(), e); } }
		public static EnumTrainType get(int intValue) { return lookup.get(intValue); }
	}