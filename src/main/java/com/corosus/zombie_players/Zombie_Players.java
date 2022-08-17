package com.corosus.zombie_players;

import com.corosus.modconfig.ConfigMod;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;

@Mod(Zombie_Players.MODID)
public class Zombie_Players {

	public static final String MODID = "zombie_players";
    public static final String version = "${version}";

    public static String[] zombiePlayerNames = new String[] { "" };

    public static List<Item> listCalmingItems = new ArrayList<>();

    public static ConfigZombiePlayersAdvanced configDev = new ConfigZombiePlayersAdvanced();

    public Zombie_Players() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.register(new EventHandlerForge());
        //ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MovePlusCfgForge.CLIENT_CONFIG);

        ConfigMod.addConfigFile(MODID, new ConfigZombiePlayers());
        ConfigMod.addConfigFile(MODID, configDev);

        //ensure the calmingItems list is populated
        if (!ConfigZombiePlayers.enableAdvancedDeveloperConfigFiles) {
            configDev.hookUpdatedValues();
        }
    }

    private void setup(final FMLCommonSetupEvent event)
    {

    }

    private void doClientStuff(final FMLClientSetupEvent event) {

    }

}
