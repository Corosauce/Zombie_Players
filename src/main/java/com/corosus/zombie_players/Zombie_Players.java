package com.corosus.zombie_players;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Zombie_Players.modID, name="Zombie Players", version=Zombie_Players.version, acceptableRemoteVersions="*")
public class Zombie_Players {
	
	@Mod.Instance( value = Zombie_Players.modID )
	public static Zombie_Players instance;
	public static final String modID = "zombie_players";
    public static final String version = "${version}";

    @SidedProxy(clientSide = "com.corosus.zombie_players.ClientProxy", serverSide = "com.corosus.zombie_players.CommonProxy")
    public static CommonProxy proxy;
    
	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {

    }
    
	@Mod.EventHandler
    public void load(FMLInitializationEvent event)
    {
		MinecraftForge.EVENT_BUS.register(new EventHandlerForge());

        proxy.init(this);
    }

}
