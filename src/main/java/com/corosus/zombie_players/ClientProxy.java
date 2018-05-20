package com.corosus.zombie_players;

import com.corosus.zombie_players.client.entity.RenderZombiePlayer;
import com.corosus.zombie_players.entity.EntityZombiePlayer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    
    public ClientProxy()
    {
        
    }

    @Override
    public void init(Zombie_Players pMod)
    {
        super.init(pMod);

        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        addMapping(EntityZombiePlayer.class, new RenderZombiePlayer(rm));

    }

    @Override
    public void addItem(RegistryEvent.Register<Item> event, Item item, String name) {
        super.addItem(event, item, name);

        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Zombie_Players.modID + ":" + name, "inventory"));
    }

    @Override
    public void addBlock(RegistryEvent.Register<Block> event, Block parBlock, String unlocalizedName, boolean creativeTab) {
        super.addBlock(event, parBlock, unlocalizedName, creativeTab);

        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(parBlock), 0, new ModelResourceLocation(Zombie_Players.modID + ":" + unlocalizedName, "inventory"));
    }

    @Override
    public void addItemBlock(RegistryEvent.Register<Item> event, Item item) {
        super.addItemBlock(event, item);

        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }

    private static void addMapping(Class<? extends Entity> entityClass, Render render) {
        RenderingRegistry.registerEntityRenderingHandler(entityClass, render);
    }
}
