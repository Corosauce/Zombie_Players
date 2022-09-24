package com.corosus.zombie_players.util;

import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.Map;

public class UtilCrops {

    public static final Map<BlockState, BlockState> crops = Maps.newHashMap();

    public static void initDataIfNeeded() {
        if (crops.size() == 0) {
            ForgeRegistries.BLOCKS.getValues().stream()
                    .filter(b -> b instanceof CropBlock)
                    .map(b -> (CropBlock) b)
                    .forEach(b -> crops.put(b.defaultBlockState().setValue(b.getAgeProperty(), last(b.getAgeProperty().getPossibleValues())), b.defaultBlockState()));
        }
    }

    private static int last(Collection<Integer> vals) {
        return vals.stream().max(Integer::compare).orElse(0);
    }

    public static boolean harvestAndReplant(Level world, BlockPos pos, BlockState inWorld, Player player, LivingEntity entityWithInventory) {
        initDataIfNeeded();
        if (!(world instanceof ServerLevel))
            return false;

        ItemStack mainHand = entityWithInventory.getMainHandItem();

        int fortune = 0;

        ItemStack copy = mainHand.copy();
        if (copy.isEmpty())
            copy = new ItemStack(Items.STICK);

        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(copy);
        enchMap.put(Enchantments.BLOCK_FORTUNE, fortune);
        EnchantmentHelper.setEnchantments(enchMap, copy);

        Item blockItem = inWorld.getBlock().asItem();
        Block.getDrops(inWorld, (ServerLevel) world, pos, world.getBlockEntity(pos), player, copy)
                .forEach((stack) -> {
                    if(stack.getItem() == blockItem)
                        stack.shrink(1);

                    if(!stack.isEmpty())
                        Block.popResource(world, pos, stack);
                });
        inWorld.spawnAfterBreak((ServerLevel) world, pos, copy);

        BlockState newBlock = crops.get(inWorld);
        if (newBlock != null) {
            world.levelEvent(2001, pos, Block.getId(newBlock));
            world.setBlockAndUpdate(pos, newBlock);
            return true;
        } else {
            return false;
        }
    }

}
