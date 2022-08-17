package com.corosus.zombie_players;

import com.corosus.zombie_players.config.ConfigZombiePlayers;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zombie_Players.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WorldRegistry {

    public static void onBiomesLoad(BiomeLoadingEvent event) {
            event.getSpawns().getSpawner(MobCategory.CREATURE).add(new MobSpawnSettings.SpawnerData(EntityRegistry.zombie_player, Math.max(1, ConfigZombiePlayers.Spawning_weight), 1, 1));
    }

}
