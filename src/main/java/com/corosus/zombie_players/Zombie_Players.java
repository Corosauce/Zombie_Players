package com.corosus.zombie_players;

import com.corosus.modconfig.ConfigMod;
import com.corosus.zombie_players.config.ConfigZombiePlayers;
import com.corosus.zombie_players.config.ConfigZombiePlayersAdvanced;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

    public static boolean matchAnyFood = false;

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

    //0 = off, 1 = click first pos, 2 = click second pos
    public static String ZP_SET_WORK_AREA_STAGE = "zp_set_work_area_stage";

    public static int getWorkAreaStage(Player player) {
        CompoundTag compoundTag = player.getPersistentData();
        if (compoundTag.contains(ZP_SET_WORK_AREA_STAGE)) {
            return compoundTag.getInt(ZP_SET_WORK_AREA_STAGE);
        } else {
            return 0;
        }
    }

}
