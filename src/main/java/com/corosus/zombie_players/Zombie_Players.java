package com.corosus.zombie_players;

import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.entity.EntityZombiePlayer;
import modconfig.ConfigMod;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.List;

@Mod(modid = Zombie_Players.modID, name="Zombie Players", version=Zombie_Players.version, acceptableRemoteVersions="*", dependencies="required-after:coroutil@[1.12.1-1.2.11,)")
public class Zombie_Players {
	
	@Mod.Instance( value = Zombie_Players.modID )
	public static Zombie_Players instance;
	public static final String modID = "zombie_players";
    public static final String version = "${version}";

    @SidedProxy(clientSide = "com.corosus.zombie_players.ClientProxy", serverSide = "com.corosus.zombie_players.CommonProxy")
    public static CommonProxy proxy;

    public static String[] zombiePlayerNames = new String[] { "" };
    
	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        ConfigMod.addConfigFile(event, new ConfigZombiePlayers());
    }
    
	@Mod.EventHandler
    public void load(FMLInitializationEvent event)
    {
		MinecraftForge.EVENT_BUS.register(new EventHandlerForge());

        proxy.init(this);

        if (ConfigZombiePlayers.Spawning_spawnZombiePlayersNaturally) {

            int weight = Math.max(1, ConfigZombiePlayers.Spawning_weight);
            int groupSize = 1;

            //add zombie players to every biome that spawns something that extends zombie
            for (Biome biome : Biome.REGISTRY) {
                List<Biome.SpawnListEntry> list = biome.getSpawnableList(EnumCreatureType.MONSTER);
                for (Biome.SpawnListEntry entry : list) {
                    if (EntityZombie.class.isAssignableFrom(entry.entityClass)) {
                        list.add(new Biome.SpawnListEntry(EntityZombiePlayer.class, weight, groupSize, groupSize));
                        break;
                    }
                }
            }
        }
    }

}
