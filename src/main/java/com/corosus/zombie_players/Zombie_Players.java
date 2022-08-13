package com.corosus.zombie_players;

import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import com.corosus.zombie_players.entity.EntityZombiePlayer;
import modconfig.ConfigMod;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Biomes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

@Mod(Zombie_Players.modID)
public class Zombie_Players {

	public static final String MODID = "zombie_players";
    public static final String version = "${version}";

    @SidedProxy(clientSide = "com.corosus.zombie_players.ClientProxy", serverSide = "com.corosus.zombie_players.CommonProxy")
    public static CommonProxy proxy;

    public static String[] zombiePlayerNames = new String[] { "" };

    public static List<Item> listCalmingItems = new ArrayList<>();

    public static ConfigZombiePlayersAdvanced configDev = new ConfigZombiePlayersAdvanced();

    public Zombie_Players() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.register(new EventHandlerForge());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MovePlusCfgForge.CLIENT_CONFIG);
    }

    private void setup(final FMLCommonSetupEvent event)
    {

    }

    private void doClientStuff(final FMLClientSetupEvent event) {

    }
    
	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        ConfigMod.addConfigFile(event, new ConfigZombiePlayers());
        //ensure the calmingItems list is populated
        if (!ConfigZombiePlayers.enableAdvancedDeveloperConfigFiles) {
            configDev.hookUpdatedValues();
        }

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
