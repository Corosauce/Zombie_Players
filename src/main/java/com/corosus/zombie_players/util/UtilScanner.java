package com.corosus.zombie_players.util;

import net.minecraft.block.BlockChest;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.function.BiPredicate;

public class UtilScanner {

    public static BlockPos findBlock(EntityLiving entity, int scanRange, int yVariance, int tries, BiPredicate<World, BlockPos> predicate) {

        int scanSize = scanRange;
        //int scanSizeY = scanRange / 2;
        int adjustRangeY = 10;

        int tryX;
        int tryY = MathHelper.floor(entity.posY);
        int tryZ;

        for (int ii = 0; ii <= tries; ii++) {
            //try close to entity first few times
            if (ii <= 3) {
                scanSize = 20;
                //scanSizeY = 10 / 2;
            } else {
                scanSize = scanRange;
                //scanSizeY = scanRange / 2;
            }
            tryX = MathHelper.floor(entity.posX) + (entity.world.rand.nextInt(scanSize)-scanSize/2);
            int i = tryY + (yVariance > 0 ? (entity.world.rand.nextInt(yVariance)-entity.world.rand.nextInt(yVariance)) : 0);
            tryZ = MathHelper.floor(entity.posZ) + (entity.world.rand.nextInt(scanSize)-scanSize/2);
            BlockPos posTry = new BlockPos(tryX, tryY, tryZ);

            boolean foundBlock = false;
            int newY = i;

            if (!entity.world.isAirBlock(posTry)) {
                //scan up
                int tryMax = adjustRangeY;
                while (!entity.world.isAirBlock(posTry) && tryMax-- > 0) {
                    newY++;
                    posTry = new BlockPos(tryX, newY, tryZ);
                }

                //if found air and water below it
                /*if (entity.world.isAirBlock(posTry) && entity.world.getBlockState(posTry.add(0, -1, 0)).getMaterial().isLiquid()) {
                    foundWater = true;
                }*/

                if (entity.world.isAirBlock(posTry) && predicate.test(entity.world, posTry.add(0, -1, 0))) {
                    foundBlock = true;
                    posTry = posTry.add(0, -1, 0);
                }
            } else {
                //scan down
                int tryMax = adjustRangeY;
                while (entity.world.isAirBlock(posTry) && tryMax-- > 0) {
                    newY--;
                    posTry = new BlockPos(tryX, newY, tryZ);
                }
                /*if (!entity.world.isAirBlock(posTry) && entity.world.getBlockState(posTry.add(0, 1, 0)).getMaterial().isLiquid()) {
                    foundWater = true;
                }*/
                if (entity.world.isAirBlock(posTry.add(0, 1, 0)) && predicate.test(entity.world, posTry)) {
                    foundBlock = true;
                }
            }

            if (foundBlock) {
                return posTry;
            }
        }

        return null;
    }

    public static boolean isWater(World world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial().isLiquid();
    }

    public static boolean isDeepWater(World world, BlockPos pos) {
        boolean clearAbove = world.isAirBlock(pos.up(1)) && world.isAirBlock(pos.up(2)) && world.isAirBlock(pos.up(3));
        boolean deep = world.getBlockState(pos).getMaterial().isLiquid() && world.getBlockState(pos.down()).getMaterial().isLiquid();
        boolean notUnderground = false;
        if (deep) {
            int height = world.getPrecipitationHeight(pos).getY() - 1;
            notUnderground = height == pos.getY();
        }

        return deep && notUnderground && clearAbove;
    }

    public static boolean isLand(World world, BlockPos pos) {
        return world.getBlockState(pos).isSideSolid(world, pos, EnumFacing.UP);
    }

    public static boolean isFire(World world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() == Material.FIRE;
    }

    public static boolean isChest(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof BlockChest;
    }
}
