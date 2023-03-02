package com.corosus.zombie_players;

import com.corosus.coroutil.util.CULog;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.ZombiePlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Zombie_Players.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldRegistry {

    @SubscribeEvent
    public static void onBiomesLoad(BiomeLoadingEvent event) {
        if (event.getSpawns().getSpawner(MobCategory.MONSTER).stream().anyMatch((entry) -> entry.type == EntityType.ZOMBIE)) {
            CULog.dbg("Adding zombie player spawning to biome category: " + event.getName());
            event.getSpawns().getSpawner(MobCategory.MONSTER).add(new MobSpawnSettings.SpawnerData(EntityRegistry.zombie_player, Math.max(1, ConfigZombiePlayers.Spawning_weight), 1, 1));
        }

        if (SpawnPlacements.getPlacementType(EntityRegistry.zombie_player) == SpawnPlacements.Type.NO_RESTRICTIONS) {
            SpawnPlacements.register(EntityRegistry.zombie_player, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        }
    }

}
